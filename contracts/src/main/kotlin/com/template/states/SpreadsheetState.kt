package com.template.states

import com.template.contracts.SpreadsheetContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(SpreadsheetContract::class)
data class SpreadsheetState(val data: String, val editors: List<Party>,override val participants: List<AbstractParty> = editors, override val linearId: UniqueIdentifier) : LinearState