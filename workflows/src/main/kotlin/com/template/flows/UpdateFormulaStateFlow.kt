package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.FormulaCalculator
import com.template.contracts.FormulaContract
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
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException

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
        val currentState = formulaStates.single { it.state.data.rowId == rowId && it.state.data.columnId == columnId }
        val newCanonicalValue = convertFormulaToUseStateRefs(newValue, valueStates)

        if (version != currentState.state.data.version)
            throw InvalidVersionException(currentState.state.data.version, version)
        val newState = FormulaState(newCanonicalValue, currentState.state.data.editors, currentState.state.data.rowId, currentState.state.data.columnId,version + 1, currentState.state.data.linearId)
        val txBuilder = TransactionBuilder(notaryToUse)
        txBuilder.addInputState(currentState)
        txBuilder.addOutputState(newState)
        txBuilder.addCommand(FormulaContract.Commands.Update(), currentState.state.data.editors.map { it.owningKey })
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherParticipantSessions = (currentState.state.data.editors - ourIdentity).map { initiateFlow(it) }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, otherParticipantSessions))

        try {
            subFlow(FinalityFlow(fullySignedTx, otherParticipantSessions))
        } catch(e: NotaryException) {
            if (e.error is NotaryError.Conflict) {
                throw ConcurrentModificationException()
            } else {
                throw e
            }
        }
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
        val stateRefIdentifier = "_${stateAndRef.ref.txhash}_${stateAndRef.ref.index}"
        val spreadsheetIdentifier = "${INDEX_TO_CHARACTERS_MAP[stateAndRef.state.data.rowId]}${stateAndRef.state.data.columnId + 1}"
        calculatedValue = calculatedValue.replace(spreadsheetIdentifier, stateRefIdentifier)
    }
    return calculatedValue
}

fun convertFormulaToUseSpreadsheetValues(originalValue: String, stateRefs: List<StateAndRef<ValueState>>): String {
    var calculatedValue = originalValue
    stateRefs.forEach { stateAndRef ->
        val stateRefIdentifier = "_${stateAndRef.ref.txhash}_${stateAndRef.ref.index}"
        val spreadsheetIdentifier = "${INDEX_TO_CHARACTERS_MAP[stateAndRef.state.data.rowId]}${stateAndRef.state.data.columnId + 1}"
        calculatedValue = calculatedValue.replace(stateRefIdentifier, spreadsheetIdentifier)
    }
    return calculatedValue
}

fun calculateFormulaValue(canonicalValue: String, stateRefs: List<StateAndRef<ValueState>>): String {
    val cellToValueMap = stateRefs.map { stateAndRef ->
        val value = if(stateAndRef.state.data.data == "") "0" else stateAndRef.state.data.data
        "_${stateAndRef.ref.txhash}_${stateAndRef.ref.index}" to value
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

class InvalidVersionException(val expectedVersion: Int, val providedVersion: Int): FlowException()
class ConcurrentModificationException(): FlowException()