package be.ecam.companion.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import be.ecam.companion.ui.components.BottomItem

@Composable
fun BottomBar(
    selected: BottomItem,
    onSelect: (BottomItem) -> Unit
) {
    NavigationBar {
        BottomItem.entries.forEach { item ->
            if (item != BottomItem.DASHBOARD) { // pas de dashboard dans la bottom bar
                NavigationBarItem(
                    selected = selected == item,
                    onClick = { onSelect(item) },
                    icon = {
                        Icon(item.getIconRes(), contentDescription = item.getLabel())
                    },
                    label = { Text(item.getLabel()) },
                    alwaysShowLabel = true
                )
            }
        }
    }
}
