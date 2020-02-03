package com.template.webserver

import com.template.flows.CreateSpreadsheetFlow
import com.template.flows.GetSpreadsheetFlow
import com.template.flows.UpdateFormulaStateFlow
import com.template.flows.UpdateValueStateFlow
import net.corda.client.jackson.JacksonSupport
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

    private var spreadsheetId = ""
    private var formulaId = ""

    @GetMapping(value = "get-spreadsheet", produces = ["application/json"])
    private fun getSpreadsheet(
            @QueryParam(value = "id") id: Int
    ) = try {
        val spreadsheet = proxy.startTrackedFlow(::GetSpreadsheetFlow).returnValue.get()
                ?: proxy.startTrackedFlow(::CreateSpreadsheetFlow).returnValue.get()

        val renderableCells = spreadsheet.valueStates.map {
            val state = it.state.data
            RenderableCell(state.rowId, state.columnId, state.data, null)
        } + RenderableCell(
                spreadsheet.valueStates.map { it.state.data.rowId }.max() ?: 0,
                0,
                null,
                spreadsheet.formulaState.state.data.formula
        )

        spreadsheetId = spreadsheet.linearId
        formulaId = spreadsheet.formulaState.state.data.linearId.toString()

        val res = renderableCells.sortedWith(compareBy({ it.row }, { it.col })).map { listOf(it.d, it.f) }
        objectMapper.writeValueAsString(res)
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
            proxy.startTrackedFlow(::UpdateValueStateFlow, spreadsheetId, row, col, d.toString())
        else
            proxy.startTrackedFlow(::UpdateFormulaStateFlow, formulaId, f)

        "Successful cell update."
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }
}

data class RenderableCell(val row: Int, val col: Int, val d: String?, val f: String?)