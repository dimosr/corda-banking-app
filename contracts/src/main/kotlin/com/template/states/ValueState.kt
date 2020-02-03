package com.template.states

import com.template.contracts.ValueContract
import com.template.persistence.ValueStateSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(ValueContract::class)
data class ValueState(val data: String, val owner: Party, val watchers: List<Party>, override val linearId: UniqueIdentifier) : LinearState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema) {
            is ValueStateSchemaV1 -> {
                ValueStateSchemaV1.PersistentValueState(data, owner, watchers, linearId.toString())
            }
            else -> throw IllegalArgumentException("Unsupported schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ValueStateSchemaV1)

    override val participants: List<AbstractParty> = watchers + owner
}