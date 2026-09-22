package com.codi.app.ui.screens.match

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.codi.app.ui.components.InterestChip
import com.codi.app.ui.components.PlaceholderAvatar
import com.codi.app.ui.components.UserAvatar
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.components.SectionBadge
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.CoDiGradients
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

@Composable
fun MatchScreen(
    profileId: String,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchViewModel = viewModel()
) {
    LaunchedEffect(profileId) { viewModel.load(profileId) }
    val uiState by viewModel.uiState.collectAsState()

    val matchedProfile = uiState.profile
    val name = matchedProfile?.name.orEmpty()
    val zone = matchedProfile?.zone.orEmpty()
    val compatibility = matchedProfile?.compatibility ?: 0
    val sharedInterests = uiState.sharedInterests

    // En cuanto se conoce el nombre, se prepara la conversación para que el
    // botón "Enviar mensaje" ya tenga chat al cual entrar.
    LaunchedEffect(name) {
        if (name.isNotBlank()) viewModel.prepararChat(name)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CoDiGradients.MatchBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                SectionBadge(text = "NUEVA CONEXIÓN", modifier = Modifier.align(Alignment.Center))
            }

            val entranceState = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = entranceState,
                enter = fadeIn(tween(450)) + scaleIn(initialScale = 0.85f, animationSpec = tween(450)),
                modifier = Modifier.weight(1f)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "¡Es un Match!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildAnnotatedString {
                        append("A $name también le gustaría ser tu CoDi en ")
                        withStyle(SpanStyle(color = AccentBlue, fontWeight = FontWeight.Bold)) {
                            append(zone)
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(Modifier.height(32.dp))
                OverlappingAvatars(
                    userPhotoUri = uiState.user?.photoUri,
                    userSeed = uiState.user?.colorSeed ?: 99,
                    matchSeed = matchedProfile?.colorSeed ?: name.hashCode()
                )
                Spacer(Modifier.height(32.dp))

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = SurfaceCard.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = AccentBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$compatibility% DE COMPATIBILIDAD",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Comparten estilo de vida, horarios y gustos parecidos. " +
                                "¡Podrían llevarse muy bien como CoDis!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (sharedInterests.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sharedInterests) { interest ->
                                    InterestChip(text = interest)
                                }
                            }
                        }
                    }
                }
            }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                CoDiPrimaryButton(
                    text = "Enviar mensaje",
                    onClick = { if (name.isNotBlank()) onSendMessage(name) },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlappingAvatars(userPhotoUri: String?, userSeed: Int, matchSeed: Int) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
    ) {
        UserAvatar(
            photoUri = userPhotoUri,
            colorSeed = userSeed,
            size = 96.dp,
            icon = Icons.Outlined.Person,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        )
        PlaceholderAvatar(
            seed = matchSeed,
            size = 96.dp,
            icon = Icons.Outlined.Person,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        )
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
        )
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomStart)
        )
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
        )
    }
}
