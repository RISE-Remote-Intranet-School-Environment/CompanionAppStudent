package be.ecam.companion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.appModule

import be.ecam.companion.ui.components.*
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode
import be.ecam.companion.ui.theme.ScreenSizeMode

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
import be.ecam.companion.viewmodel.LoginViewModel
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    extraModules: List<Module> = emptyList(),
    loginUrlGenerator: (() -> String)? = null,
    navigateToUrl: ((String) -> Unit)? = null,
    // Nouveaux paramètres pour OAuth Desktop
    pendingOAuthResult: Pair<String, String>? = null,
    onOAuthResultConsumed: (() -> Unit)? = null
) {

    KoinApplication(application = { modules(appModule + extraModules) }) {

        val vm = koinInject<HomeViewModel>()
        // Initialize LoginViewModel here so it survives across screens
        val loginViewModel = remember { LoginViewModel() }

        // Gérer le callback OAuth Desktop
        LaunchedEffect(pendingOAuthResult) {
            if (pendingOAuthResult != null) {
                val (accessToken, refreshToken) = pendingOAuthResult
                loginViewModel.restoreSession(accessToken)
                onOAuthResultConsumed?.invoke()
            }
        }

        var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }
        var textScaleMode by remember { mutableStateOf(TextScaleMode.NORMAL) }
        var screenSizeMode by remember { mutableStateOf(ScreenSizeMode.Default) }
        var showNotifications by remember { mutableStateOf(false) }
        val baseDensity = LocalDensity.current

        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * screenSizeMode.scale,
                fontScale = textScaleMode.fontScale
            )
        ) {
            MaterialTheme(
                colorScheme = themeMode.colorScheme()
            ) {

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
                            viewModel = loginViewModel,
                            onLoginSuccess = { isLoggedIn = true },
                            onNavigateToRegister = { showRegister = true },
                            loginUrlGenerator = loginUrlGenerator,
                            navigateToUrl = navigateToUrl
                        )
                    }
                    return@MaterialTheme
                }

                // --- KEY STEP: Retrieve the connected username ---
                // Use Nirina Crépin as the default fallback
                val connectedUser = loginViewModel.currentUser

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                var selectedScreen by remember { mutableStateOf(BottomItem.HOME) }
                var showCoursesPage by remember { mutableStateOf(false) }
                var showProfessorsPage by remember { mutableStateOf(false) }
                var showPaePage by remember { mutableStateOf(false) }
                var coursesTitleSuffix by remember { mutableStateOf<String?>(null) }
                var paeTitleSuffix by remember { mutableStateOf<String?>(null) }
                var coursesResetCounter by remember { mutableStateOf(0) }
                var courseCalendarInitialYearOption by remember { mutableStateOf<String?>(null) }
                var courseCalendarInitialSeries by remember { mutableStateOf<String?>(null) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        AppDrawer(
                            user = connectedUser,
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
                                loginViewModel.logout() 
                                isLoggedIn = false
                                showPaePage = false
                                paeTitleSuffix = null
                                showCoursesPage = false
                                showProfessorsPage = false
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
                                screenSizeMode = screenSizeMode,
                                onZoomChange = { screenSizeMode = screenSizeMode.next() },
                                textScaleMode = textScaleMode,
                                onToggleTextScale = { textScaleMode = textScaleMode.next() },
                                themeMode = themeMode,
                                onToggleTheme = { themeMode = themeMode.toggle() },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                showNotifications = showNotifications,
                                onNotificationsClick = { showNotifications = !showNotifications }
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
                                    courseCalendarInitialYearOption = null
                                    courseCalendarInitialSeries = null
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
                                authToken = loginViewModel.jwtToken,
                                onContextChange = { coursesTitleSuffix = it },
                                onOpenCourseCalendar = { yearOption, series ->
                                    showCoursesPage = false
                                    showProfessorsPage = false
                                    showPaePage = false
                                    coursesTitleSuffix = null
                                    courseCalendarInitialYearOption = yearOption
                                    courseCalendarInitialSeries = series
                                    selectedScreen = BottomItem.COURSECALENDAR
                                }
                            )

                            showProfessorsPage -> ProfessorsScreen(
                                modifier = baseModifier,
                                authToken = loginViewModel.jwtToken
                            )

                            showPaePage -> MonPaeScreen(
                                modifier = baseModifier,
                                userIdentifier = connectedUser?.username?:"",
                                authToken = loginViewModel.jwtToken,
                                onContextChange = { paeTitleSuffix = it }
                            )

                            else -> when (selectedScreen) {
                                BottomItem.HOME -> {
                                    // Remove the manual vm.load() call here
                                    // HomeScreen now handles loading internally via its LaunchedEffect
                                    HomeScreen(
                                        modifier = baseModifier,
                                        vm = vm,
                                        loginViewModel = loginViewModel,
                                        
                                    )
                                }

                                BottomItem.COURSECALENDAR -> {
                                    StudentCourseCalendar(
                                        modifier = baseModifier,
                                        initialYearOption = courseCalendarInitialYearOption,
                                        initialSeries = courseCalendarInitialSeries,
                                        username = connectedUser?.username?:"",
                                        authToken = loginViewModel.jwtToken
                                    )
                                }

                                BottomItem.SETTINGS -> {
                                    val settingsRepo = koinInject<SettingsRepository>()
                                    SettingsScreen(
                                        repo = settingsRepo,
                                        modifier = baseModifier,
                                        onSaved = { scope.launch { vm.load(connectedUser) } }
                                    )
                                }

                                BottomItem.DASHBOARD -> {
                                    UserDashboardScreen(
                                        loginViewModel = loginViewModel,
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
}
