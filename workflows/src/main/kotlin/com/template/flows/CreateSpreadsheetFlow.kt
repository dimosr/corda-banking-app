package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SpreadsheetContract
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import com.template.states.ValueState
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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateSpreadsheetFlow : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {
        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()

        val participants = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - notaries - ourIdentity

        val txBuilder = TransactionBuilder(notaryToUse)
        val valueStates = participants.map { ValueState("", ourIdentity, participants, UniqueIdentifier.fromString(UUID.randomUUID().toString())) }
        val formulaState = FormulaState("", participants + ourIdentity)
        val spreadsheetId = UniqueIdentifier.fromString(UUID.randomUUID().toString())
        val spreadsheetState = SpreadsheetState(participants + ourIdentity, spreadsheetId)

        valueStates.forEach { txBuilder.addOutputState(it) }
        txBuilder.addOutputState(spreadsheetState)
        txBuilder.addOutputState(formulaState)
        txBuilder.addCommand(SpreadsheetContract.CreateSpreadsheet(), participants.map { it.owningKey } + ourIdentity.owningKey)
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = participants.map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))

        subFlow(FinalityFlow(fullySignedTx, sessions))

        return spreadsheetId.toString()
    }
}

@InitiatedBy(CreateSpreadsheetFlow::class)
class CreateSpreadsheetFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signFlow = object : SignTransactionFlow(counterpartySession) {
            @Suspendable override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }
        val signedTx = subFlow(signFlow)

        subFlow(ReceiveFinalityFlow(counterpartySession, signedTx.id))
    }
}
