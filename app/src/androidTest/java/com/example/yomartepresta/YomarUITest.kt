package com.example.yomartepresta

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.yomartepresta.data.model.Settings
import com.example.yomartepresta.data.model.User
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.example.yomartepresta.ui.MainViewModel
import com.example.yomartepresta.ui.admin.AdminDashboardScreen
import com.example.yomartepresta.ui.dashboard.DashboardScreen
import com.example.yomartepresta.ui.loan.LoanRequestScreen
import com.example.yomartepresta.ui.theme.YomarTePrestaTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PRUEBAS INSTRUMENTADAS DE INTERFAZ (UI Testing)
 * Utiliza Compose UI Test para validar el flujo de usuario y reglas de negocio en pantalla.
 */
@RunWith(AndroidJUnit4::class)
class YomarUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock del ViewModel para controlar los estados de la UI
    private val mockViewModel = mockk<MainViewModel>(relaxed = true)

    @Test
    fun testLoanRequestFlow_TermsValidation() {
        // Configuramos estados iniciales
        every { mockViewModel.user } returns MutableStateFlow(User(level = 1, idVerificationStatus = "approved"))
        every { mockViewModel.loans } returns MutableStateFlow(emptyList())
        every { mockViewModel.settings } returns MutableStateFlow(Settings(bcvRate = 36.50))
        every { mockViewModel.calculateLoanLimit(any()) } returns 10.0
        every { mockViewModel.getAvailableLimit(any(), any()) } returns 10.0

        composeTestRule.setContent {
            YomarTePrestaTheme {
                LoanRequestScreen(viewModel = mockViewModel, onBack = {})
            }
        }

        // 1. Verificar que el monto inicial sea $10
        composeTestRule.onNodeWithTag("amount_text").assertTextContains("$10")

        // 2. El botón "CONTINUAR" debe estar deshabilitado porque no se han aceptado términos
        composeTestRule.onNodeWithTag("next_step_button").assertIsNotEnabled()

        // 3. Marcamos el checkbox de términos
        composeTestRule.onNodeWithTag("terms_checkbox").performClick()

        // 4. Ahora el botón debe estar habilitado
        composeTestRule.onNodeWithTag("next_step_button").assertIsEnabled()
    }

    @Test
    fun testAdminPanel_UpdateBcvRate() {
        // Mockeamos el repositorio directamente que es el que usa la vista
        val mockRepo = mockk<FirebaseRepository>(relaxed = true)
        every { mockRepo.getSettingsFlow() } returns flowOf(Settings(bcvRate = 36.00))
        
        composeTestRule.setContent {
            YomarTePrestaTheme {
                AdminDashboardScreen(onBack = {}, repository = mockRepo)
            }
        }

        // 1. Navegamos a la pestaña de TASAS (Tab 2)
        composeTestRule.onNodeWithText("TASAS").performClick()

        // 2. Ingresamos una nueva tasa
        composeTestRule.onNodeWithText("Tasa BCV", substring = true).performTextInput("40.00")

        // 3. Verificamos que el botón de actualizar sea visible
        composeTestRule.onNodeWithText("ACTUALIZAR TASA").assertIsDisplayed()
    }

    @Test
    fun testDashboard_PendingVerification_ShowsBanner() {
        // Simulamos usuario pendiente de verificación
        every { mockViewModel.user } returns MutableStateFlow(User(idVerificationStatus = "pending"))
        every { mockViewModel.loans } returns MutableStateFlow(emptyList())
        every { mockViewModel.settings } returns MutableStateFlow(Settings(bcvRate = 36.00))
        every { mockViewModel.isAdmin } returns MutableStateFlow(false)

        composeTestRule.setContent {
            YomarTePrestaTheme {
                DashboardScreen(
                    viewModel = mockViewModel,
                    onRequestLoan = {},
                    onNavigateToAdmin = {},
                    onLogout = {}
                )
            }
        }

        // 1. Debe mostrarse el banner de "VERIFICACIÓN REQUERIDA"
        composeTestRule.onNodeWithText("VERIFICACIÓN REQUERIDA").assertIsDisplayed()
        
        // 2. El botón de solicitar debe estar deshabilitado
        composeTestRule.onNodeWithText("Solicitar Vale").assertIsNotEnabled()
    }
}