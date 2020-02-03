package com.template.states

import com.template.contracts.ValueContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(ValueContract::class)
data class ValueState(val data: String, val owner: Party, val watchers: List<Party>, override val participants: List<AbstractParty> = watchers + owner, override val linearId: UniqueIdentifier) : LinearState