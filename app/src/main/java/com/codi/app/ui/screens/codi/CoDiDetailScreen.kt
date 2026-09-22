package com.codi.app.ui.screens.codi

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codi.app.data.CoDiProfile
import com.codi.app.data.SwipeStatus
import com.codi.app.ui.components.CoDiOutlineButton
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.components.InterestChip
import com.codi.app.ui.components.PlaceholderAvatar
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.CoDiGradients
import com.codi.app.ui.theme.PillShape
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

/** Detalle completo de un candidato a CoDi. Se abre al tocar la tarjeta central de Home. */
@Composable
fun CoDiDetailScreen(
    profileId: String,
    onBack: () -> Unit,
    onMatch: (profileId: String) -> Unit,
    onOpenChat: (contactName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoDiDetailViewModel = viewModel()
) {
    LaunchedEffect(profileId) { viewModel.load(profileId) }
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DetailHeader(profile = profile, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            val current = profile
            if (current == null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Este perfil ya no está disponible.",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                return@Column
            }

            Spacer(Modifier.height(20.dp))
            CompatibilityCard(compatibility = current.compatibility)

            Spacer(Modifier.height(20.dp))
            SectionTitle("Sobre mí")
            Spacer(Modifier.height(8.dp))
            Surface(shape = MaterialTheme.shapes.large, color = SurfaceCard, modifier = Modifier.fillMaxWidth()) {
                Text(
                    current.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Intereses")
            Spacer(Modifier.height(8.dp))
            InterestGrid(interests = current.interests)

            Spacer(Modifier.height(20.dp))
            SectionTitle("Datos de convivencia")
            Spacer(Modifier.height(8.dp))
            Surface(shape = MaterialTheme.shapes.large, color = SurfaceCard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DetailRow(Icons.Outlined.Work, "Ocupación", current.occupation)
                    DetailRow(Icons.Outlined.AttachMoney, "Presupuesto", "$${current.budget} al mes")
                    DetailRow(Icons.Outlined.Schedule, "Horarios", current.schedule)
                    DetailRow(
                        Icons.Outlined.CleaningServices,
                        "Orden y limpieza",
                        "${current.cleanliness} de 5"
                    )
                    DetailRow(
                        if (current.smoker) Icons.Outlined.SmokingRooms else Icons.Outlined.SmokeFree,
                        "Fuma",
                        if (current.smoker) "Sí" else "No"
                    )
                    DetailRow(
                        Icons.Outlined.Pets,
                        "Mascotas",
                        if (current.petFriendly) "Acepta mascotas" else "Prefiere sin mascotas"
                    )
                    DetailRow(Icons.Outlined.CalendarMonth, "Se muda", current.moveInDate)
                }
            }

            Spacer(Modifier.height(24.dp))
            DetailActions(
                profile = current,
                onMeInteresa = {
                    viewModel.meInteresa()
                    onMatch(current.id)
                },
                onPasar = {
                    viewModel.pasar()
                    onBack()
                },
                onOpenChat = { onOpenChat(current.name) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailHeader(profile: CoDiProfile?, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(CoDiGradients.Brand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlaceholderAvatar(
                seed = profile?.colorSeed ?: 0,
                size = 120.dp,
                icon = Icons.Outlined.Person,
                modifier = Modifier.background(Color.White.copy(alpha = 0.25f), CircleShape)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = profile?.let { "${it.name}, ${it.age}" } ?: "Perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            if (profile != null) {
                Surface(shape = PillShape, color = Color.White.copy(alpha = 0.2f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = Color.White)
        }
    }
}

@Composable
private fun CompatibilityCard(compatibility: Int) {
    Surface(shape = MaterialTheme.shapes.large, color = SurfaceCard, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { compatibility / 100f },
                color = AccentBlue,
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
private fun InterestGrid(interests: List<String>) {
    // Dos por renglón: evita un LazyRow anidado dentro de la columna con scroll.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        interests.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { interest ->
                    InterestChip(text = interest)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.9f)
        )
        Spacer(Modifier.width(8.dp))
        // El valor se reparte el ancho con la etiqueta: los textos largos
        // (por ejemplo los horarios) se acomodan en varias líneas en lugar
        // de encimarse sobre la etiqueta.
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun DetailActions(
    profile: CoDiProfile,
    onMeInteresa: () -> Unit,
    onPasar: () -> Unit,
    onOpenChat: () -> Unit
) {
    when (profile.status) {
        SwipeStatus.INTERESA -> CoDiPrimaryButton(
            text = "Enviar mensaje",
            onClick = onOpenChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )

        else -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoDiOutlineButton(
                text = "Pasar",
                onClick = onPasar,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            )
            CoDiPrimaryButton(
                text = "Me Interesa",
                onClick = onMeInteresa,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            )
        }
    }
}
