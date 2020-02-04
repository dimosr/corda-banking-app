package com.template

import com.template.flows.ConcurrentModificationException
import com.template.flows.CreateSpreadsheetFlow
import com.template.flows.GetAllSpreadsheetsFlow
import com.template.flows.GetSpreadsheetFlow
import com.template.flows.InvalidVersionException
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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `test happy path`() = withDriver {
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
        assertEquals("", spreadsheetDto.formulaStates.first().second)

        partyAHandle.rpc.startFlow(::UpdateValueStateFlow, spreadsheetDto.linearId, ourState.state.data.rowId, ourState.state.data.columnId,  "12", ourState.state.data.version).returnValue.get()

        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertNotNull(spreadsheetDto)
        assertEquals(2, spreadsheetDto!!.valueStates.size)
        ourState = spreadsheetDto.valueStates.filter { it.state.data.owner == ourIdentity }.single()
        assertEquals("12", ourState.state.data.data)

        val formulaState = spreadsheetDto.formulaStates.first().first.state.data
        partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "A1+B1", formulaState.version).returnValue.get()
        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertEquals("A1+B1", spreadsheetDto!!.formulaStates.first().first.state.data.formula)
    }

    @Test
    fun `test invalid version is detected and rejected with exception`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)
        val ourIdentity = partyAHandle.nodeInfo.legalIdentities.first()

        partyAHandle.rpc.startFlow(::CreateSpreadsheetFlow).returnValue.get()
        val spreadsheetIds = partyAHandle.rpc.startFlow(::GetAllSpreadsheetsFlow).returnValue.get()
        val spreadsheetId = spreadsheetIds.first()

        var spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()

        val formulaState = spreadsheetDto!!.formulaStates.first().first.state.data
        assertThatThrownBy { partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "a+b", formulaState.version+1).returnValue.getOrThrow() }
                .isInstanceOf(InvalidVersionException::class.java)
    }

    @Test
    fun `test concurrent update of formula blocks one writer and allows the other one`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)
        val ourIdentity = partyAHandle.nodeInfo.legalIdentities.first()

        partyAHandle.rpc.startFlow(::CreateSpreadsheetFlow).returnValue.get()
        val spreadsheetIds = partyAHandle.rpc.startFlow(::GetAllSpreadsheetsFlow).returnValue.get()
        val spreadsheetId = spreadsheetIds.first()

        var spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()

        val formulaState = spreadsheetDto!!.formulaStates.first().first.state.data
        assertThatThrownBy {
            val flow1Handle = partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "a+b", formulaState.version)
            val flow2Handle = partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "a+b", formulaState.version)

            flow1Handle.returnValue.getOrThrow()
            flow2Handle.returnValue.getOrThrow()
        }
        .isInstanceOf(ConcurrentModificationException::class.java)
    }

    @Test
    fun `formula calculation works correctly`() = withDriver {
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
        val theirState = spreadsheetDto.valueStates.filter { it.state.data.owner == partyBHandle.nodeInfo.legalIdentities.first() }.single()

        partyAHandle.rpc.startFlow(::UpdateValueStateFlow, spreadsheetDto.linearId, ourState.state.data.rowId, ourState.state.data.columnId,  "12", ourState.state.data.version).returnValue.get()
        partyBHandle.rpc.startFlow(::UpdateValueStateFlow, spreadsheetDto.linearId, theirState.state.data.rowId, theirState.state.data.columnId,  "5", ourState.state.data.version).returnValue.get()

        var formulaState = spreadsheetDto.formulaStates.first().first.state.data
        partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "A1+B1", formulaState.version).returnValue.get()
        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertEquals("A1+B1", spreadsheetDto!!.formulaStates.first().first.state.data.formula)
        assertEquals(17, spreadsheetDto.formulaStates.first().second.toFloat().toInt())

        formulaState = spreadsheetDto.formulaStates.first().first.state.data
        partyAHandle.rpc.startFlow(::UpdateFormulaStateFlow, spreadsheetDto.linearId, formulaState.rowId, formulaState.columnId, "A1*B1", formulaState.version).returnValue.get()
        spreadsheetDto = partyAHandle.rpc.startFlow(::GetSpreadsheetFlow, spreadsheetId).returnValue.get()
        assertEquals("A1*B1", spreadsheetDto!!.formulaStates.first().first.state.data.formula)
        assertEquals(60, spreadsheetDto.formulaStates.first().second.toFloat().toInt())
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