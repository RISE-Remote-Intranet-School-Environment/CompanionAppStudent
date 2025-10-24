package be.ecam.companion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.appModule
import be.ecam.companion.ui.CalendarScreen
import be.ecam.companion.ui.SettingsScreen
import be.ecam.companion.ui.LoginScreen
import be.ecam.companion.viewmodel.HomeViewModel
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.calendar
import companion.composeapp.generated.resources.home
import companion.composeapp.generated.resources.settings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(extraModules: List<Module> = emptyList()) {
    KoinApplication(application = { modules(appModule + extraModules) }) {
        val vm = koinInject<HomeViewModel>()
        MaterialTheme {
            // ✅ Ajout d’un état pour gérer la connexion
            var isLoggedIn by remember { mutableStateOf(false) }

            if (!isLoggedIn) {
                // --- Écran de connexion ---
                LoginScreen(
                    onLogin = { email, password ->
                        // Ici tu pourras plus tard ajouter ta logique d’authentification réelle
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoggedIn = true
                        }
                    }
                )
            } else {
                // --- Interface principale ---
                var selectedScreen by remember { mutableStateOf(BottomItem.HOME) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = selectedScreen != BottomItem.CALENDAR,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                            Text("Drawer content here")
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(selectedScreen.getLabel()) },
                                navigationIcon = {
                                    if (selectedScreen != BottomItem.CALENDAR) {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                BottomItem.entries.forEach { item ->
                                    NavigationBarItem(
                                        selected = selectedScreen == item,
                                        onClick = { selectedScreen = item },
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
                    ) { paddingValues ->
                        // Contenu principal
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (selectedScreen) {
                                BottomItem.HOME -> {
                                    LaunchedEffect(Unit) { vm.load() }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = selectedScreen.getLabel(),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        if (vm.lastErrorMessage.isNotEmpty()) {
                                            Text(vm.lastErrorMessage, color = MaterialTheme.colorScheme.error)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        Text(vm.helloMessage)
                                    }
                                }

                                BottomItem.CALENDAR -> {
                                    LaunchedEffect(Unit) { vm.load() }
                                    CalendarScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        scheduledByDate = vm.scheduledByDate
                                    )
                                }

                                BottomItem.SETTINGS -> {
                                    val settingsRepo = koinInject<SettingsRepository>()
                                    SettingsScreen(repo = settingsRepo, onSaved = { scope.launch { vm.load() } })
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
    HOME, CALENDAR, SETTINGS;

    @Composable
    fun getLabel() = when (this) {
        HOME -> stringResource(Res.string.home)
        CALENDAR -> stringResource(Res.string.calendar)
        SETTINGS -> stringResource(Res.string.settings)
    }

    fun getIconRes() = when (this) {
        HOME -> Icons.Filled.Home
        CALENDAR -> Icons.Filled.CalendarMonth
        SETTINGS -> Icons.Filled.Settings
    }
}