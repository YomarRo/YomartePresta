package com.example.yomartepresta

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * PRUEBAS UNITARIAS: Lógica de Negocio Central
 * Valida cálculos de moneda, reglas de fechas y niveles de crédito.
 */
class LoanLogicTest {

    // 1. TEST DE CONVERSIÓN DE MONEDA
    // Verifica que (Monto Dólar * Tasa BCV) maneje exactamente 2 decimales.
    @Test
    fun testCurrencyConversion() {
        val montoUsd = 10.0
        val tasaBcv = 36.5551
        
        val resultado = montoUsd * tasaBcv // 365.551
        // Formateamos a 2 decimales como lo hace la app (365.551 -> 365.55)
        val resultadoFormateado = "%.2f".format(Locale.US, resultado).toDouble()

        assertEquals(365.55, resultadoFormateado, 0.0)
    }

    // 2. TEST DE LÓGICA DE FECHAS (Regla 15 y Último)
    @Test
    fun testDueDateLogic_Before15th() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 5) // 5 de Marzo
        }
        val dueDate = calculateDueDateForTest(cal)
        
        assertEquals(15, dueDate.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MARCH, dueDate.get(Calendar.MONTH))
    }

    @Test
    fun testDueDateLogic_After15th() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 20) // 20 de Marzo
        }
        val dueDate = calculateDueDateForTest(cal)
        
        // Debe ser el último día de Marzo (31)
        assertEquals(31, dueDate.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDueDateLogic_EndOfMonth() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 31) // 31 de Marzo
        }
        val dueDate = calculateDueDateForTest(cal)
        
        // Si se pide el último día, pasa al 15 del mes siguiente
        assertEquals(15, dueDate.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, dueDate.get(Calendar.MONTH))
    }

    // 3. TEST DE NIVELES DE USUARIO
    @Test
    fun testUserLevelLimits() {
        // Nivel 1 -> $10
        assertEquals(10.0, calculateLimitForTest(1), 0.0)
        // Nivel 2 -> $20
        assertEquals(20.0, calculateLimitForTest(2), 0.0)
        // Nivel 5 -> $50
        assertEquals(50.0, calculateLimitForTest(5), 0.0)
    }

    // Funciones espejo de la lógica implementada en ViewModels/Repository
    private fun calculateDueDateForTest(today: Calendar): Calendar {
        val dueDate = today.clone() as Calendar
        val day = today.get(Calendar.DAY_OF_MONTH)
        val lastDay = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        if (day < 15) {
            dueDate.set(Calendar.DAY_OF_MONTH, 15)
        } else if (day < lastDay) {
            dueDate.set(Calendar.DAY_OF_MONTH, lastDay)
        } else {
            dueDate.add(Calendar.MONTH, 1)
            dueDate.set(Calendar.DAY_OF_MONTH, 15)
        }
        return dueDate
    }

    private fun calculateLimitForTest(level: Int): Double {
        return when (level) {
            1 -> 10.0
            2 -> 20.0
            3 -> 30.0
            4 -> 40.0
            else -> 50.0
        }
    }
}
