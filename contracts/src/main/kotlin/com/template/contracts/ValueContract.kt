package com.template.contracts

import com.template.states.ValueState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class ValueContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.SpreadsheetContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val valueCommands = tx.commands.map { it.value }.filterIsInstance<Commands>()
        valueCommands.forEach { command ->
            when (command) {
                Commands.Update() -> {
                    val inputStates = tx.inputStates
                    val outputStates = tx.outputStates
                    requireThat { "single input" using (inputStates.size == 1) }
                    requireThat { "single output" using (outputStates.size == 1) }

                    val inputState = inputStates.first() as ValueState
                    val outputState = outputStates.first() as ValueState

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