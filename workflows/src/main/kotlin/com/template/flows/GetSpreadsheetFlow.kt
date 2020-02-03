package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.persistence.FormulaStateSchemaV1
import com.template.persistence.SpreadsheetStateSchemaV1
import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class GetSpreadsheetFlow(private val spreadsheetId: String) : FlowLogic<SpreadsheetDTO?>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SpreadsheetDTO? {
        val spreadsheet = retrieveSpreadsheetState(UniqueIdentifier.fromString(spreadsheetId), serviceHub.vaultService)?.state?.data ?: return null

        val valueStates = spreadsheet.valueStates.map { linearId ->
            retrieveValueState(linearId, serviceHub.vaultService)
        }
        val formulaState = retrieveFormulaState(spreadsheet.formulaState, serviceHub.vaultService)
        return SpreadsheetDTO(valueStates, formulaState, spreadsheet.editors, spreadsheet.linearId.toString())
    }
}

fun retrieveSpreadsheetState(spreadsheetStateId: UniqueIdentifier, vaultService: VaultService): StateAndRef<SpreadsheetState>? {
    val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
    val results = builder {
        val spreadsheetIdCheck = SpreadsheetStateSchemaV1.PersistentSpreadsheetState::linearId.equal(spreadsheetStateId.toString())
        val criteria = generalCriteria.and(QueryCriteria.VaultCustomQueryCriteria(spreadsheetIdCheck))
        vaultService.queryBy(SpreadsheetState::class.java, criteria)
    }

    return results.states.singleOrNull()
}