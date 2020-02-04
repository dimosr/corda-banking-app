package com.template.states

import com.template.contracts.SpreadsheetContract
import com.template.persistence.SpreadsheetStateSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(SpreadsheetContract::class)
data class SpreadsheetState(val valueStates: List<UniqueIdentifier>,
                            val formulaStates: List<UniqueIdentifier>,
                            val editors: List<Party>,
                            override val linearId: UniqueIdentifier) : LinearState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SpreadsheetStateSchemaV1 -> {
                SpreadsheetStateSchemaV1.PersistentSpreadsheetState(valueStates.map { it.toString() }, formulaStates.toString(), editors, linearId.toString())
            }
            else -> throw IllegalArgumentException("Unsupported schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SpreadsheetStateSchemaV1)

    override val participants: List<AbstractParty> = editors

    fun addValueState(valueState: UniqueIdentifier) = copy(valueStates = valueStates + valueState)

    fun addFormulaState(formulaState: UniqueIdentifier) = copy(formulaStates = formulaStates + formulaState)
}