package com.template.states

import com.template.contracts.FormulaContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(FormulaContract::class)
data class FormulaState(val formula: String, val editors: List<Party>,override val participants: List<AbstractParty> = editors) : ContractState