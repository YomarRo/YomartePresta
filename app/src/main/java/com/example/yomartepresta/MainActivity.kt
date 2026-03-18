package com.example.yomartepresta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.yomartepresta.ui.MainViewModel
import com.example.yomartepresta.ui.admin.AdminDashboardScreen
import com.example.yomartepresta.ui.auth.LoginScreen
import com.example.yomartepresta.ui.auth.RegistrationScreen
import com.example.yomartepresta.ui.dashboard.DashboardScreen
import com.example.yomartepresta.ui.loan.LoanRequestScreen
import com.example.yomartepresta.ui.payment.PaymentScreen
import com.example.yomartepresta.ui.profile.ProfileScreen
import com.example.yomartepresta.ui.theme.YomarTePrestaTheme
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YomarTePrestaTheme {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                val mainViewModel: MainViewModel = viewModel()
                
                val userProfile by mainViewModel.user.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(userProfile, currentRoute) {
                    val user = userProfile
                    val currentUser = auth.currentUser
                    
                    if (currentUser != null && user != null) {
                        if (currentUser.email == "yomarhdg34@gmail.com") return@LaunchedEffect
                        if (currentRoute?.startsWith("register") == true) return@LaunchedEffect
                        
                        if (user.firstName.isEmpty()) {
                            val encodedName = URLEncoder.encode(currentUser.displayName ?: "", StandardCharsets.UTF_8.toString())
                            val encodedEmail = URLEncoder.encode(currentUser.email ?: "", StandardCharsets.UTF_8.toString())
                            navController.navigate("register_complete/$encodedName/$encodedEmail") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                val startDestination = if (auth.currentUser != null) "dashboard" else "login"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                            onGoogleSignInNewUser = { name, email, _ ->
                                val encodedName = URLEncoder.encode(name ?: "", StandardCharsets.UTF_8.toString())
                                val encodedEmail = URLEncoder.encode(email ?: "", StandardCharsets.UTF_8.toString())
                                navController.navigate("register_complete/$encodedName/$encodedEmail")
                            }
                        )
                    }
                    composable("register_complete/{name}/{email}", arguments = listOf(navArgument("name") { type = NavType.StringType }, navArgument("email") { type = NavType.StringType })) { backStackEntry ->
                        val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
                        val email = URLDecoder.decode(backStackEntry.arguments?.getString("email") ?: "", StandardCharsets.UTF_8.toString())
                        RegistrationScreen(initialEmail = email, initialName = name, isGoogleAuth = true, onRegistrationSuccess = { navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } }, onBack = { navController.popBackStack() })
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            onRequestLoan = { navController.navigate("loan_request") },
                            onNavigateToAdmin = { navController.navigate("admin_dashboard") },
                            onLogout = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                            onPayLoan = { loan -> navController.navigate("payment/${loan.id}") },
                            onEditProfile = { navController.navigate("profile") }
                        )
                    }
                    composable("profile") {
                        val user by mainViewModel.user.collectAsState()
                        user?.let { ProfileScreen(user = it, onBack = { navController.popBackStack() }) }
                    }
                    composable("admin_edit_user/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        // Obtenemos los usuarios desde el repo a través del ViewModel
                        val allUsers by mainViewModel.repository.getAllUsersFlow().collectAsState(initial = emptyList())
                        val userToEdit = allUsers.find { it.uid == userId }
                        
                        if (userToEdit != null) {
                            ProfileScreen(
                                user = userToEdit, 
                                onBack = { navController.popBackStack() }, 
                                isAdminEdit = true
                            )
                        }
                    }
                    composable("admin_dashboard") {
                        AdminDashboardScreen(
                            onBack = { navController.popBackStack() },
                            onEditUser = { userId -> navController.navigate("admin_edit_user/$userId") }
                        )
                    }
                    composable("loan_request") {
                        LoanRequestScreen(viewModel = mainViewModel, onBack = { navController.popBackStack() })
                    }
                    composable("payment/{loanId}", arguments = listOf(navArgument("loanId") { type = NavType.StringType })) { backStackEntry ->
                        val loanId = backStackEntry.arguments?.getString("loanId")
                        val loans by mainViewModel.loans.collectAsState()
                        val loan = loans.find { it.id == loanId }
                        if (loan != null) {
                            PaymentScreen(loan = loan, viewModel = mainViewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
