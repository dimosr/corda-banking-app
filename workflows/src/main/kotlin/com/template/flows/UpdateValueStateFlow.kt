package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ValueContract
import com.template.persistence.FormulaStateSchemaV1
import com.template.persistence.ValueStateSchemaV1
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import com.template.states.ValueState
import net.corda.core.contracts.StateAndRef
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
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateValueStateFlow(
        private val spreadsheetStateId: String,
        private val rowId: Int,
        private val columnId: Int,
        private val newValue: String
) : FlowLogic<Unit>() {

    @Suspendable
    override fun call(): Unit {
        // TODO: Can be done more efficiently, by embedding spreadsheet ID on states and querying by it.
        val spreadsheets = serviceHub.vaultService.queryBy<SpreadsheetState>()
        val spreadsheet = spreadsheets.states.filter { it.state.data.linearId.toString() == spreadsheetStateId }.single()
        val valueStates = spreadsheet.state.data.valueStates.map {
            retrieveValueState(it, serviceHub.vaultService)
        }

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()
        val currentState = valueStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }

        val newState = ValueState(newValue, currentState.state.data.owner, currentState.state.data.watchers, currentState.state.data.rowId, currentState.state.data.columnId, currentState.state.data.linearId)
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
        val valueIdCheck = ValueStateSchemaV1.PersistentValueState::linearId.equal(valueStateId.toString())
        val criteria = generalCriteria.and(QueryCriteria.VaultCustomQueryCriteria(valueIdCheck))
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
