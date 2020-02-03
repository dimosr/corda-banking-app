package com.template.persistence

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Table

object ValueStateSchema

object ValueStateSchemaV1: MappedSchema(schemaFamily = ValueStateSchema.javaClass, version = 1, mappedTypes = listOf(PersistentValueState::class.java)) {

    @Entity
    @Table(name = "value_states")
    class PersistentValueState(
        @Column(name = "data", nullable = false)
        var data: String,

        @Column(name = "owner", nullable = false)
        var owner: Party?,

        @Column(name = "watchers", nullable = false)
        @ElementCollection
        var watchers: List<Party>,

        @Column(name = "row_id", nullable = false)
        var rowId: Int,

        @Column(name = "column_id", nullable = false)
        var columnId: Int,

        @Column(name = "linear_id", nullable = false)
        var linearId: String
    ): PersistentState() {
        // no-arg constructor required by hibernate
        constructor(): this("", null, emptyList(), 0, 0, "")
    }

}