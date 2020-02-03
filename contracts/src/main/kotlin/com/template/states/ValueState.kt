package com.template.states

import com.template.contracts.SpreadsheetContract
import com.template.contracts.ValueContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(ValueContract::class)
data class ValueState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState