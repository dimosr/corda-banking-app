package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ValueContract
import com.template.persistence.FormulaStateSchemaV1
import com.template.persistence.ValueStateSchemaV1
import com.template.states.FormulaState
import com.template.states.ValueState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class UpdateValueStateFlow(private val stateIdentifier: String, private val newValue: String) : FlowLogic<Unit>() {

    @Suspendable
    override fun call(): Unit {
        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()
        val currentState = retrieveValueState(UniqueIdentifier.fromString(stateIdentifier), serviceHub.vaultService)

        val newState = ValueState(newValue, currentState.state.data.owner, currentState.state.data.watchers, currentState.state.data.rowId, currentState.state.data.columnId,currentState.state.data.linearId)
        val txBuilder = TransactionBuilder(notaryToUse)
        txBuilder.addInputState(currentState)
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(ValueContract.Update(), ourIdentity.owningKey)
        val fullySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = newState.watchers.map { initiateFlow(it) }
        subFlow(FinalityFlow(fullySignedTx, sessions))
    }
}

fun retrieveValueState(valueStateId: UniqueIdentifier, vaultService: VaultService): StateAndRef<ValueState> {
    val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
    val results = builder {
        val cargoIdCheck = ValueStateSchemaV1.PersistentValueState::linearId.equal(valueStateId.toString())
        val criteria = generalCriteria.and(QueryCriteria.VaultCustomQueryCriteria(cargoIdCheck))
        vaultService.queryBy(ValueState::class.java, criteria)
    }

    return results.states.single()
}

fun retrieveFormulaState(valueStateId: UniqueIdentifier, vaultService: VaultService): StateAndRef<FormulaState> {
    val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
    val results = builder {
        val cargoIdCheck = FormulaStateSchemaV1.PersistentFormulaState::linearId.equal(valueStateId.toString())
        val criteria = generalCriteria.and(QueryCriteria.VaultCustomQueryCriteria(cargoIdCheck))
        vaultService.queryBy(FormulaState::class.java, criteria)
    }

    return results.states.single()
}

@InitiatedBy(UpdateValueStateFlow::class)
class UpdateValueStateFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
