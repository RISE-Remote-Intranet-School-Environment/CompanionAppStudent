package be.ecam.companion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.appModule

import be.ecam.companion.ui.components.*

import be.ecam.companion.ui.screens.CalendarScreen
import be.ecam.companion.ui.screens.CoursesFormationScreen
import be.ecam.companion.ui.screens.HomeScreen
import be.ecam.companion.ui.screens.LoginScreen
import be.ecam.companion.ui.screens.RegisterScreen
import be.ecam.companion.ui.screens.SettingsScreen
import be.ecam.companion.ui.screens.UserDashboardScreen
import be.ecam.companion.ui.screens.ProfessorsScreen
import be.ecam.companion.ui.screens.MonPaeScreen


import be.ecam.companion.viewmodel.HomeViewModel
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.nicolas
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.clip
import companion.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import companion.composeapp.generated.resources.nicolas



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
            var showPaePage by remember { mutableStateOf(false) }
            var coursesTitleSuffix by remember { mutableStateOf<String?>(null) }
            var paeTitleSuffix by remember { mutableStateOf<String?>(null) }
            var coursesResetCounter by remember { mutableStateOf(0) }



            ModalNavigationDrawer(
                drawerState = drawerState,
                // Allow swipe/edge gestures to open/close the drawer on every screen
                gesturesEnabled = true,
                drawerContent = {
                    AppDrawer(
                        onSelectDashboard = {
                            selectedScreen = BottomItem.DASHBOARD
                            showPaePage = false
                            paeTitleSuffix = null
                            showCoursesPage = false
                            showProfessorsPage = false
                            scope.launch { drawerState.close() }
                        },
                        onSelectCourses = {
                            showCoursesPage = true
                            coursesTitleSuffix = null
                            showPaePage = false
                            paeTitleSuffix = null
                            showProfessorsPage = false
                            coursesResetCounter++
                            scope.launch { drawerState.close() }
                        },
                        onSelectProfessors = {
                            showProfessorsPage = true
                            showPaePage = false
                            paeTitleSuffix = null
                            showCoursesPage = false
                            scope.launch { drawerState.close() }
                        },
                        onSelectPae = {
                            showPaePage = true
                            paeTitleSuffix = null
                            showCoursesPage = false
                            showProfessorsPage = false
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            showPaePage = false
                            paeTitleSuffix = null
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
                            showPaePage = showPaePage,
                            coursesTitleSuffix = coursesTitleSuffix,
                            paeTitleSuffix = paeTitleSuffix,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    },
                    bottomBar = {
                        BottomBar(
                            selected = selectedScreen,
                            onSelect = { item ->
                                showCoursesPage = false
                                showProfessorsPage = false
                                showPaePage = false
                                coursesTitleSuffix = null
                                paeTitleSuffix = null
                                selectedScreen = item
                            }
                        )
                    }
                ) { paddingValues ->

                    val baseModifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)

                    when {
                        showCoursesPage -> CoursesFormationScreen(
                            modifier = baseModifier,
                            resetTrigger = coursesResetCounter,
                            onContextChange = { coursesTitleSuffix = it }
                        )

                        showProfessorsPage -> ProfessorsScreen(baseModifier)

                        showPaePage -> MonPaeScreen(
                            modifier = baseModifier,
                            onContextChange = { paeTitleSuffix = it }
                        )

                        else -> when (selectedScreen) {
                            BottomItem.HOME -> {
                                LaunchedEffect(Unit) { vm.load() }
                                HomeScreen(modifier = baseModifier, vm = vm)
                            }

                            BottomItem.EVENTCALENDAR -> {
                                LaunchedEffect(Unit) { vm.load() }
                                CalendarScreen(
                                    modifier = baseModifier,
                                    scheduledByDate = vm.scheduledByDate
                                )
                            }

                            BottomItem.COURSECALENDAR -> {
                                StudentCourseCalendar(modifier = baseModifier)
                            }

                            BottomItem.SETTINGS -> {
                                val settingsRepo = koinInject<SettingsRepository>()
                                SettingsScreen(
                                    repo = settingsRepo,
                                    modifier = baseModifier,
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
