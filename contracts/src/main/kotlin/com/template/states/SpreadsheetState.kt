package com.template.states

import com.template.contracts.SpreadsheetContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(SpreadsheetContract::class)
data class SpreadsheetState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState