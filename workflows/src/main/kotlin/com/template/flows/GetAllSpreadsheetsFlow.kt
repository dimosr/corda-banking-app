package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.SpreadsheetState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker

/**
 * Returns the linear IDs of all the spreadsheets.
 */
@InitiatingFlow
@StartableByRPC
class GetAllSpreadsheetsFlow : FlowLogic<List<String>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<String> {
        val spreadsheets = serviceHub.vaultService.queryBy<SpreadsheetState>()

        return spreadsheets.states.map { spreadsheet -> spreadsheet.state.data.linearId.toString() }
    }
}
