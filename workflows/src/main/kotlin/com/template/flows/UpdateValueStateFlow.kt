package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SpreadsheetContract
import com.template.contracts.ValueContract
import com.template.persistence.FormulaStateSchemaV1
import com.template.persistence.ValueStateSchemaV1
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import com.template.states.ValueState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.util.*

@InitiatingFlow
@StartableByRPC
class UpdateValueStateFlow(
        private val spreadsheetStateId: String,
        private val rowId: Int,
        private val columnId: Int,
        private val newValue: String,
        private val version: Int
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
        if (valueStates.find { it.state.data.rowId == rowId && it.state.data.columnId == columnId } != null) {
            updateValue(valueStates, notaryToUse)
        } else {
            createValue(spreadsheet, notaryToUse)
        }
    }

    @Suspendable
    private fun updateValue(valueStates: List<StateAndRef<ValueState>>, notaryToUse: Party) {
        val currentState = valueStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }

        if (currentState.state.data.owner != ourIdentity)
            throw InvalidOwnerException(currentState.state.data.owner)

        if (version != currentState.state.data.version)
            throw InvalidVersionException(currentState.state.data.version, version)
        val newState = ValueState(newValue, currentState.state.data.owner, currentState.state.data.watchers, currentState.state.data.rowId, currentState.state.data.columnId, version + 1, currentState.state.data.linearId)
        val txBuilder = TransactionBuilder(notaryToUse)
        txBuilder.addInputState(currentState)
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(ValueContract.Commands.Update(), ourIdentity.owningKey)
        val fullySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = newState.watchers.map { initiateFlow(it) }
        subFlow(FinalityFlow(fullySignedTx, sessions))
    }

    @Suspendable
    private fun createValue(spreadsheet: StateAndRef<SpreadsheetState>, notaryToUse: Party) {
        val txBuilder = TransactionBuilder(notaryToUse)

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val otherParticipants = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - notaries - ourIdentity
        val newState = ValueState(newValue, ourIdentity, otherParticipants, rowId, columnId, 0, UniqueIdentifier.fromString(UUID.randomUUID().toString()))
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(ValueContract.Commands.Issue(), ourIdentity.owningKey)

        txBuilder.addInputState(spreadsheet)
        txBuilder.addOutputState(spreadsheet.state.data.addValueState(newState.linearId))
        txBuilder.addCommand(SpreadsheetContract.Commands.UpdateSpreadsheet(), otherParticipants.map { it.owningKey } + ourIdentity.owningKey)

        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = newState.watchers.map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))
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
        val signFlow = object : SignTransactionFlow(counterpartySession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }
        val signedTx = subFlow(signFlow)

        subFlow(ReceiveFinalityFlow(counterpartySession, signedTx.id))
    }
}

class InvalidOwnerException(owner: Party) : IllegalArgumentException()