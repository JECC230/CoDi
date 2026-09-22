package com.codi.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codi.app.data.SessionManager
import com.codi.app.ui.screens.chat.ChatScreen
import com.codi.app.ui.screens.codi.CoDiDetailScreen
import com.codi.app.ui.screens.home.HomeScreen
import com.codi.app.ui.screens.login.LoginScreen
import com.codi.app.ui.screens.login.RegisterScreen
import com.codi.app.ui.screens.match.MatchScreen
import com.codi.app.ui.screens.mensajes.MensajesScreen
import com.codi.app.ui.screens.perfil.PerfilScreen
import com.codi.app.ui.screens.settings.SettingsScreen
import com.codi.app.ui.theme.BackgroundDark
import androidx.compose.ui.unit.dp

/**
 * Ancho a partir del cual se considera "pantalla ancha" (tablet, plegable
 * abierto, landscape grande): a partir de aquí la barra inferior se
 * reemplaza por un riel lateral, siguiendo la guía de Material 3 de mostrar
 * la navegación principal en un [CoDiNavigationRail] en vez de una
 * [CoDiBottomBar] cuando sobra espacio horizontal.
 */
private val WIDE_SCREEN_BREAKPOINT = 600.dp

@Composable
fun CoDiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigation = currentRoute in Routes.BOTTOM_BAR_ROUTES

    /**
     * Navega a una pestaña de la barra inferior. Home es la base fija de la
     * pila (se llega a ella una sola vez, justo después del login), así que
     * cambiar de pestaña siempre resulta en, cuando mucho, [Home, pestaña].
     *
     * A propósito NO se usa `saveState`/`restoreState`: como Login se saca
     * del back stack al iniciar sesión, no hay ningún destino "ancla" que
     * quede realmente en la pila para que `popUpTo` lo detecte y dispare el
     * guardado de estado. Sin ese guardado, pedir `restoreState = true` al
     * navegar hacia una ruta que técnicamente sigue en la pila (porque nunca
     * se llegó a sacar) se vuelve un no-op silencioso.
     *
     * La comprobación de "ya estás aquí" se hace leyendo
     * `navController.currentDestination` directamente en vez del `currentRoute`
     * recolectado con `collectAsState`: ese estado de Compose puede quedar un
     * paso atrás justo después de una navegación reciente, y si se usa para
     * esta guarda, un segundo toque rápido en la barra se descarta por error
     * creyendo que ya se está ahí.
     */
    fun navigateToTab(route: String) {
        if (route == navController.currentDestination?.route) return
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
            launchSingleTop = true
        }
    }

    /** Cierra sesión: limpia toda la pila y regresa a Login desde cero. */
    fun logout() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    // BoxWithConstraints (en vez de WindowSizeClass) para no añadir una
    // dependencia extra solo por esto: el ancho disponible ya nos dice si el
    // dispositivo es compacto o ancho (tablet / landscape / plegable abierto).
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= WIDE_SCREEN_BREAKPOINT

        if (isWideScreen && showNavigation) {
            Row(modifier = Modifier.fillMaxSize()) {
                CoDiNavigationRail(currentRoute = currentRoute, onNavigate = ::navigateToTab)
                CoDiNavHost(
                    navController = navController,
                    onLogout = ::logout,
                    onNavigateToTab = ::navigateToTab,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                )
            }
        } else {
            Scaffold(
                containerColor = BackgroundDark,
                bottomBar = {
                    if (showNavigation) {
                        CoDiBottomBar(currentRoute = currentRoute, onNavigate = ::navigateToTab)
                    }
                }
            ) { innerPadding ->
                CoDiNavHost(
                    navController = navController,
                    onLogout = ::logout,
                    onNavigateToTab = ::navigateToTab,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

private const val TRANSITION_MILLIS = 260

@Composable
private fun CoDiNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Si ya había una sesión activa (no se cerró sesión explícitamente la
    // última vez), se entra directo a Home en vez de pedir login de nuevo.
    val startDestination = remember {
        if (SessionManager.currentUid.value != null) Routes.HOME else Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Transición sutil de fundido entre pantallas: evita el "salto" seco
        // por defecto sin distraer de la navegación real (swipe, tabs, back).
        enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MILLIS)) }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onMeInteresa = { profileId ->
                    navController.navigate(Routes.match(profileId))
                },
                onOpenProfileDetail = { profileId ->
                    navController.navigate(Routes.codiDetail(profileId))
                },
                onOpenMyProfile = { onNavigateToTab(Routes.PERFIL) }
            )
        }

        composable(Routes.MENSAJES) {
            MensajesScreen(
                onOpenChat = { contactName ->
                    navController.navigate(Routes.chat(contactName))
                }
            )
        }

        composable(Routes.PERFIL) {
            PerfilScreen(
                onOpenCoDiDetail = { profileId ->
                    navController.navigate(Routes.codiDetail(profileId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }

        composable(
            route = Routes.MATCH_PATTERN,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { entry ->
            val profileId = entry.arguments?.getString("profileId").orEmpty()
            MatchScreen(
                profileId = profileId,
                onBack = { navController.popBackStack() },
                onSendMessage = { contactName ->
                    navController.navigate(Routes.chat(contactName)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument("contactName") { type = NavType.StringType })
        ) { entry ->
            val contactName = entry.arguments?.getString("contactName").orEmpty()
            ChatScreen(
                contactName = contactName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CODI_DETAIL_PATTERN,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { entry ->
            val profileId = entry.arguments?.getString("profileId").orEmpty()
            CoDiDetailScreen(
                profileId = profileId,
                onBack = { navController.popBackStack() },
                onMatch = { matchedId ->
                    navController.navigate(Routes.match(matchedId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onOpenChat = { contactName ->
                    navController.navigate(Routes.chat(contactName))
                }
            )
        }
    }
}
