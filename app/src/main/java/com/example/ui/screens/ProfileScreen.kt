package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.HirafiCategories
import com.example.data.model.MoroccanLocations
import com.example.data.model.PortfolioItem
import com.example.data.model.UserRole
import com.example.ui.components.LivePulseDot
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.viewmodel.HirafiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: HirafiViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val s = hirafiStrings()
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val portfolio by viewModel.myPortfolio.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val supabaseAuth by viewModel.supabaseAuthState.collectAsStateWithLifecycle()

    var fullName by remember(session.userId) { mutableStateOf(session.fullName) }
    var phone by remember(session.userId) { mutableStateOf(session.phone) }
    var city by remember(session.userId) { mutableStateOf(session.city) }
    var avatarUri by remember(session.userId) { mutableStateOf(session.avatarUri) }
    var bio by remember(session.userId) { mutableStateOf(session.bio) }
    var experienceYears by remember(session.userId) { mutableIntStateOf(session.experienceYears) }
    var basePriceDh by remember(session.userId) { mutableIntStateOf(session.basePriceDh) }

    LaunchedEffect(session.city) {
        city = session.city
    }

    var showAddPortfolioDialog by remember { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    val personalInfoTitle = when (s.language) {
        AppLanguage.ARABIC -> "المعلومات الشخصية"
        AppLanguage.FRENCH -> "Informations personnelles"
        AppLanguage.ENGLISH -> "Personal Information"
    }
    val proCardTitle = when (s.language) {
        AppLanguage.ARABIC -> "البطاقة المهنية للحرفي"
        AppLanguage.FRENCH -> "Profil professionnel"
        AppLanguage.ENGLISH -> "Craftsman Pro Card"
    }
    val experienceLabel = when (s.language) {
        AppLanguage.ARABIC -> "سنوات الخبرة"
        AppLanguage.FRENCH -> "Expérience (ans)"
        AppLanguage.ENGLISH -> "Experience (yrs)"
    }
    val basePriceLabel = when (s.language) {
        AppLanguage.ARABIC -> "السعر الابتدائي"
        AppLanguage.FRENCH -> "Tarif de base"
        AppLanguage.ENGLISH -> "Starting Price"
    }
    val bioLabel = when (s.language) {
        AppLanguage.ARABIC -> "النبذة المهنية (تظهر للزبائن)"
        AppLanguage.FRENCH -> "Bio professionnelle (visible aux clients)"
        AppLanguage.ENGLISH -> "Professional Bio (visible to clients)"
    }
    val bioPlaceholder = when (s.language) {
        AppLanguage.ARABIC -> "اذكر خبرتك والأدوات المتوفرة لديك..."
        AppLanguage.FRENCH -> "Décrivez votre expérience et votre équipement..."
        AppLanguage.ENGLISH -> "Describe your experience and tools..."
    }
    val portfolioEmptyTitle = when (s.language) {
        AppLanguage.ARABIC -> "لا توجد أعمال مضافة بعد"
        AppLanguage.FRENCH -> "Aucune réalisation ajoutée"
        AppLanguage.ENGLISH -> "No portfolio items yet"
    }
    val portfolioEmptyHint = when (s.language) {
        AppLanguage.ARABIC -> "انقر على «إضافة عمل» لإبراز مهاراتك أمام الزبائن عند تصفح الخريطة"
        AppLanguage.FRENCH -> "Ajoutez des réalisations pour rassurer les clients sur la carte"
        AppLanguage.ENGLISH -> "Tap Add Work to showcase completed jobs to clients on the map"
    }
    val addWorkLabel = when (s.language) {
        AppLanguage.ARABIC -> "إضافة عمل"
        AppLanguage.FRENCH -> "Ajouter"
        AppLanguage.ENGLISH -> "Add Work"
    }
    val providerDisplay = if (session.authProvider.equals("google", ignoreCase = true)) {
        "Google"
    } else {
        when (s.language) {
            AppLanguage.ARABIC -> "البريد الإلكتروني"
            AppLanguage.FRENCH -> "E-mail"
            AppLanguage.ENGLISH -> "Email"
        }
    }
    val isCloudActive = session.isSupabaseAuthenticated || supabaseAuth != null

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text(s.navProfile, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Profile Avatar Card with ObsidianHeroBrush
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianHeroBrush)
                        .border(1.dp, VoltGlowAccent.copy(alpha = 0.30f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1F2A33))
                                    .border(2.5.dp, VoltGlowAccent, CircleShape)
                                    .clickable {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_picker")
                            ) {
                                if (!avatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUri,
                                        contentDescription = s.navProfile,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = fullName.trim().take(1).ifBlank { "H" },
                                        color = VoltGlowAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 34.sp
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = WepeEmerald,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = s.navProfile,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = fullName.ifBlank { session.fullName },
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp
                            )
                            if (session.isCraftsmanVerified) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = s.kycVerified,
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = VoltGlowAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = VoltGlowAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = session.role.localizedLabel(s),
                                        color = VoltGlowAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (session.role == UserRole.CRAFTSMAN) {
                                val spec = HirafiCategories.byId(session.craftsmanSpecialtyId)
                                Surface(
                                    color = VerifiedEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = spec.localizedName(s),
                                        color = Color(0xFF9DF8CC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (session.email.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Email,
                                    contentDescription = null,
                                    tint = Color(0xFFB5BFC9),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = session.email,
                                    color = Color(0xFFB5BFC9),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Account & Session Readiness + Language Quick Switcher Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        .testTag("profile_session_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.profileSessionCardTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                color = if (isCloudActive) VerifiedEmeraldSoft else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    if (isCloudActive) {
                                        LivePulseDot(color = VerifiedEmerald, dotSize = 7.dp)
                                    } else {
                                        Icon(
                                            Icons.Rounded.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isCloudActive) s.profileCloudSynced else s.connected,
                                        color = if (isCloudActive) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.profileAuthProviderLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = WepeEmeraldSoft,
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text(
                                    text = providerDisplay,
                                    color = WepeEmeraldDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = s.profileChangeLanguage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = appLanguage == lang,
                                    onClick = { viewModel.setAppLanguage(lang) },
                                    label = {
                                        Text(
                                            text = "${lang.shortCode} • ${lang.nativeLabel}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WepeEmerald,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_lang_${lang.code}")
                                )
                            }
                        }
                    }
                }
            }

            // Personal Info Editor Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = personalInfoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(s.authFullNameLabel) },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_fullname_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(s.authPhoneLabel) },
                            placeholder = { Text("0612345678") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.LocationCity,
                                    contentDescription = null,
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = s.authCityLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MoroccanLocations.cities) { cityOption ->
                                    FilterChip(
                                        selected = city == cityOption,
                                        onClick = { city = cityOption },
                                        label = { Text(cityOption, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WepeEmerald,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("profile_city_$cityOption")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Craftsman-only Professional Details Card
            if (session.role == UserRole.CRAFTSMAN) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = proCardTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Surface(
                                color = if (session.isCraftsmanOnline) VerifiedEmeraldSoft else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (session.isCraftsmanOnline) s.craftsmanOnline else s.craftsmanOffline,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (session.isCraftsmanOnline) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = s.craftsmanLiveGpsActive,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = session.isCraftsmanOnline,
                                        onCheckedChange = { viewModel.toggleCraftsmanOnline(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = WepeEmerald
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Experience Years Stepper
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.WorkHistory, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(experienceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = { experienceYears = (experienceYears - 1).coerceAtLeast(0) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Remove, contentDescription = "-")
                                            }
                                            Text(
                                                text = "$experienceYears",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                            IconButton(
                                                onClick = { experienceYears = (experienceYears + 1).coerceAtMost(60) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "+")
                                            }
                                        }
                                    }
                                }

                                // Base Price Stepper
                                Surface(
                                    color = WepeEmeraldSoft,
                                    border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Payments, contentDescription = null, tint = WepeEmeraldDark, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(basePriceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WepeEmeraldDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = { basePriceDh = (basePriceDh - 10).coerceAtLeast(40) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Remove, contentDescription = "-", tint = WepeEmeraldDark)
                                            }
                                            Text(
                                                text = "$basePriceDh ${s.currencyDh}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = WepeEmeraldDark
                                            )
                                            IconButton(
                                                onClick = { basePriceDh = (basePriceDh + 10).coerceAtMost(5000) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "+", tint = WepeEmeraldDark)
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { if (it.length <= 350) bio = it },
                                label = { Text(bioLabel) },
                                placeholder = { Text(bioPlaceholder) },
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_bio_input")
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        viewModel.saveFullProfile(
                            fullName = fullName,
                            phone = phone,
                            city = city,
                            avatarUri = avatarUri,
                            bio = bio,
                            experienceYears = experienceYears,
                            basePriceDh = basePriceDh
                        )
                    },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WepeEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.save, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            // Craftsman Portfolio Gallery Section
            if (session.role == UserRole.CRAFTSMAN) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Workspaces, contentDescription = null, tint = VerifiedEmerald)
                            Column {
                                Text(
                                    text = "${s.navPortfolioBadge} (${portfolio.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = portfolioEmptyHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = { showAddPortfolioDialog = true },
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WepeEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_portfolio_work_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(addWorkLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (portfolio.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(22.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = portfolioEmptyTitle,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = portfolioEmptyHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(portfolio, key = { it.id }) { work ->
                        PortfolioWorkCard(
                            item = work,
                            deleteDescription = s.delete,
                            onDelete = { viewModel.deletePortfolioWork(work.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPortfolioDialog) {
        AddPortfolioWorkDialog(
            onDismiss = { showAddPortfolioDialog = false },
            onSave = { title, description, photoUri ->
                viewModel.addPortfolioWork(title, description, photoUri)
                showAddPortfolioDialog = false
            }
        )
    }
}

@Composable
private fun PortfolioWorkCard(
    item: PortfolioItem,
    deleteDescription: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            if (!item.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.photoUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Surface(
                    color = WepeEmeraldSoft,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Workspaces,
                            contentDescription = null,
                            tint = WepeEmeraldDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = VerifiedEmeraldSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.categoryName,
                        color = VerifiedEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_portfolio_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = deleteDescription,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddPortfolioWorkDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, photoUri: String?) -> Unit
) {
    val s = hirafiStrings()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        photoUri = uri?.toString()
    }

    val dialogTitle = when (s.language) {
        AppLanguage.ARABIC -> "إضافة عمل جديد للمعرض"
        AppLanguage.FRENCH -> "Ajouter une réalisation"
        AppLanguage.ENGLISH -> "Add Portfolio Item"
    }
    val pickPhotoLabel = when (s.language) {
        AppLanguage.ARABIC -> if (photoUri == null) "اختيار صورة العمل (اختياري)" else "تغيير الصورة"
        AppLanguage.FRENCH -> if (photoUri == null) "Choisir une photo (optionnel)" else "Changer la photo"
        AppLanguage.ENGLISH -> if (photoUri == null) "Choose Photo (optional)" else "Change Photo"
    }
    val titleLabel = when (s.language) {
        AppLanguage.ARABIC -> "عنوان العمل"
        AppLanguage.FRENCH -> "Titre du projet"
        AppLanguage.ENGLISH -> "Work Title"
    }
    val descLabel = when (s.language) {
        AppLanguage.ARABIC -> "تفاصيل الإنجاز (اختياري)"
        AppLanguage.FRENCH -> "Détails (optionnel)"
        AppLanguage.ENGLISH -> "Details (optional)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(dialogTitle, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_portfolio_photo_button")
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pickPhotoLabel)
                }

                if (!photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = titleLabel,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text(titleLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portfolio_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 350) description = it },
                    label = { Text(descLabel) },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portfolio_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, photoUri) },
                enabled = title.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WepeEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_portfolio_item_button")
            ) {
                Text(s.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text(s.cancel)
            }
        }
    )
}

