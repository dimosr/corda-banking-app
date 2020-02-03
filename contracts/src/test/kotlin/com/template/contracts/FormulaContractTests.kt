package com.template.contracts

import org.junit.Test
import kotlin.test.assertEquals

class FormulaContractTests {

    private val formulaCalculator = FormulaCalculator()

    @Test
    fun test_calculateFormula_equationIsCalculatedCorrectly() {
        val result = formulaCalculator.calculateFormula("20+(4/2)")
        assertEquals(result, "22")
    }
}