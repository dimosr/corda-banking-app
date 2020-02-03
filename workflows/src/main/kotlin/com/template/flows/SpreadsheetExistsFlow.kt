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
class SpreadsheetExistsFlow : FlowLogic<Boolean>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Boolean {
        val spreadsheets = serviceHub.vaultService.queryBy<SpreadsheetState>()

        return !spreadsheets.states.isEmpty()
    }
}