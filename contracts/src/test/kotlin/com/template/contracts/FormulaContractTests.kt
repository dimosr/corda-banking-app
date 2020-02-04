package com.template.contracts

import org.junit.Test
import kotlin.test.assertEquals

class FormulaContractTests {

    private val cellToValueMap = hashMapOf("A1" to "2", "A11" to "7")

    @Test
    fun test_calculateFormula_equationIsCalculatedCorrectly() {
        val result = FormulaCalculator.calculateFormula("20+(4/2)", cellToValueMap)
        assertEquals("22", result)
    }

    @Test
    fun test_calculateFormula_cellValuesAreExtractedCorrectly() {
        val result = FormulaCalculator.calculateFormula("A1+A11", cellToValueMap)
        assertEquals("9", result)
    }

    @Test
    fun test_calculateFormula_mixEquationWorks() {
        val result = FormulaCalculator.calculateFormula("A1*5-(A11+1)", cellToValueMap)
        assertEquals("2", result)
    }

    @Test
    fun test_calculateFormula_unknownBindersAreReplacedWithZero() {
        val result = FormulaCalculator.calculateFormula("A2+5", cellToValueMap)
        assertEquals("5", result)
    }

    @Test
    fun test_calculateFormula_ZeroIsCorrectlyDisplayed() {
        val result = FormulaCalculator.calculateFormula("A2", cellToValueMap)
        assertEquals("0", result)
    }

    @Test
    fun test_calculateFormula_FloatIsCorrectlyDisplayed() {
        val result = FormulaCalculator.calculateFormula("0.1+9", cellToValueMap)
        assertEquals("9.1", result)
    }

    @Test
    fun test_calculateFormula_invalidFormula() {
        val result = FormulaCalculator.calculateFormula("1+2(", cellToValueMap)
        assertEquals(FormulaCalculator.invalidFormulaErrorMessage, result)
    }
}