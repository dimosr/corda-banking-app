package com.template

import com.template.flows.CreateSpreadsheetFlow
import com.template.flows.GetAllSpreadsheetsFlow
import com.template.flows.GetSpreadsheetFlow
import com.template.flows.UpdateFormulaStateFlow
import com.template.flows.UpdateValueStateFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `create spreadsheet test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)
        val ourIdentity = partyAHandle.nodeInfo.legalIdentities.first()

        partyAHandle.rpc.startFlow(::CreateSpreadsheetFlow).returnValue.get()
        val spreadsheetIds = partyAHandle.rpc.startFlow(::GetAllSpreadsheetsFlow).returnValue.get()
        val spreadsheetId = spreadsheetIds.first()

        var spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertNotNull(spreadsheetDto)
        assertEquals(2, spreadsheetDto!!.valueStates.size)
        var ourState = spreadsheetDto.valueStates.filter { it.state.data.owner == ourIdentity }.single()
        assertEquals("", ourState.state.data.data)

        partyAHandle.rpc.startFlow(::UpdateValueStateFlow, spreadsheetDto.linearId, ourState.state.data.rowId, ourState.state.data.columnId, "12").returnValue.get()

        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertNotNull(spreadsheetDto)
        assertEquals(2, spreadsheetDto!!.valueStates.size)
        ourState = spreadsheetDto.valueStates.filter { it.state.data.owner == ourIdentity }.single()
        assertEquals("12", ourState.state.data.data)

        partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.formulaState.state.data.linearId.toString(), "a+b").returnValue.get()
        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertEquals("a+b", spreadsheetDto!!.formulaState.state.data.formula)
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}