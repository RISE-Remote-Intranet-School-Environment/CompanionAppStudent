package be.ecam.companion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.appModule

import be.ecam.companion.ui.components.*

import be.ecam.companion.ui.screens.*

import be.ecam.companion.viewmodel.HomeViewModel


import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(extraModules: List<Module> = emptyList()) {

    KoinApplication(application = { modules(appModule + extraModules) }) {

        val vm = koinInject<HomeViewModel>()
        MaterialTheme {

            var isLoggedIn by remember { mutableStateOf(false) }
            var showRegister by remember { mutableStateOf(false) }

            if (!isLoggedIn) {
                if (showRegister) {
                    RegisterScreen(
                        onRegisterSuccess = { isLoggedIn = true },
                        onNavigateToLogin = { showRegister = false }
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = { isLoggedIn = true },
                        onNavigateToRegister = { showRegister = true }
                    )
                }
                return@MaterialTheme
            }

            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var selectedScreen by remember { mutableStateOf(BottomItem.HOME) }
            var showCoursesPage by remember { mutableStateOf(false) }
            var showProfessorsPage by remember { mutableStateOf(false) }
            var coursesTitleSuffix by remember { mutableStateOf<String?>(null) }
            var coursesResetCounter by remember { mutableStateOf(0) }

            val scrollState = rememberScrollState()

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = selectedScreen != BottomItem.CALENDAR && !showCoursesPage && !showProfessorsPage,
                drawerContent = {
                    AppDrawer(
                        onSelectDashboard = {
                            selectedScreen = BottomItem.DASHBOARD
                            scope.launch { drawerState.close() }
                        },
                        onSelectCourses = {
                            showCoursesPage = true
                            coursesTitleSuffix = null
                            showProfessorsPage = false
                            coursesResetCounter++
                            scope.launch { drawerState.close() }
                        },
                        onSelectProfessors = {
                            showProfessorsPage = true
                            showCoursesPage = false
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            isLoggedIn = false
                        }
                    )
                }
            ) {

                Scaffold(
                    topBar = {
                        TopBar(
                            selectedScreen = selectedScreen,
                            showCoursesPage = showCoursesPage,
                            showProfessorsPage = showProfessorsPage,
                            coursesTitleSuffix = coursesTitleSuffix,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    },
                    bottomBar = {
                        BottomBar(
                            selectedScreen = selectedScreen,
                            onSelect = { item ->
                                showCoursesPage = false
                                showProfessorsPage = false
                                coursesTitleSuffix = null
                                selectedScreen = item
                            }
                        )
                    }
                ) { padding ->

                    val baseModifier = Modifier
                        .fillMaxSize()
                        .padding(padding)

                    when {
                        showCoursesPage -> CoursesScreen(
                            modifier = baseModifier,
                            resetTrigger = coursesResetCounter,
                            onContextChange = { coursesTitleSuffix = it }
                        )
                        showProfessorsPage -> ProfessorsScreen(baseModifier)
                        else -> when (selectedScreen) {
                            BottomItem.HOME -> {
                                LaunchedEffect(Unit) { vm.load() }
                                HomeScreen(
                                    modifier = baseModifier,
                                    vm = vm
                                )
                            }
                            BottomItem.CALENDAR -> {
                                LaunchedEffect(Unit) { vm.load() }
                                CalendarScreen(
                                    modifier = baseModifier,
                                    scheduledByDate = vm.scheduledByDate
                                )
                            }
                            BottomItem.SETTINGS -> {
                                val settingsRepo = koinInject<SettingsRepository>()
                                SettingsScreen(
                                    repo = settingsRepo,
                                    onSaved = { scope.launch { vm.load() } }
                                )
                            }
                            BottomItem.DASHBOARD -> {
                                UserDashboardScreen(
                                    isAdmin = false,
                                    modifier = baseModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
