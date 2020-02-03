package com.template.contracts

import org.junit.Test
import kotlin.test.assertEquals

class FormulaContractTests {

    private val formulaCalculator = FormulaCalculator()
    private val cellToValueMap = hashMapOf("A_1" to "2", "A_11" to "7")


    @Test
    fun test_calculateFormula_equationIsCalculatedCorrectly() {
        val result = formulaCalculator.calculateFormula("20+(4/2)", cellToValueMap)
        assertEquals(result, "22")
    }

    @Test
    fun test_calculateFormula_cellValuesAreExtractedCorrectly() {
        val result = formulaCalculator.calculateFormula("A_1+A_11", cellToValueMap)
        assertEquals(result, "9.0")
    }

    @Test
    fun test_calculateFormula_mixEquationWorks() {
        val result = formulaCalculator.calculateFormula("A_1*5-(A_11+1)", cellToValueMap)
        assertEquals(result, "2.0")
    }
}