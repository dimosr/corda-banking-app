package com.template.contracts

import com.template.states.FormulaState
import com.template.states.ValueState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings

class FormulaContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.SpreadsheetContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val formulaCommands = tx.commands.map { it.value }.filterIsInstance<Commands>()
        formulaCommands.forEach { command ->
            when (command) {
                Commands.Update() -> {
                    val inputStates = tx.inputStates
                    val outputStates = tx.outputStates
                    requireThat { "single input" using (inputStates.size == 1) }
                    requireThat { "single output" using (outputStates.size == 1) }

                    val inputState = inputStates.first() as FormulaState
                    val outputState = outputStates.first() as FormulaState

                    requireThat { "correct version" using (outputState.version == inputState.version + 1) }
                }
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Update : TypeOnlyCommandData(), Commands
    }
}

class FormulaCalculator { // TODO: handle empty cells

    companion object {
        private val engine = ScriptEngineManager().getEngineByName("JavaScript")
        private val cellIdentifierRegex = Regex("[A-Z]+_[0-9]+")

        fun calculateFormula(formula: String, cellToValueMap: HashMap<String, String>): String {
            if (formula == "")
                return ""

            val bindings = SimpleBindings()
            for ((cellName, cellValue) in cellToValueMap) {
                if (cellValue == "") {
                    bindings[cellName] = 0
                } else {
                    bindings[cellName] = cellValue.toFloat()
                }
            }

            var result: Any
            try {
                result = engine.eval(formula, bindings)
            } catch (e: ScriptException) {
                val fixedFormula = replaceUnknownCellValuesWith0(formula)
                result = engine.eval(fixedFormula, bindings)
            }
            return result.toString()
        }

        fun replaceUnknownCellValuesWith0(formula: String): String {
            return formula.replace(cellIdentifierRegex, "0")
        }
    }
}
