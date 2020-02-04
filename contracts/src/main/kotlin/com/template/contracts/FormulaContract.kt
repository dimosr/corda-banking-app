package com.template.contracts

import com.template.states.FormulaState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import org.apache.commons.lang3.StringUtils
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
            when(command) {
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
    interface Commands: CommandData {
        class Update: TypeOnlyCommandData(), Commands
    }
}

class FormulaCalculator { // TODO: handle empty cells

    companion object {
        private val engine = ScriptEngineManager().getEngineByName("JavaScript")
        private val cellIdentifierRegex = Regex("[A-Z]+[0-9]+")
        val invalidFormulaErrorMessage = "INVALID FORMULA"

        fun calculateFormula(formula: String, cellToValueMap: HashMap<String, String>): String {
            try {
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

                return prepareResultString(result)
            } catch (e: Exception) {
                return invalidFormulaErrorMessage
            }

        }

        fun replaceUnknownCellValuesWith0(formula: String): String {
            var fixedFormula = formula

            var regex = Regex("""^[A-Z]+[0-9]+$""")
            var match = regex.find(fixedFormula)
            if(match != null) {
                fixedFormula = "[" + formula + "]"
            }

            regex = Regex("""^[A-Z]+[0-9]+[\+\*\\-]""")
            match = regex.find(fixedFormula)
            if(match != null) {
                val last = match.groups[0]?.range?.last
                val length = last!!

                val part1 = fixedFormula.substring(0, last)
                val part2 = fixedFormula.substring(last)

                fixedFormula = "[" + part1 + "]" + part2
            }

            regex = Regex("""[\+\*\\-][A-Z]+[0-9]+[\+\*\\-]""")
            match = regex.find(fixedFormula)
            while(match != null) {
                val first = match.groups[0]?.range?.first
                val last = match.groups[0]?.range?.last
                val length = last!! - first!! + 1

                val part1 = fixedFormula.substring(0, first+1)
                val part2 = fixedFormula.substring(first+1, last)
                val part3 = fixedFormula.substring(last)

                fixedFormula = part1 + "[" + part2 + "]" + part3
                match = regex.find(fixedFormula)
            }

            regex = Regex("""[\+\*\\-][A-Z]+[0-9]+$""")
            match = regex.find(fixedFormula)
            if(match != null) {
                val first = match.groups[0]?.range?.first
                val length = first!! + 1

                val part1 = fixedFormula.substring(0, first+1)
                val part2 = fixedFormula.substring(first+1)

                fixedFormula = part1 + "[" + part2 + "]"
            }

            return fixedFormula.replace(Regex("""\[[A-Z]+[0-9]+\]"""), "0")
        }

        fun prepareResultString(result: Any): String {
            var resultString = result.toString()
            if(resultString.takeLast(2) == ".0")
                resultString = resultString.substring(0, resultString.length - 2)
            return resultString
        }
    }
}
