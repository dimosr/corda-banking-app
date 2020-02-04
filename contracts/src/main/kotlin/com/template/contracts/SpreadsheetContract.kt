package com.template.contracts

import com.template.states.FormulaState
import com.template.states.SpreadsheetState
import com.template.states.ValueState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SpreadsheetContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.SpreadsheetContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val spreadsheetCommands = tx.commands.map { it.value }.filterIsInstance<Commands>()
        spreadsheetCommands.forEach { command ->
            when(command) {
                Commands.CreateSpreadsheet() -> {
                    val inputStates = tx.inputStates
                    requireThat { "no inputs" using (inputStates.isEmpty()) }

                    val spreadsheetStates = tx.outputStates.filterIsInstance<SpreadsheetState>()
                    val valueStates = tx.outputStates.filterIsInstance<ValueState>()
                    val formulaStates = tx.outputStates.filterIsInstance<FormulaState>()

                    requireThat { "single spreadsheet" using (spreadsheetStates.size == 1) }
                    val spreadsheet = spreadsheetStates.first()

                    valueStates.forEach { state ->
                        requireThat { "initial version" using (state.version == 0) }
                    }
                    formulaStates.forEach { state ->
                        requireThat { "initial version" using (state.version == 0) }
                    }

                }
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands: CommandData {
        class CreateSpreadsheet: TypeOnlyCommandData(), Commands
    }
}