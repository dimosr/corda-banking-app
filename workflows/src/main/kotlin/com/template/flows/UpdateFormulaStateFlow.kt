package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.FormulaCalculator
import com.template.contracts.FormulaContract
import com.template.contracts.SpreadsheetContract
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import com.template.states.ValueState
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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.HashMap

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
        val valueStates = spreadsheet.state.data.valueStates.map {
            retrieveValueState(it, serviceHub.vaultService)
        }

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val notaryToUse = notaries.first()
        if (formulaStates.find { it.state.data.rowId == rowId && it.state.data.columnId == columnId } != null) {
            updateFormula(formulaStates, valueStates, notaryToUse)
        } else {
            createFormula(spreadsheet, valueStates, notaryToUse)
        }
    }

    @Suspendable
    private fun updateFormula(formulaStates: List<StateAndRef<FormulaState>>, valueStates: List<StateAndRef<ValueState>>, notaryToUse: Party) {
        val currentState = formulaStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }
        val newCanonicalValue = convertFormulaToUseStateRefs(newValue, valueStates)

        if (version != currentState.state.data.version)
            throw InvalidVersionException(currentState.state.data.version, version)
        val newState = FormulaState(newCanonicalValue, currentState.state.data.editors, currentState.state.data.rowId, currentState.state.data.columnId, version + 1, currentState.state.data.linearId)
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

    @Suspendable
    private fun createFormula(spreadsheet: StateAndRef<SpreadsheetState>, valueStates: List<StateAndRef<ValueState>>, notaryToUse: Party) {
        val newCanonicalValue = convertFormulaToUseStateRefs(newValue, valueStates)

        val txBuilder = TransactionBuilder(notaryToUse)

        val notaries = serviceHub.networkMapCache.notaryIdentities
        val otherParticipants = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - notaries - ourIdentity
        val newState = FormulaState(newCanonicalValue, otherParticipants + ourIdentity, rowId, columnId, 0, UniqueIdentifier.fromString(UUID.randomUUID().toString()))
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(FormulaContract.Commands.Issue(), otherParticipants.map { it.owningKey } + ourIdentity.owningKey)

        txBuilder.addInputState(spreadsheet)
        txBuilder.addOutputState(spreadsheet.state.data.addFormulaState(newState.linearId))
        txBuilder.addCommand(SpreadsheetContract.Commands.UpdateSpreadsheet(), otherParticipants.map { it.owningKey } + ourIdentity.owningKey)

        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = otherParticipants.map { initiateFlow(it) }
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

fun convertFormulaToUseStateRefs(originalValue: String, stateRefs: List<StateAndRef<ValueState>>): String {
    var calculatedValue = originalValue
    stateRefs.forEach { stateAndRef ->
        val stateRefIdentifier = "_${stateAndRef.state.data.linearId.toString().replace("-", "_")}"
        val spreadsheetIdentifier = "${INDEX_TO_CHARACTERS_MAP[stateAndRef.state.data.columnId]}${stateAndRef.state.data.rowId + 1}"
        // using double replace to avoid identifying spreadsheet identifiers inside stateref hashes
        calculatedValue = calculatedValue.replace(spreadsheetIdentifier, "($spreadsheetIdentifier)")
    }
    stateRefs.forEach { stateAndRef ->
        val stateRefIdentifier = "_${stateAndRef.state.data.linearId.toString().replace("-", "_")}"
        val spreadsheetIdentifier = "${INDEX_TO_CHARACTERS_MAP[stateAndRef.state.data.columnId]}${stateAndRef.state.data.rowId + 1}"
        calculatedValue = calculatedValue.replace("($spreadsheetIdentifier)", stateRefIdentifier)
    }
    return calculatedValue
}

fun convertFormulaToUseSpreadsheetValues(originalValue: String, stateRefs: List<StateAndRef<ValueState>>): String {
    var calculatedValue = originalValue
    stateRefs.forEach { stateAndRef ->
        val stateRefIdentifier = "_${stateAndRef.state.data.linearId.toString().replace("-", "_")}"
        val spreadsheetIdentifier = "${INDEX_TO_CHARACTERS_MAP[stateAndRef.state.data.columnId]}${stateAndRef.state.data.rowId + 1}"
        calculatedValue = calculatedValue.replace(stateRefIdentifier, spreadsheetIdentifier)
    }
    return calculatedValue
}

fun calculateFormulaValue(canonicalValue: String, stateRefs: List<StateAndRef<ValueState>>): String {
    val cellToValueMap = stateRefs.map { stateAndRef ->
        val value = if (stateAndRef.state.data.data == "") "0" else stateAndRef.state.data.data
        "_${stateAndRef.state.data.linearId.toString().replace("-", "_")}" to value
    }.toMap() as HashMap<String, String>
    return FormulaCalculator.calculateFormula(canonicalValue, cellToValueMap)
}

val CHARACTERS_TO_INDEX_MAP = mapOf(
        "A" to 0,
        "B" to 1,
        "C" to 2,
        "D" to 3,
        "E" to 4,
        "F" to 5,
        "G" to 6,
        "H" to 7,
        "I" to 8
)
val INDEX_TO_CHARACTERS_MAP = CHARACTERS_TO_INDEX_MAP.entries.associateBy({ it.value }) { it.key }

class InvalidVersionException(val expectedVersion: Int, val providedVersion: Int) : FlowException()
class ConcurrentModificationException() : FlowException()
