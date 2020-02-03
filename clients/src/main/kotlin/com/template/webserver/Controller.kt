package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.QueryParam

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

    private fun createSpreadsheet() {}

    @GetMapping(value = "/bounds", produces = ["application/json"])
    private fun bounds(
            @QueryParam(value = "spreadsheetId") spreadsheetId: Int
    ) = "(2, 2)"

    @GetMapping(value = "get-data", produces = ["application/json"])
    private fun getData(
            @QueryParam(value = "row") row: Int,
            @QueryParam(value = "col") col: Int
    ) = CellValue("A1", "99")

    @GetMapping(value = "set-data", produces = ["application/json"])
    private fun setData(
            @QueryParam(value = "row") row: Int,
            @QueryParam(value = "col") col: Int,
            @QueryParam(value = "value") value: String
    ) = "Done"
}