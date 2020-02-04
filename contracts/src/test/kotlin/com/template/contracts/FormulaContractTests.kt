package com.template.contracts

import org.junit.Test
import kotlin.test.assertEquals

class FormulaContractTests {

    private val cellToValueMap = hashMapOf("A_1" to "2", "A_11" to "7")

    @Test
    fun test_calculateFormula_equationIsCalculatedCorrectly() {
        val result = FormulaCalculator.calculateFormula("20+(4/2)", cellToValueMap)
        assertEquals("22", result)
    }

    @Test
    fun test_calculateFormula_cellValuesAreExtractedCorrectly() {
        val result = FormulaCalculator.calculateFormula("A_1+A_11", cellToValueMap)
        assertEquals("9", result)
    }

    @Test
    fun test_calculateFormula_mixEquationWorks() {
        val result = FormulaCalculator.calculateFormula("A_1*5-(A_11+1)", cellToValueMap)
        assertEquals("2", result)
    }

    @Test
    fun test_calculateFormula_unknownBindersAreReplacedWithZero() {
        val result = FormulaCalculator.calculateFormula("A_2+5", cellToValueMap)
        assertEquals("5", result)
    }

    @Test
    fun test_calculateFormula_ZeroIsCorrectlyDisplayed() {
        val result = FormulaCalculator.calculateFormula("A_2", cellToValueMap)
        assertEquals("0", result)
    }

    @Test
    fun test_calculateFormula_FloatIsCorrectlyDisplayed() {
        val result = FormulaCalculator.calculateFormula("0.1+9", cellToValueMap)
        assertEquals("9.1", result)
    }
}