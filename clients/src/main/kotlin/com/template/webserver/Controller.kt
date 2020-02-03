package com.template.webserver

import com.template.flows.CreateSpreadsheetFlow
import com.template.flows.GetSpreadsheetFlow
import com.template.flows.UpdateFormulaStateFlow
import com.template.flows.UpdateValueStateFlow
import com.template.states.FormulaState
import com.template.states.ValueState
import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startTrackedFlow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Exception
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy
    private val objectMapper = JacksonSupport.createNonRpcMapper()

    private var valueCellTagToRef = mutableMapOf<Pair<Int, Int>, StateAndRef<ValueState>>()
    private var formulaCellTagToRef = mutableMapOf<Pair<Int, Int>, StateAndRef<FormulaState>>()

    @GetMapping(value = "get-spreadsheet", produces = ["application/json"])
    private fun getSpreadsheet(
            @QueryParam(value = "id") id: Int
    ) = try {
        val spreadsheet = proxy.startTrackedFlow(::GetSpreadsheetFlow).returnValue.get()
                ?: proxy.startTrackedFlow(::CreateSpreadsheetFlow).returnValue.get().apply {
                    valueStates.forEachIndexed { index, stateAndRef ->
                        valueCellTagToRef[index to 1] = stateAndRef
                    }
                    listOf(formulaState).forEachIndexed { index, stateAndRef ->
                        formulaCellTagToRef[index to 1] = stateAndRef
                    }
                }

        objectMapper.writeValueAsString(spreadsheet)
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }

    @GetMapping(value = "set-data", produces = ["application/json"])
    private fun setData(
            @QueryParam(value = "d") d: Int?,
            @QueryParam(value = "f") f: String?,
            @QueryParam(value = "row") row: Int,
            @QueryParam(value = "col") col: Int
    ) = try {
        if (f == null)
            proxy.startTrackedFlow(
                    ::UpdateValueStateFlow,
                    valueCellTagToRef[row to col]!!.state.data.linearId.toString(),
                    d.toString()
            )
        else
            proxy.startTrackedFlow(
                    ::UpdateFormulaStateFlow,
                    formulaCellTagToRef[row to col]!!.state.data.linearId.toString(),
                    f
            )

        "Successful cell update."
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }
}