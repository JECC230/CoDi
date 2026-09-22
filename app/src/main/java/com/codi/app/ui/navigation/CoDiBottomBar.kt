package com.codi.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextMuted

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_ITEMS = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Outlined.Home),
    BottomItem(Routes.MENSAJES, "Mensajes", Icons.Outlined.ChatBubbleOutline),
    BottomItem(Routes.PERFIL, "Perfil", Icons.Outlined.Person)
)

/** Navegación inferior, usada en teléfonos y pantallas angostas. */
@Composable
fun CoDiBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = SurfaceCard) {
        BOTTOM_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = AccentBlue,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = AccentBlue.copy(alpha = 0.16f)
                )
            )
        }
    }
}

/**
 * Navegación lateral para pantallas anchas (tablets, plegables abiertos,
 * landscape): mismos destinos que [CoDiBottomBar], solo que a un lado en
 * vez de abajo, siguiendo la guía de adaptabilidad de Material 3.
 */
@Composable
fun CoDiNavigationRail(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationRail(containerColor = SurfaceCard) {
        BOTTOM_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = AccentBlue,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = AccentBlue.copy(alpha = 0.16f)
                )
            )
        }
    }
}
