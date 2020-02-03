package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.SpreadsheetState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class GetSpreadsheetFlow : FlowLogic<SpreadsheetDTO?>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SpreadsheetDTO? {
        val spreadsheets = serviceHub.vaultService.queryBy<SpreadsheetState>()

        val spreadsheet = spreadsheets.states.map { it.state.data }.firstOrNull() ?: return null

        val valueStates = spreadsheet.valueStates.map { linearId ->
            retrieveValueState(linearId, serviceHub.vaultService)
        }
        val formulaState = retrieveFormulaState(spreadsheet.formulaState, serviceHub.vaultService)
        return SpreadsheetDTO(valueStates, formulaState, spreadsheet.editors, spreadsheet.linearId.toString())
    }
}

