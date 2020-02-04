package com.template.states

import com.template.contracts.FormulaContract
import com.template.persistence.FormulaStateSchemaV1
import com.template.persistence.ValueStateSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(FormulaContract::class)
data class FormulaState(val formula: String,
                        val editors: List<Party>,
                        val rowId: Int,
                        val columnId: Int,
                        val version: Int,
                        override val linearId: UniqueIdentifier) : LinearState, ContractState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema) {
            is FormulaStateSchemaV1 -> {
                FormulaStateSchemaV1.PersistentFormulaState(formula, editors, rowId, columnId, 0, linearId.toString())
            }
            else -> throw IllegalArgumentException("Unsupported schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(FormulaStateSchemaV1)

    override val participants: List<AbstractParty> = editors
}