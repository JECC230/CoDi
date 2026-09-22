package com.codi.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.codi.app.data.CoDiProfile
import com.codi.app.ui.components.InterestChip
import com.codi.app.ui.components.PlaceholderAvatar
import com.codi.app.ui.components.UserAvatar
import com.codi.app.ui.components.CoDiOutlineButton
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.PillShape
import com.codi.app.ui.theme.CoDiGradients
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onMeInteresa: (profileId: String) -> Unit,
    onOpenProfileDetail: (profileId: String) -> Unit,
    onOpenMyProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Si el servidor rechaza una decisión, la tarjeta vuelve al centro.
    var resetToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(saveError) {
        val message = saveError ?: return@LaunchedEffect
        resetToken++
        viewModel.saveErrorShown()
        snackbarHostState.showSnackbar(message)
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(
                userName = uiState.user?.name ?: "CoDi",
                userSeed = uiState.user?.colorSeed ?: 99,
                userPhotoUri = uiState.user?.photoUri,
                onOpenMyProfile = onOpenMyProfile,
                onOpenNotifications = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Los avisos de match y mensajes llegan como notificación del sistema")
                    }
                }
            )
            Spacer(Modifier.height(20.dp))

            val profile = uiState.currentProfile
            when {
                uiState.loading -> LoadingState(modifier = Modifier.weight(1f))

                profile == null -> EmptyDeckState(
                    onReiniciar = { viewModel.reiniciarMazo() },
                    modifier = Modifier.weight(1f)
                )

                else -> SwipeDeck(
                    top = profile,
                    next = uiState.deck.getOrNull(1),
                    resetToken = resetToken,
                    onPasar = { viewModel.pasar() },
                    onMeInteresa = {
                        val matched = viewModel.meInteresa()
                        if (matched != null) onMeInteresa(matched.id)
                    },
                    onOpenProfile = onOpenProfileDetail,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HomeTopBar(
    userName: String,
    userSeed: Int,
    userPhotoUri: String?,
    onOpenMyProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // La foto y el nombre llevan al perfil propio.
        Row(
            modifier = Modifier
                .clickable(onClick = onOpenMyProfile)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUri = userPhotoUri, colorSeed = userSeed, size = 44.dp, icon = Icons.Outlined.Person)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(greetingForNow(), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(userName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(SurfaceCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(
    profile: CoDiProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** -1 (arrastrando hacia "Pasar") .. 1 (hacia "Me Interesa"); se lee en la fase de dibujo. */
    swipeProgress: () -> Float = { 0f }
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CoDiGradients.Brand)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlaceholderAvatar(
                    seed = profile.colorSeed,
                    size = 96.dp,
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.25f), CircleShape)
                )
                Spacer(Modifier.height(14.dp))
                Surface(shape = PillShape, color = Color.White.copy(alpha = 0.2f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            profile.zone,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${profile.name}, ${profile.age}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))
                // FlowRow (sin scroll) en vez de LazyRow: una fila con scroll
                // horizontal se quedaría con el gesto de deslizar la tarjeta.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.interests.forEach { interest ->
                        InterestChip(
                            text = interest,
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f)
                )
                Spacer(Modifier.weight(1f))
                // Pista visual de que la tarjeta se puede abrir.
                Surface(shape = PillShape, color = Color.White.copy(alpha = 0.2f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Ver perfil completo",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            SwipeStamp(
                text = "ME INTERESA",
                rotation = -14f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(22.dp)
                    .graphicsLayer { alpha = swipeProgress().coerceIn(0f, 1f) }
            )
            SwipeStamp(
                text = "PASAR",
                rotation = 14f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(22.dp)
                    .graphicsLayer { alpha = (-swipeProgress()).coerceIn(0f, 1f) }
            )
        }
    }
}

/** Sello que aparece sobre la tarjeta mientras se arrastra hacia un lado. */
@Composable
private fun SwipeStamp(text: String, rotation: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .rotate(rotation)
            .border(3.dp, Color.White, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/**
 * Estado de la tarjeta de arriba del mazo: cuánto se ha arrastrado y la
 * animación de salida. Los botones y el gesto usan la misma animación, así
 * "Pasar" siempre se ve salir hacia la izquierda y "Me Interesa" a la derecha.
 */
@Stable
private class SwipeCardState {
    // Durante el gesto la posición se escribe directo (sin corrutinas) para
    // que al soltar el dedo ya refleje todo lo arrastrado; al soltar, las
    // animaciones continúan desde ese punto.
    private var dragging by mutableStateOf(false)
    private var dragX by mutableFloatStateOf(0f)
    private var dragY by mutableFloatStateOf(0f)
    private val animX = Animatable(0f)
    private val animY = Animatable(0f)

    var width by mutableFloatStateOf(1f)
    var isFlinging by mutableStateOf(false)
        private set

    val offsetX: Float get() = if (dragging) dragX else animX.value
    val offsetY: Float get() = if (dragging) dragY else animY.value

    /** -1..1 respecto al umbral de decisión. */
    val progress: Float get() = (offsetX / (width * DECISION_FRACTION)).coerceIn(-1f, 1f)

    fun startDrag() {
        dragX = animX.value
        dragY = animY.value
        dragging = true
    }

    fun dragBy(dx: Float, dy: Float) {
        dragX += dx
        dragY += dy * 0.35f
    }

    /** Pasa el control del gesto a las animaciones, sin saltos. */
    suspend fun endDrag() {
        animX.snapTo(dragX)
        animY.snapTo(dragY)
        dragging = false
    }

    /** Saca la tarjeta de la pantalla hacia [direction] (-1 izquierda, 1 derecha). */
    suspend fun fling(direction: Int) {
        isFlinging = true
        coroutineScope {
            launch { animX.animateTo(direction * width * 1.4f, tween(FLING_MILLIS, easing = FastOutLinearInEasing)) }
            launch { animY.animateTo(animY.value - width * 0.08f, tween(FLING_MILLIS)) }
        }
    }

    suspend fun settle() {
        isFlinging = false
        coroutineScope {
            launch { animX.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
            launch { animY.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
        }
    }

    companion object {
        const val DECISION_FRACTION = 0.33f
        const val FLING_MILLIS = 280
    }
}

/**
 * Mazo estilo Tinder: la tarjeta de arriba se arrastra con el dedo (o sale
 * con los botones) y deja ver la siguiente, que crece a su lugar conforme la
 * de arriba se aleja.
 */
@Composable
private fun SwipeDeck(
    top: CoDiProfile,
    next: CoDiProfile?,
    resetToken: Int,
    onPasar: () -> Unit,
    onMeInteresa: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val state = remember(top.id) { SwipeCardState() }

    LaunchedEffect(resetToken) {
        if (resetToken > 0) state.settle()
    }

    fun decide(direction: Int) {
        if (state.isFlinging) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            state.fling(direction)
            if (direction < 0) onPasar() else onMeInteresa()
        }
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            state.width = constraints.maxWidth.toFloat()

            if (next != null) {
                key(next.id) {
                    ProfileCard(
                        profile = next,
                        onClick = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = abs(state.progress)
                                val scale = 0.93f + 0.07f * p
                                scaleX = scale
                                scaleY = scale
                                translationY = (1f - p) * 18.dp.toPx()
                                alpha = 0.55f + 0.45f * p
                            }
                    )
                }
            }

            key(top.id) {
                ProfileCard(
                    profile = top,
                    onClick = { onOpenProfile(top.id) },
                    swipeProgress = { state.progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = state.offsetX
                            translationY = state.offsetY
                            rotationZ = state.offsetX / state.width * MAX_ROTATION_DEGREES
                        }
                        .pointerInput(top.id) {
                            detectDragGestures(
                                onDragStart = { if (!state.isFlinging) state.startDrag() },
                                onDragEnd = {
                                    val p = state.progress
                                    scope.launch {
                                        state.endDrag()
                                        when {
                                            p >= 1f -> decide(1)
                                            p <= -1f -> decide(-1)
                                            else -> state.settle()
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        state.endDrag()
                                        state.settle()
                                    }
                                }
                            ) { change, dragAmount ->
                                if (state.isFlinging) return@detectDragGestures
                                change.consume()
                                state.dragBy(dragAmount.x, dragAmount.y)
                            }
                        }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CoDiOutlineButton(
                text = "Pasar",
                onClick = { decide(-1) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            CoDiPrimaryButton(
                text = "Me Interesa",
                onClick = { decide(1) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
        }
    }
}

private const val MAX_ROTATION_DEGREES = 16f

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentBlue)
    }
}

@Composable
private fun EmptyDeckState(onReiniciar: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No hay más perfiles por ahora",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ya revisaste todo el mazo. Puedes volver a empezar desde cero.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(20.dp))
            CoDiPrimaryButton(
                text = "Reiniciar mazo",
                onClick = onReiniciar,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.height(48.dp)
            )
        }
    }
}

/** Saludo según la hora del día. */
private fun greetingForNow(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}
