package com.codi.app.ui.screens.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.codi.app.data.UserProfile
import com.codi.app.ui.theme.CoDiGradients
import com.codi.app.ui.theme.GradientPurple
import com.codi.app.ui.theme.PillShape
import com.codi.app.ui.theme.placeholderColor
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codi.app.data.CoDiProfile
import com.codi.app.data.calculateAge
import com.codi.app.ui.components.BirthDateField
import com.codi.app.ui.components.CoDiOutlineButton
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.components.FilterChip
import com.codi.app.ui.components.InterestChip
import com.codi.app.ui.components.PlaceholderAvatar
import com.codi.app.ui.components.UserAvatar
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextMuted
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

@Composable
fun PerfilScreen(
    onOpenCoDiDetail: (profileId: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    val draft = uiState.draft
    var isEditing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Se comprime y se sube al perfil en la nube de inmediato, así que no
        // hace falta conservar el permiso de lectura de la URI de la galería.
        viewModel.updatePhoto(uri)
    }

    LaunchedEffect(uiState.saveError) {
        val message = uiState.saveError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.errorShown()
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            // Se sale del modo edición antes de mostrar el aviso: showSnackbar
            // suspende hasta que el aviso desaparece. savedShown() va al final
            // porque cambia la llave de este efecto y lo cancelaría.
            isEditing = false
            snackbarHostState.showSnackbar("Perfil actualizado")
            viewModel.savedShown()
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mi perfil",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = TextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            ProfileHeroCard(
                user = user,
                isEditing = isEditing,
                onPickPhoto = { photoPickerLauncher.launch("image/*") },
                onEditClick = { isEditing = true }
            )
            Spacer(Modifier.height(16.dp))

            if (!isEditing) {
                ProfileReadOnlyView(
                    user = user,
                    matches = uiState.matches,
                    onEditClick = { isEditing = true },
                    onOpenCoDiDetail = onOpenCoDiDetail
                )
            } else {
                ProfileEditForm(
                    draft = draft,
                    uiState = uiState,
                    viewModel = viewModel,
                    onCancel = {
                        viewModel.discardDraft()
                        isEditing = false
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Tarjeta principal del perfil: el mismo rectángulo con gradiente de marca
 * que la tarjeta de candidatos de Home (esquinas extraLarge, texto blanco,
 * píldoras translúcidas), pero con la foto, nombre y datos propios.
 */
@Composable
private fun ProfileHeroCard(
    user: UserProfile?,
    isEditing: Boolean,
    onPickPhoto: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(CoDiGradients.Brand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfilePhoto(
                photoUri = user?.photoUri,
                colorSeed = user?.colorSeed ?: 99,
                onPickPhoto = onPickPhoto
            )
            Spacer(Modifier.height(14.dp))
            Surface(shape = PillShape, color = Color.White.copy(alpha = 0.2f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        user?.city ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val name = user?.fullName?.takeIf { it.isNotBlank() } ?: "Tu nombre"
            val age = user?.age?.takeIf { it > 0 }
            Text(
                text = if (age != null) "$name, $age" else name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                user?.occupation?.takeIf { it.isNotBlank() } ?: "Agrega tu ocupación",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )
            if (!user?.email.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    user?.email.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        if (!isEditing) {
            // Lápiz: activa el modo edición (misma píldora translúcida que Home).
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyView(
    user: UserProfile?,
    matches: List<CoDiProfile>,
    onEditClick: () -> Unit,
    onOpenCoDiDetail: (String) -> Unit
) {
    // Fila de "stats": tres rectángulos de color, cada uno con un tramo de la paleta de marca.
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            value = user?.age?.takeIf { it > 0 }?.toString() ?: "--",
            label = "Años",
            brush = CoDiGradients.Warm,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatTile(
            value = matches.size.toString(),
            label = if (matches.size == 1) "CoDi" else "CoDis",
            brush = CoDiGradients.Cool,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatTile(
            value = user?.budget?.takeIf { it > 0 }?.let { "\$$it" } ?: "--",
            label = "Presupuesto",
            brush = CoDiGradients.Deep,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }

    Spacer(Modifier.height(16.dp))
    ColorBlock(title = "Sobre mí", brush = CoDiGradients.Cool) {
        Text(
            user?.bio?.takeIf { it.isNotBlank() } ?: "Aún no agregas una biografía. Toca el lápiz para contar quién eres.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f)
        )
        if (!user?.gender.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            WhitePill(text = user?.gender.orEmpty())
        }
    }

    Spacer(Modifier.height(16.dp))
    ColorBlock(title = "Intereses y estilo de vida", brush = CoDiGradients.Warm) {
        val interests = user?.interests.orEmpty()
        if (interests.isEmpty()) {
            Text(
                "Agrega tus intereses para encontrar CoDis más compatibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(12.dp))
            Surface(onClick = onEditClick, shape = PillShape, color = Color.White.copy(alpha = 0.22f)) {
                Text(
                    "Agregar intereses",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            ChipRows(items = interests) { interest -> WhitePill(text = interest) }
        }
    }

    Spacer(Modifier.height(24.dp))
    SectionTitle("Mis CoDis (${matches.size})")
    Spacer(Modifier.height(10.dp))
    if (matches.isEmpty()) {
        Surface(shape = MaterialTheme.shapes.large, color = SurfaceCard, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Todavía no tienes matches. Da \"Me Interesa\" en Home para empezar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            matches.forEach { match ->
                MatchRow(profile = match, onClick = { onOpenCoDiDetail(match.id) })
            }
        }
    }
}

/** Rectángulo de color pequeño con un número grande y su etiqueta. */
@Composable
private fun StatTile(value: String, label: String, brush: Brush, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(brush)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
    }
}

/** Sección del perfil dentro de un rectángulo con gradiente, igual que las tarjetas de Home. */
@Composable
private fun ColorBlock(title: String, brush: Brush, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(brush)
            .padding(20.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun WhitePill(text: String) {
    InterestChip(text = text, containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White)
}

@Composable
private fun ProfileEditForm(
    draft: ProfileDraft,
    uiState: PerfilUiState,
    viewModel: PerfilViewModel,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Editar perfil")
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("Nombre") },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = errorText(uiState.nameError),
                colors = profileFieldColors()
            )
            OutlinedTextField(
                value = draft.lastName,
                onValueChange = viewModel::onLastNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("Apellido") },
                singleLine = true,
                isError = uiState.lastNameError != null,
                supportingText = errorText(uiState.lastNameError),
                colors = profileFieldColors()
            )
        }
        Spacer(Modifier.height(10.dp))

        BirthDateField(
            isoDate = draft.birthDate,
            onDateSelected = viewModel::onBirthDateChange,
            isError = uiState.birthDateError != null,
            supportingText = errorText(uiState.birthDateError)
                ?: { Text("${calculateAge(draft.birthDate).takeIf { draft.birthDate.isNotBlank() } ?: "--"} años", color = TextSecondary) },
            colors = profileFieldColors()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.occupation,
            onValueChange = viewModel::onOccupationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ocupación") },
            singleLine = true,
            isError = uiState.occupationError != null,
            supportingText = errorText(uiState.occupationError),
            colors = profileFieldColors()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.budget,
            onValueChange = viewModel::onBudgetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Presupuesto mensual") },
            singleLine = true,
            isError = uiState.budgetError != null,
            supportingText = errorText(uiState.budgetError),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = profileFieldColors()
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle("Género")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerfilViewModel.GENDER_OPTIONS.forEach { option ->
                FilterChip(
                    text = option,
                    selected = draft.gender == option,
                    onClick = { viewModel.onGenderChange(if (draft.gender == option) "" else option) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Sobre mí")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.bio,
            onValueChange = viewModel::onBioChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            isError = uiState.bioError != null,
            supportingText = {
                val error = uiState.bioError
                Text(
                    text = error ?: "${draft.bio.length}/${PerfilViewModel.MAX_BIO_LENGTH}",
                    color = if (error != null) MaterialTheme.colorScheme.error else TextSecondary
                )
            },
            colors = profileFieldColors()
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle("Intereses y estilo de vida")
        Spacer(Modifier.height(8.dp))
        ChipRows(items = PerfilViewModel.INTEREST_OPTIONS) { option ->
            FilterChip(
                text = option,
                selected = option in draft.interests,
                onClick = { viewModel.onToggleInterest(option) }
            )
        }

        Spacer(Modifier.height(20.dp))
        // Uno sobre otro y a todo lo ancho: en mitades, "Guardar cambios" no
        // cabe en teléfonos angostos y se corta.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CoDiPrimaryButton(
                text = "Guardar cambios",
                onClick = { viewModel.guardarPerfil() },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            CoDiOutlineButton(
                text = "Cancelar",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

private fun errorText(error: String?): (@Composable () -> Unit)? =
    error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
private fun ProfilePhoto(
    photoUri: String?,
    colorSeed: Int,
    onPickPhoto: () -> Unit
) {
    Box(modifier = Modifier.size(116.dp)) {
        UserAvatar(
            photoUri = photoUri,
            colorSeed = colorSeed,
            size = 100.dp,
            icon = Icons.Outlined.Person,
            modifier = Modifier
                .align(Alignment.TopStart)
                .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
        // El botón mide 48dp (mínimo de accesibilidad) aunque el círculo
        // visual se vea más compacto que el resto de la insignia.
        IconButton(
            onClick = onPickPhoto,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = "Cambiar foto de perfil",
                tint = GradientPurple,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Reparte [items] en filas de a 3 para no anidar un LazyRow dentro de la columna con scroll. */
@Composable
private fun ChipRows(items: List<String>, chip: @Composable (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> chip(item) }
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
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Cada CoDi con el que hiciste match: tarjeta oscura sin relleno de color,
 * con un borde fino del degradado de marca; el color de la persona queda
 * solo en su avatar.
 */
@Composable
private fun MatchRow(profile: CoDiProfile, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = SurfaceCard,
        border = BorderStroke(1.dp, CoDiGradients.Brand),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceholderAvatar(seed = profile.colorSeed, size = 44.dp, icon = Icons.Outlined.Person)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${profile.name}, ${profile.age}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${profile.zone} · ${profile.compatibility}% compatible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
