package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.FormulaContract
import com.template.contracts.SpreadsheetContract
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.internal._allEnabledSerializationEnvs
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.util.*

@InitiatingFlow
@StartableByRPC
class UpdateFormulaStateFlow(
        private val spreadsheetStateId: String,
        private val rowId: Int,
        private val columnId: Int,
        private val newValue: String,
        private val version: Int
) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val spreadsheets = serviceHub.vaultService.queryBy<SpreadsheetState>()
        val spreadsheet = spreadsheets.states.filter { it.state.data.linearId.toString() == spreadsheetStateId }.single()
        val formulaStates = spreadsheet.state.data.formulaStates.map {
            retrieveFormulaState(it, serviceHub.vaultService)
        }

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()
        if (formulaStates.find { it.state.data.rowId == rowId && it.state.data.columnId == columnId } != null) {
            updateFormula(formulaStates, notaryToUse)
        } else {
            createFormula(spreadsheet, notaryToUse)
        }
    }

    private fun updateFormula(formulaStates: List<StateAndRef<FormulaState>>, notaryToUse: Party) {
        val currentState = formulaStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }

        if (version != currentState.state.data.version) {
            throw InvalidVersionException(currentState.state.data.version, version)
        }

        val newState = FormulaState(newValue, currentState.state.data.editors, currentState.state.data.rowId, currentState.state.data.columnId, version + 1, currentState.state.data.linearId)
        val txBuilder = TransactionBuilder(notaryToUse)
        txBuilder.addInputState(currentState)
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(FormulaContract.Commands.Update(), currentState.state.data.editors.map { it.owningKey })
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherParticipantSessions = (currentState.state.data.editors - ourIdentity).map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, otherParticipantSessions))

        try {
            subFlow(FinalityFlow(fullySignedTx, otherParticipantSessions))
        } catch (e: NotaryException) {
            if (e.error is NotaryError.Conflict) {
                throw ConcurrentModificationException()
            } else {
                throw e
            }
        }
    }

    private fun createFormula(spreadsheet: StateAndRef<SpreadsheetState>, notaryToUse: Party) {
        val txBuilder = TransactionBuilder(notaryToUse)

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val otherParticipants = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - notaries - ourIdentity
        val newState = FormulaState(newValue, otherParticipants + ourIdentity, rowId, columnId, 0, UniqueIdentifier.fromString(UUID.randomUUID().toString()))
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(FormulaContract.Commands.Issue(), otherParticipants.map { it.owningKey } + ourIdentity.owningKey)

        txBuilder.addInputState(spreadsheet)
        txBuilder.addOutputState(spreadsheet.state.data.addFormulaState(newState.linearId))
        txBuilder.addCommand(SpreadsheetContract.Commands.UpdateSpreadsheet(), otherParticipants.map { it.owningKey } + ourIdentity.owningKey)

        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = newState.editors.map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))
        subFlow(FinalityFlow(fullySignedTx, sessions))
    }
}

@InitiatedBy(UpdateFormulaStateFlow::class)
class UpdateFormulaStateFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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

class InvalidVersionException(val expectedVersion: Int, val providedVersion: Int) : FlowException()
class ConcurrentModificationException() : FlowException()