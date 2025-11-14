package be.ecam.companion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.appModule
import be.ecam.companion.ui.CalendarScreen
import be.ecam.companion.ui.CoursesScreen
import be.ecam.companion.ui.LoginScreen
import be.ecam.companion.ui.RegisterScreen
import be.ecam.companion.ui.SettingsScreen
import be.ecam.companion.ui.ProfessorsScreen
import be.ecam.companion.ui.UserDashboardScreen
import be.ecam.companion.viewmodel.HomeViewModel
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.nicolas
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
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
            } else {
                var selectedScreen by remember { mutableStateOf(BottomItem.HOME) }
                var showCoursesPage by remember { mutableStateOf(false) }
                var showProfessorsPage by remember { mutableStateOf(false) }
                var coursesTitleSuffix by remember { mutableStateOf<String?>(null) }
                var coursesResetCounter by remember { mutableStateOf(0) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val drawerScroll = rememberScrollState()
                val contentScroll = rememberScrollState()

                ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = selectedScreen != BottomItem.CALENDAR && !showCoursesPage && !showProfessorsPage,
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxHeight()
                                                        .padding(
                                                                vertical = 12.dp,
                                                                horizontal = 16.dp
                                                        ),
                                        verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        // --- Photo de profil cliquable vers Dashboard ---
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(bottom = 8.dp)
                                                                .clickable {
                                                                    selectedScreen =
                                                                            BottomItem.DASHBOARD
                                                                    scope.launch {
                                                                        drawerState.close()
                                                                    }
                                                                }
                                        ) {
                                            Box(
                                                    modifier =
                                                            Modifier.size(56.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                            MaterialTheme
                                                                                    .colorScheme
                                                                                    .primary
                                                                    ),
                                                    contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                        painter =
                                                                painterResource(
                                                                        Res.drawable.nicolas
                                                                ),
                                                        contentDescription = "Profile Picture",
                                                        modifier =
                                                                Modifier.size(56.dp)
                                                                        .clip(CircleShape)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text("Nicolas Schell")
                                        }

                                        Button(
                                                onClick = {
                                                    showCoursesPage = true
                                                    coursesTitleSuffix = null
                                                    showProfessorsPage = false
                                                    coursesResetCounter++
                                                    scope.launch { drawerState.close() }
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth().padding(top = 16.dp)
                                        ) { Text("Formations") }

                                        Button(
                                                onClick = {
                                                    showProfessorsPage = true
                                                    showCoursesPage = false
                                                    scope.launch { drawerState.close() }
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) { Text("Professeurs") }
                                    }

                                    Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.verticalScroll(drawerScroll)) {
                                        Text("Drawer content here")
                                    }

                                    Column {
                                        HorizontalDivider()
                                        TextButton(
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    isLoggedIn = false
                                                },
                                        ) { Text("Logout") }
                                    }
                                }
                            }
                        }
                ) {
                    Scaffold(
                            topBar = {
                                TopAppBar(
                                        title = {
                                            when {
                                                showCoursesPage -> {
                                                    val dynamicTitle =
                                                            coursesTitleSuffix?.let {
                                                                "Formations - $it"
                                                            }
                                                                    ?: "Formations"
                                                    Text(dynamicTitle)
                                                }
                                                showProfessorsPage -> Text("Professeurs")
                                                else -> Text(selectedScreen.getLabel())
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(
                                                    onClick = {
                                                        scope.launch { drawerState.open() }
                                                    }
                                            ) {
                                                Icon(
                                                        Icons.Filled.Menu,
                                                        contentDescription = "Open drawer"
                                                )
                                            }
                                        }
                                )
                            },
                            bottomBar = {
                                NavigationBar {
                                    BottomItem.entries.forEach { item ->
                                        if (item != BottomItem.DASHBOARD
                                        ) { // on n'affiche pas l'item dashboard dans la bottom bar
                                            NavigationBarItem(
                                                    selected = selectedScreen == item,
                                                    onClick = {
                                                        showCoursesPage = false
                                                        showProfessorsPage = false
                                                        coursesTitleSuffix = null
                                                        selectedScreen = item
                                                    },
                                                    icon = {
                                                        Icon(
                                                                item.getIconRes(),
                                                                contentDescription = item.getLabel()
                                                        )
                                                    },
                                                    label = { Text(item.getLabel()) },
                                                    alwaysShowLabel = true
                                            )
                                        }
                                    }
                                }
                            }
                    ) { paddingValues ->
                        val baseModifier =
                                Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)

                        when {
                            showCoursesPage -> {
                                CoursesScreen(
                                        modifier = baseModifier,
                                        resetTrigger = coursesResetCounter,
                                        onContextChange = { coursesTitleSuffix = it }
                                )
                            }
                            showProfessorsPage -> {
                                ProfessorsScreen(baseModifier)
                            }
                            else -> {
                                when (selectedScreen) {
                                    BottomItem.HOME -> {
                                        LaunchedEffect(Unit) { vm.load() }
                                        Column(
                                                modifier = baseModifier.verticalScroll(contentScroll),
                                                verticalArrangement = Arrangement.Top,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                    text = selectedScreen.getLabel(),
                                                    style = MaterialTheme.typography.titleLarge
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            if (vm.lastErrorMessage.isNotEmpty()) {
                                                Text(
                                                        vm.lastErrorMessage,
                                                        color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            Text(vm.helloMessage)
                                        }
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
                                        Box(modifier = baseModifier) {
                                            SettingsScreen(
                                                    repo = settingsRepo,
                                                    onSaved = { scope.launch { vm.load() } }
                                            )
                                        }
                                    }
                                    BottomItem.DASHBOARD -> {
                                        UserDashboardScreen(
                                                isAdmin = false,
                                                modifier = baseModifier
                                        ) // mock pour le moment
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class BottomItem {
    HOME,
    CALENDAR,
    SETTINGS,
    DASHBOARD; // nouvel item dashboard

    @Composable
    fun getLabel() =
            when (this) {
                HOME -> "Accueil"
                CALENDAR -> "Calendrier"
                SETTINGS -> "Paramètres"
                DASHBOARD -> "Dashboard"
            }

    fun getIconRes() =
            when (this) {
                HOME -> Icons.Filled.Home
                CALENDAR -> Icons.Filled.CalendarMonth
                SETTINGS -> Icons.Filled.Settings
                DASHBOARD -> Icons.Filled.Dashboard
            }

    companion object {
        val entries = values()
    }
}
