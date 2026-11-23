package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.HirafiCategories
import com.example.data.model.LoginValidator
import com.example.data.model.MoroccanLocations
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.remote.SupabaseConfig
import com.example.ui.auth.GoogleAuthAccount
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline

@Composable
fun RoleRegistrationScreen(
    initialSession: UserSession,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    isCloudSaving: Boolean = false,
    authStatusMessage: String? = null,
    isAuthError: Boolean = false,
    onClearAuthMessage: () -> Unit = {},
    currentSupabaseUid: String? = null,
    currentSupabaseEmail: String? = null,
    currentSupabaseDisplayName: String? = null,
    onEmailSignUp: (
        email: String,
        password: String,
        fullName: String,
        city: String,
        role: UserRole,
        specialtyId: String?
    ) -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit = {},
    onGoogleAuthSuccess: (
        account: GoogleAuthAccount,
        preferredDisplayName: String,
        city: String,
        role: UserRole,
        specialtyId: String?,
        autoCreateIfMissing: Boolean
    ) -> Unit,
    onSwitchAccount: (() -> Unit)? = null,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val s = hirafiStrings()

    var isSignUpMode by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var selectedSpecialtyId by remember { mutableStateOf(initialSession.craftsmanSpecialtyId) }
    var fullName by remember(currentSupabaseUid) {
        mutableStateOf(
            when {
                initialSession.isRoleLocked -> initialSession.fullName
                !currentSupabaseDisplayName.isNullOrBlank() -> currentSupabaseDisplayName
                else -> ""
            }
        )
    }
    var email by remember(currentSupabaseUid) {
        mutableStateOf(currentSupabaseEmail.orEmpty())
    }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf(initialSession.city) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }

    val isBusy = isGoogleLoading || isCloudSaving
    val isNameValid = LoginValidator.isValidFullName(fullName)
    val isEmailValid = LoginValidator.isValidEmail(email)
    val isPasswordValid = LoginValidator.isValidPassword(password)
    val chosenSpecialty = if (selectedRole == UserRole.CRAFTSMAN) selectedSpecialtyId else null
    val roleLabel = if (selectedRole == UserRole.CLIENT) {
        UserRole.CLIENT.localizedLabel(s)
    } else {
        "${UserRole.CRAFTSMAN.localizedLabel(s)} (${HirafiCategories.byId(selectedSpecialtyId).localizedName(s)})"
    }

    val passwordScore = remember(password) {
        var pts = 0
        if (password.length >= 6) pts++
        if (password.length >= 8) pts++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) pts++
        if (password.any { !it.isLetterOrDigit() }) pts++
        pts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("role_registration_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Language Selector Pills (AR / FR / EN)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == appLanguage
                Surface(
                    color = if (isSelected) WepeEmerald else Color.White,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, if (isSelected) WepeEmerald else WepeHairline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable { onSelectLanguage(lang) }
                        .testTag("lang_pill_${lang.code}")
                ) {
                    Text(
                        text = "${lang.shortCode} • ${lang.nativeLabel}",
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Obsidian Hero Header
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianHeroBrush)
                .border(1.dp, VoltGlowAccent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = WepeEmerald,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = s.appTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = s.authTagline,
                            color = VoltGlowAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("تفاوض شفاف", "حرفيون موثوقون", "تتبع GPS حي")) { pill ->
                        Surface(
                            color = Color(0xFF20242A),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, VoltGlowAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "✓ $pill",
                                color = VoltGlowAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF20242A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val heroNote = when {
                        !currentSupabaseUid.isNullOrBlank() -> when (s.language) {
                            AppLanguage.ARABIC -> "تم التحقق من حسابك بنجاح! اختر نوع الحساب والمدينة لإتمام الدخول."
                            AppLanguage.FRENCH -> "Compte vérifié ! Choisissez votre rôle et votre ville pour continuer."
                            AppLanguage.ENGLISH -> "Account verified! Choose your role and city to continue."
                        }
                        isSignUpMode -> when (s.language) {
                            AppLanguage.ARABIC -> "تنبيه: نوع الحساب (زبون أو حرفي) يُحفظ ويُقفل لكل حساب بأمان. يمكنك التسجيل بالبريد الإلكتروني أو عبر حساب غوغل."
                            AppLanguage.FRENCH -> "Info : le rôle (Client ou Artisan) est enregistré de manière sécurisée pour chaque compte."
                            AppLanguage.ENGLISH -> "Note: your account role (Client or Craftsman) is securely locked per account."
                        }
                        else -> when (s.language) {
                            AppLanguage.ARABIC -> "أهلاً بعودتك! سجّل الدخول ببريدك الإلكتروني أو عبر حساب غوغل للوصول الفوري لحسابك."
                            AppLanguage.FRENCH -> "Bon retour ! Connectez-vous avec votre e-mail ou Google pour accéder à votre espace."
                            AppLanguage.ENGLISH -> "Welcome back! Sign in with your email or Google to access your account."
                        }
                    }
                    Text(
                        text = heroNote,
                        color = StarAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        if (!SupabaseConfig.isConfigured) {
            Surface(
                color = UrgentCoralSoft,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, UrgentCoral.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = UrgentCoral)
                    Text(
                        text = "إعدادات Supabase غير مضافة بعد. أضف SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets داخل AI Studio ثم أعد تشغيل التطبيق.",
                        color = UrgentCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!currentSupabaseUid.isNullOrBlank()) {
            Surface(
                color = VerifiedEmeraldSoft,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تم التحقق من الجلسة السحابية في Supabase",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VerifiedEmerald
                        )
                        if (!currentSupabaseEmail.isNullOrBlank()) {
                            Text(
                                text = currentSupabaseEmail,
                                fontSize = 12.sp,
                                color = ObsidianBlack
                            )
                        }
                    }
                }
            }
        }

        if (!authStatusMessage.isNullOrBlank()) {
            Surface(
                color = if (isAuthError) UrgentCoralSoft else VerifiedEmeraldSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAuthError) UrgentCoral.copy(alpha = 0.45f) else VerifiedEmerald.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_status_banner")
            ) {
                Text(
                    text = authStatusMessage,
                    color = if (isAuthError) UrgentCoral else VerifiedEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Auth Mode Switcher: Sign In (default) vs Create Account
        if (currentSupabaseUid.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = if (!isSignUpMode) WepeEmerald else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (!isSignUpMode) WepeEmerald else WepeHairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            isSignUpMode = false
                            onClearAuthMessage()
                        }
                        .testTag("auth_mode_signin")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = s.authSignInTab,
                            color = if (!isSignUpMode) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
                Surface(
                    color = if (isSignUpMode) WepeEmerald else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSignUpMode) WepeEmerald else WepeHairline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            isSignUpMode = true
                            onClearAuthMessage()
                        }
                        .testTag("auth_mode_signup")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = s.authSignUpTab,
                            color = if (isSignUpMode) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Role & Profile Fields (only when creating a new account or finishing an incomplete Supabase session)
        if (isSignUpMode || !currentSupabaseUid.isNullOrBlank()) {
            Text(
                text = "1. ${s.authChooseRole}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            // IMPORTANT: Strictly Client and Craftsman only — Admin is never shown here
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoleSelectionCard(
                    title = s.roleClient,
                    subtitle = s.clientHeroBadge,
                    icon = Icons.Rounded.Person,
                    selected = selectedRole == UserRole.CLIENT,
                    onClick = { selectedRole = UserRole.CLIENT },
                    tag = "register_role_client",
                    modifier = Modifier.weight(1f)
                )
                RoleSelectionCard(
                    title = s.roleCraftsman,
                    subtitle = s.authChooseSpecialty,
                    icon = Icons.Rounded.Build,
                    selected = selectedRole == UserRole.CRAFTSMAN,
                    onClick = { selectedRole = UserRole.CRAFTSMAN },
                    tag = "register_role_craftsman",
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = selectedRole == UserRole.CRAFTSMAN) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.authChooseSpecialty,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = VerifiedEmerald
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(HirafiCategories.all, key = { it.id }) { category ->
                            val isPicked = category.id == selectedSpecialtyId
                            Surface(
                                color = if (isPicked) WepeEmerald else Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (isPicked) 2.dp else 1.dp,
                                        color = if (isPicked) WepeEmerald else WepeHairline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSpecialtyId = category.id }
                                    .testTag("register_specialty_${category.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isPicked) Color.White else category.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = category.localizedName(s),
                                        color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. الاسم الظاهر والمدينة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { if (it.length <= 80) fullName = it },
                        label = {
                            Text(
                                if (selectedRole == UserRole.CRAFTSMAN)
                                    "الاسم الظاهر للزبائن"
                                else
                                    "الاسم الكامل الظاهر للحرفيين"
                            )
                        },
                        placeholder = {
                            Text(
                                if (selectedRole == UserRole.CRAFTSMAN)
                                    "مثال: أحمد السباك"
                                else
                                    "مثال: يونس العلمي"
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        trailingIcon = if (isNameValid) {
                            { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald) }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_full_name_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                            Text("المدينة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MoroccanLocations.cities) { cityName ->
                                FilterChip(
                                    selected = selectedCity == cityName,
                                    onClick = { selectedCity = cityName },
                                    label = { Text(cityName, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WepeEmerald,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Complete existing Supabase session OR show Email/Password + Google auth
        if (!currentSupabaseUid.isNullOrBlank()) {
            Button(
                onClick = {
                    onGoogleAuthSuccess(
                        GoogleAuthAccount(
                            idToken = "",
                            displayName = currentSupabaseDisplayName.orEmpty(),
                            email = currentSupabaseEmail.orEmpty()
                        ),
                        fullName.trim(),
                        selectedCity,
                        selectedRole,
                        chosenSpecialty,
                        true
                    )
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WepeEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("جاري حفظ الحساب في Supabase...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("تأكيد التسجيل كـ $roleLabel في Supabase", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            if (onSwitchAccount != null) {
                OutlinedButton(
                    onClick = onSwitchAccount,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_google_account_button")
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("استخدام حساب آخر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            // Google Sign-In Button on TOP
            OutlinedButton(
                onClick = {
                    isGoogleLoading = true
                    GoogleAuthHelper.launchGoogleSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = { account ->
                            isGoogleLoading = false
                            onGoogleAuthSuccess(
                                account,
                                fullName.trim(),
                                selectedCity,
                                selectedRole,
                                chosenSpecialty,
                                isSignUpMode
                            )
                        },
                        onAuthError = { msg ->
                            isGoogleLoading = false
                            onShowError(msg)
                        },
                        onAuthCancelled = {
                            isGoogleLoading = false
                        },
                        onLaunchedBrowserOAuth = {
                            isGoogleLoading = false
                            onShowError("جاري فتح تسجيل الدخول عبر متصفح غوغل...")
                        }
                    )
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.5.dp, WepeHairline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = ObsidianBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = WepeEmerald, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("جاري الاتصال بحساب غوغل...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    GoogleLogoBadge(modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isSignUpMode) {
                            when (s.language) {
                                AppLanguage.ARABIC -> "المتابعة كـ $roleLabel عبر غوغل"
                                AppLanguage.FRENCH -> "Continuer en tant que $roleLabel avec Google"
                                AppLanguage.ENGLISH -> "Continue as $roleLabel with Google"
                            }
                        } else {
                            s.authGoogleButton
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            if (SupabaseConfig.isConfigured) {
                TextButton(
                    onClick = {
                        if (GoogleAuthHelper.launchBrowserGoogleOAuth(context)) {
                            onShowError("جاري فتح تسجيل الدخول عبر متصفح غوغل...")
                        } else {
                            onShowError("تعذر فتح المتصفح لتسجيل الدخول عبر غوغل")
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_browser_oauth_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = WepeEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "أو المتابعة عبر متصفح غوغل",
                        color = WepeEmeraldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("أو", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Email + Password Form Card on BOTTOM
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isSignUpMode) "3. بيانات الدخول بالبريد الإلكتروني" else "تسجيل الدخول لحسابك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            if (it.length <= 120) {
                                email = it
                                onClearAuthMessage()
                            }
                        },
                        label = { Text(s.authEmailLabel) },
                        placeholder = { Text("example@gmail.com") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        trailingIcon = if (isEmailValid) {
                            { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = email.isNotBlank() && !isEmailValid,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            if (it.length <= 72) {
                                password = it
                                onClearAuthMessage()
                            }
                        },
                        label = { Text(s.authPasswordLabel) },
                        placeholder = { Text("••••••") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = isSignUpMode && password.isNotEmpty() && !isPasswordValid,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    if (isSignUpMode && password.isNotEmpty()) {
                        val (strengthLabel, strengthColor, strengthProgress) = when {
                            passwordScore >= 3 -> Triple(s.authPasswordStrong, VerifiedEmerald, 1f)
                            passwordScore == 2 -> Triple(s.authPasswordMedium, StarAmber, 0.62f)
                            else -> Triple(s.authPasswordWeak, UrgentCoral, 0.30f)
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_strength_bar")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${s.authPasswordStrengthLabel}: $strengthLabel",
                                    color = strengthColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!isPasswordValid) s.authPasswordMinError else "",
                                    color = UrgentCoral,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { strengthProgress },
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                            )
                            if (passwordScore < 3) {
                                Text(
                                    text = s.authPasswordHint,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    if (!isSignUpMode) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = email.trim()
                                    showForgotPasswordDialog = true
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = s.authForgotPassword,
                                    color = VerifiedEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                onEmailSignUp(
                                    email.trim(),
                                    password,
                                    fullName.trim(),
                                    selectedCity,
                                    selectedRole,
                                    chosenSpecialty
                                )
                            } else {
                                onEmailSignIn(email.trim(), password)
                            }
                        },
                        enabled = !isBusy && isEmailValid && (if (isSignUpMode) isPasswordValid else password.isNotBlank()),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WepeEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("email_auth_submit_button")
                    ) {
                        if (isCloudSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(s.loading, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text(
                                text = if (isSignUpMode) "${s.authSignUpCta} ($roleLabel)" else s.authSignInCta,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (!isSignUpMode) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    isSignUpMode = true
                                    onClearAuthMessage()
                                },
                                modifier = Modifier.testTag("switch_to_signup_link")
                            ) {
                                Text(
                                    text = when (s.language) {
                                        AppLanguage.ARABIC -> "ليس لديك حساب؟ أنشئ حساباً جديداً"
                                        AppLanguage.FRENCH -> "Pas encore de compte ? Créer un compte"
                                        AppLanguage.ENGLISH -> "Don't have an account? Create one"
                                    },
                                    color = WepeEmeraldDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = VerifiedEmerald) },
            title = { Text(s.authForgotPasswordTitle, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.authForgotPasswordBody, fontSize = 13.sp)
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text(s.authEmailLabel) },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onForgotPassword(resetEmailInput.trim())
                        showForgotPasswordDialog = false
                    },
                    enabled = LoginValidator.isValidEmail(resetEmailInput),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WepeEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_password_reset_button")
                ) {
                    Text(s.authSendResetLink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text(s.cancel)
                }
            }
        )
    }
}

@Composable
private fun GoogleLogoBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.20f, cap = StrokeCap.Butt)
        val inset = stroke.width / 2f
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -35f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 150f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.50f),
            end = Offset(size.width * 0.92f, size.height * 0.50f),
            strokeWidth = stroke.width
        )
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) WepeEmeraldSoft else Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) WepeEmerald else WepeHairline,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (selected) WepeEmerald else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (selected) ObsidianBlack else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (selected) WepeEmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) WepeEmerald else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
