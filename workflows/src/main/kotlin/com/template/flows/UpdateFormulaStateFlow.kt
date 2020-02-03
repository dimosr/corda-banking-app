package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.FormulaContract
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UpdateFormulaStateFlow(
        private val spreadsheetStateId: String,
        private val rowId: Int,
        private val columnId: Int,
        private val newValue: String
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
        val currentState = formulaStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }

        val newState = FormulaState(newValue, currentState.state.data.editors, currentState.state.data.rowId, currentState.state.data.columnId, currentState.state.data.linearId)
        val txBuilder = TransactionBuilder(notaryToUse)
        txBuilder.addInputState(currentState)
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(FormulaContract.Update(), currentState.state.data.editors.map { it.owningKey })
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherParticipantSessions = (currentState.state.data.editors - ourIdentity).map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, otherParticipantSessions))

        subFlow(FinalityFlow(fullySignedTx, otherParticipantSessions))
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
