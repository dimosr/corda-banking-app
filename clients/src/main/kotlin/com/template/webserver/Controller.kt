package com.template.webserver

import com.template.flows.*
import net.corda.client.jackson.JacksonSupport
import net.corda.core.messaging.startTrackedFlow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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

    @GetMapping(value = "get-all-spreadsheets", produces = ["application/json"])
    private fun getAllSpreadsheets() = try {
        proxy.startTrackedFlow(::GetAllSpreadsheetsFlow).returnValue.get()
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }

    @GetMapping(value = "get-spreadsheet", produces = ["application/json"])
    private fun getSpreadsheet(
            @QueryParam(value = "id") id: String
    ) = try {
        val spreadsheet = proxy.startTrackedFlow(::GetSpreadsheetFlow, id).returnValue.get()
        require(spreadsheet != null) { "Sheet with id $id does not exist." }

        val renderableCells = spreadsheet!!.valueStates.map {
            val state = it.state.data
            RenderableCell(state.rowId, state.columnId, state.data, null)
        } + spreadsheet.formulaStates.map {
            val state = it.state.data
            RenderableCell(state.rowId, state.columnId, null, state.formula)
        }

        val res = renderableCells.sortedWith(compareBy({ it.row }, { it.col })).map {
            listOf(it.d, it.f, it.row, it.col)
        }
        objectMapper.writeValueAsString(res)
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }

    @GetMapping(value = "create-spreadsheet", produces = ["application/json"])
    private fun createSpreadsheet() = try {
        proxy.startTrackedFlow(::CreateSpreadsheetFlow).returnValue.get()
        "Spreadsheet created successfully."
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }

    @GetMapping(value = "set-data", produces = ["application/json"])
    private fun setData(
            @QueryParam(value = "id") id: String,
            @QueryParam(value = "d") d: String?,
            @QueryParam(value = "f") f: String?,
            @QueryParam(value = "row") row: Int,
            @QueryParam(value = "col") col: Int
    ) = try {
        require(d != null || f != null) { "Either formula or value must be non trivial value." }

        if (f == null)
            // TODO: update version
            proxy.startTrackedFlow(::UpdateValueStateFlow, id, row, col, d!!, 0).returnValue.get()
        else
            // TODO: update version
            proxy.startTrackedFlow(::UpdateFormulaStateFlow, id, row, col, f, 0).returnValue.get()

        "Successful cell update."
    } catch (e: Exception) {
        Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }
}

data class RenderableCell(val row: Int, val col: Int, val d: String?, val f: String?)