package com.template.contracts

import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

class FormulaContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.SpreadsheetContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    class Update: TypeOnlyCommandData()
}

class FormulaCalculator {

    private val engine = ScriptEngineManager().getEngineByName("JavaScript")

    fun calculateFormula(formula: String, cellToValueMap: HashMap<String, String>): String {
        val bindings = SimpleBindings()
        for((cellName, cellValue) in cellToValueMap) {
            bindings[cellName] = cellValue.toFloat()
        }

        val result = engine.eval(formula, bindings)
        return result.toString()
    }

}