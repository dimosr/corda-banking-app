package com.template.flows

import com.template.states.FormulaState
import com.template.states.ValueState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable


@CordaSerializable
data class SpreadsheetDTO(val valueStates: List<StateAndRef<ValueState>>,
                          val formulaStates: List<Pair<StateAndRef<FormulaState>, CalculatedFormulaValue>>,
                          val editors: List<Party>,
                          val linearId: String)

typealias CalculatedFormulaValue = String