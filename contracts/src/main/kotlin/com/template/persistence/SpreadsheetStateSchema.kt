package com.template.persistence

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Table

object SpreadsheetStateSchema

object SpreadsheetStateSchemaV1: MappedSchema(schemaFamily = SpreadsheetStateSchema.javaClass, version = 1, mappedTypes = listOf(PersistentSpreadsheetState::class.java)) {

    @Entity
    @Table(name = "spreadsheet_states")
    class PersistentSpreadsheetState(
            @Column(name = "value_states", nullable = false)
            @ElementCollection
            var valueStates: List<String>,

            @Column(name = "formula_state", nullable = false)
            var formulaState: String,

            @Column(name = "editors", nullable = false)
            @ElementCollection
            var editors: List<Party>,

            @Column(name = "linear_id", nullable = false)
            var linearId: String
    ): PersistentState() {
        // no-arg constructor required by hibernate
        constructor(): this(emptyList(), "", emptyList(), "")
    }

}