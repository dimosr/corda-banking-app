package com.template.persistence

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Table

object FormulaStateSchema

object FormulaStateSchemaV1: MappedSchema(schemaFamily = FormulaStateSchema.javaClass, version = 1, mappedTypes = listOf(PersistentFormulaState::class.java)) {

    @Entity
    @Table(name = "formula_states")
    class PersistentFormulaState(
            @Column(name = "formula", nullable = false)
            var formula: String,

            @Column(name = "editors", nullable = false)
            @ElementCollection
            var editors: List<Party>,

            @Column(name = "linear_id", nullable = false)
            var linearId: String
    ): PersistentState() {
        // no-arg constructor required by hibernate
        constructor(): this("", emptyList(), "")
    }

}