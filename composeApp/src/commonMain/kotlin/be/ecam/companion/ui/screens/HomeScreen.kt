package be.ecam.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.ecam.companion.ui.components.BottomItem
import be.ecam.companion.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        vm.load()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = BottomItem.HOME.getLabel(),
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
