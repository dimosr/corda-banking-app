package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.FormulaCalculator
import com.template.contracts.FormulaContract
import com.template.contracts.SpreadsheetContract
import com.template.contracts.ValueContract
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
class CreateSpreadsheetFlow : FlowLogic<SpreadsheetDTO>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SpreadsheetDTO {
        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()

        val otherParticipants = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - notaries - ourIdentity
        val allParticipants = otherParticipants + ourIdentity

        val txBuilder = TransactionBuilder(notaryToUse)
        allParticipants.sortedBy { it.name.toString() }

        val valueStates = allParticipants.sortedBy { it.name.toString() }
                .mapIndexed { index, participant -> ValueState("", participant, allParticipants - participant, index, 0, 0, UniqueIdentifier.fromString(UUID.randomUUID().toString())) }
        val formulaStates = listOf(0, 1, 2).map { index ->
            FormulaState("", allParticipants, allParticipants.size, index, 0, UniqueIdentifier.fromString(UUID.randomUUID().toString()))
        }
        val spreadsheetId = UniqueIdentifier.fromString(UUID.randomUUID().toString())
        val spreadsheetState = SpreadsheetState(valueStates.map { it.linearId }, formulaStates.map { it.linearId }, otherParticipants + ourIdentity, spreadsheetId)

        valueStates.forEach { txBuilder.addOutputState(it) }
        formulaStates.forEach { txBuilder.addOutputState(it) }
        txBuilder.addOutputState(spreadsheetState)
        val signers = otherParticipants.map { it.owningKey } + ourIdentity.owningKey
        txBuilder.addCommand(SpreadsheetContract.Commands.CreateSpreadsheet(), signers)
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = otherParticipants.map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))

        subFlow(FinalityFlow(fullySignedTx, sessions))

        val valueStatesWithRefs = valueStates.map { retrieveValueState(it.linearId, serviceHub.vaultService) }
        val formulaStatesWithRef = formulaStates.map { retrieveFormulaState(it.linearId, serviceHub.vaultService) }
        val formulaStatesWithCalculatedValues = formulaStatesWithRef.map {
            it to "0"
        }

        return SpreadsheetDTO(valueStatesWithRefs, formulaStatesWithCalculatedValues, spreadsheetState.editors, spreadsheetId.toString())
    }
}

@InitiatedBy(CreateSpreadsheetFlow::class)
class CreateSpreadsheetFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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
