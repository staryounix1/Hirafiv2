package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AccountRolePolicy
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.IdentityVerificationValidator
import com.example.data.model.UserSession
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
import com.example.ui.theme.WepeSoftCard
import com.example.ui.viewmodel.HirafiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftsmanVerificationScreen(
    viewModel: HirafiViewModel,
    onBack: () -> Unit
) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()

    if (!AccountRolePolicy.canAccessIdentityVerification(session)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("التحقق من الهوية", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = StarAmber, modifier = Modifier.size(36.dp))
                        Text("هذه الصفحة مخصصة لحسابات الحرفيين فقط", fontWeight = FontWeight.Bold)
                        Button(onClick = onBack) { Text("العودة") }
                    }
                }
            }
        }
        return
    }

    var cinInput by rememberSaveable(session.nationalIdNumber) {
        mutableStateOf(session.nationalIdNumber)
    }
    var frontUri by rememberSaveable(session.idFrontPhotoUri) {
        mutableStateOf(session.idFrontPhotoUri)
    }
    var backUri by rememberSaveable(session.idBackPhotoUri) {
        mutableStateOf(session.idBackPhotoUri)
    }
    var selfieUri by rememberSaveable(session.selfiePhotoUri) {
        mutableStateOf(session.selfiePhotoUri)
    }

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) frontUri = uri.toString()
    }
    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) backUri = uri.toString()
    }
    val selfiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) selfieUri = uri.toString()
    }

    val completedSteps = when (session.verificationStatus) {
        IdentityVerificationStatus.VERIFIED,
        IdentityVerificationStatus.PENDING_REVIEW -> 4
        IdentityVerificationStatus.NOT_SUBMITTED,
        IdentityVerificationStatus.REJECTED -> IdentityVerificationValidator.completionStepsCount(
            cin = cinInput,
            frontUri = frontUri,
            backUri = backUri,
            selfieUri = selfieUri
        )
    }
    val progress by animateFloatAsState(
        targetValue = completedSteps / 4f,
        animationSpec = tween(350),
        label = "kyc_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("التحقق من هوية الحرفي", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            text = HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("verification_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Surface(
                        color = when (session.verificationStatus) {
                            IdentityVerificationStatus.VERIFIED -> VerifiedEmeraldSoft
                            IdentityVerificationStatus.PENDING_REVIEW -> WepeEmeraldSoft
                            IdentityVerificationStatus.REJECTED -> UrgentCoralSoft
                            IdentityVerificationStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = when (session.verificationStatus) {
                                    IdentityVerificationStatus.VERIFIED -> Icons.Rounded.Verified
                                    IdentityVerificationStatus.PENDING_REVIEW -> Icons.Rounded.HourglassTop
                                    IdentityVerificationStatus.REJECTED,
                                    IdentityVerificationStatus.NOT_SUBMITTED -> Icons.Rounded.Shield
                                },
                                contentDescription = null,
                                tint = when (session.verificationStatus) {
                                    IdentityVerificationStatus.VERIFIED -> VerifiedEmerald
                                    IdentityVerificationStatus.PENDING_REVIEW -> WepeEmeraldDark
                                    IdentityVerificationStatus.REJECTED -> UrgentCoral
                                    IdentityVerificationStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = session.verificationStatus.labelAr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (session.verificationStatus) {
                                    IdentityVerificationStatus.VERIFIED -> VerifiedEmerald
                                    IdentityVerificationStatus.PENDING_REVIEW -> WepeEmeraldDark
                                    IdentityVerificationStatus.REJECTED -> UrgentCoral
                                    IdentityVerificationStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("craftsman_verification_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VerificationHeroCard(
                session = session,
                completedSteps = completedSteps,
                progress = progress
            )

            when (session.verificationStatus) {
                IdentityVerificationStatus.VERIFIED -> {
                    VerifiedDigitalBadgeSection(
                        session = session,
                        onDone = onBack,
                        onRequestUpdate = { viewModel.resetCraftsmanVerificationForUpdate() }
                    )
                }

                IdentityVerificationStatus.PENDING_REVIEW -> {
                    PendingVerificationTimelineCard()
                }

                IdentityVerificationStatus.NOT_SUBMITTED,
                IdentityVerificationStatus.REJECTED -> {
                    if (session.verificationStatus == IdentityVerificationStatus.REJECTED) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = UrgentCoralSoft,
                            border = BorderStroke(1.dp, UrgentCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "تم رفض ملف التوثيق السابق",
                                    color = UrgentCoral,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = session.rejectionReason.ifBlank {
                                        "يرجى التأكد من رقم البطاقة ووضوح الصور وإعادة الإرسال."
                                    },
                                    color = ObsidianBlack,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    VerificationStepCinCard(
                        cinValue = cinInput,
                        onCinChange = { raw ->
                            if (raw.length <= 12) cinInput = raw.uppercase()
                        }
                    )

                    VerificationDocumentSlotCard(
                        stepNumber = 2,
                        title = "الوجه الأمامي للبطاقة الوطنية",
                        subtitle = "تأكد أن الاسم والصورة والرقم واضحة بدون انعكاس ضوئي",
                        icon = Icons.Rounded.CreditCard,
                        selectedUri = frontUri,
                        isFrontGuide = true,
                        onPickClick = {
                            frontPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        testTag = "kyc_front_id_card"
                    )

                    VerificationDocumentSlotCard(
                        stepNumber = 3,
                        title = "الوجه الخلفي للبطاقة الوطنية",
                        subtitle = "صوّر الجهة الخلفية كاملة مع ظهور العنوان والرموز بوضوح",
                        icon = Icons.Rounded.Badge,
                        selectedUri = backUri,
                        isFrontGuide = false,
                        onPickClick = {
                            backPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        testTag = "kyc_back_id_card"
                    )

                    VerificationSelfieSlotCard(
                        selectedUri = selfieUri,
                        onPickClick = {
                            selfiePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "حماية الخصوصية: وثائقك مشفّرة وتُستخدم لتوثيق حساب الحرفي فقط ولا يتم عرضها للزبائن أو لحرفيين آخرين.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.submitCraftsmanVerification(
                                nationalIdNumber = cinInput,
                                frontPhotoUri = frontUri,
                                backPhotoUri = backUri,
                                selfiePhotoUri = selfieUri
                            )
                        },
                        enabled = completedSteps == 4,
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WepeEmerald,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_verification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (completedSteps == 4)
                                "إرسال ملف التوثيق الآن"
                            else
                                "أكمل خطوات التحقق ($completedSteps من 4)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VerificationHeroCard(
    session: UserSession,
    completedSteps: Int,
    progress: Float
) {
    val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
    val accent = when (session.verificationStatus) {
        IdentityVerificationStatus.VERIFIED -> VerifiedEmerald
        IdentityVerificationStatus.PENDING_REVIEW -> VoltGlowAccent
        IdentityVerificationStatus.REJECTED -> UrgentCoral
        IdentityVerificationStatus.NOT_SUBMITTED -> VoltGlowAccent
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * 0.14f, size.height * 0.22f),
                        radius = size.maxDimension * 0.55f
                    ),
                    center = Offset(size.width * 0.14f, size.height * 0.22f),
                    radius = size.maxDimension * 0.55f
                )
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF232830))
                                .border(2.dp, accent, CircleShape)
                        ) {
                            Text(
                                text = session.fullName.take(1),
                                color = accent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                        if (session.verificationStatus == IdentityVerificationStatus.VERIFIED) {
                            Surface(
                                color = VerifiedEmerald,
                                shape = CircleShape,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.fullName,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${category.nameAr} • ${session.city}",
                            color = Color(0xFFB5BFC9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = when (session.verificationStatus) {
                        IdentityVerificationStatus.VERIFIED ->
                            "هويتك موثّقة بالكامل! تظهر شارة الثقة بجانب عروضك وتزداد فرص قبول الزبائن لطلباتك."
                        IdentityVerificationStatus.PENDING_REVIEW ->
                            "تم إرسال ملفك إلى السحابة وهو الآن بانتظار مراجعة واعتماد إدارة التطبيق."
                        IdentityVerificationStatus.REJECTED ->
                            "تحتاج وثائقك إلى تحديث: ${session.rejectionReason.ifBlank { "أعد رفع صور أوضح." }}"
                        IdentityVerificationStatus.NOT_SUBMITTED ->
                            "وثّق هويتك للحصول على شارة الحرفي المعتمد وزيادة ثقة الزبائن."
                    },
                    color = Color(0xFFD6DDE6),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "جاهزية ملف التوثيق",
                            color = Color(0xFF9AA5B4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$completedSteps / 4 خطوات",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = accent,
                        trackColor = Color(0xFF2B3038),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationStepCinCard(
    cinValue: String,
    onCinChange: (String) -> Unit
) {
    val isValid = IdentityVerificationValidator.isValidCin(cinValue)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isValid) 1.5.dp else 1.dp,
            color = if (isValid) VerifiedEmerald else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StepHeaderRow(
                    stepNumber = 1,
                    title = "رقم البطاقة الوطنية (CIN)",
                    isDone = isValid
                )
                if (isValid) {
                    Surface(
                        color = VerifiedEmeraldSoft,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = "صيغة صحيحة ✓",
                            color = VerifiedEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cinValue,
                onValueChange = onCinChange,
                label = { Text("أدخل رقم البطاقة الوطنية") },
                placeholder = { Text("مثال: BK654321") },
                leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WepeEmerald,
                    unfocusedBorderColor = WepeHairline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kyc_cin_input")
            )
        }
    }
}

@Composable
private fun VerificationDocumentSlotCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedUri: String?,
    isFrontGuide: Boolean,
    onPickClick: () -> Unit,
    testTag: String
) {
    val isDone = !selectedUri.isNullOrBlank()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isDone) 1.5.dp else 1.dp,
            color = if (isDone) VerifiedEmerald else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPickClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StepHeaderRow(stepNumber = stepNumber, title = title, isDone = isDone)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = ObsidianBlack.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(14.dp))
                            Text("تغيير الصورة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = VerifiedEmerald,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("مرفقة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WepeSoftCard,
                    border = BorderStroke(1.dp, WepeHairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = WepeEmeraldSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, tint = WepeEmeraldDark, modifier = Modifier.size(24.dp))
                                }
                            }
                            Column {
                                Text(
                                    text = if (isFrontGuide) "ارفع صورة الواجهة الأمامية" else "ارفع صورة الواجهة الخلفية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "اضغط لاختيار الصورة من المعرض",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = WepeEmerald,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationSelfieSlotCard(
    selectedUri: String?,
    onPickClick: () -> Unit
) {
    val isDone = !selectedUri.isNullOrBlank()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isDone) 1.5.dp else 1.dp,
            color = if (isDone) VerifiedEmerald else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPickClick)
            .testTag("kyc_selfie_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isDone) VerifiedEmeraldSoft else WepeSoftCard)
                    .border(2.dp, if (isDone) VerifiedEmerald else WepeEmerald, CircleShape)
            ) {
                if (isDone) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "الصورة الشخصية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = null,
                        tint = WepeEmeraldDark,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StepHeaderRow(stepNumber = 4, title = "صورة سيلفي للمطابقة", isDone = isDone)
                Text(
                    text = if (isDone) "تم إرفاق السيلفي بنجاح — اضغط للتغيير" else "اختر صورة واضحة لوجهك في إضاءة جيدة لمطابقتها مع البطاقة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = WepeEmerald,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.AddAPhoto,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingVerificationTimelineCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "kyc_scan")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, WepeEmerald),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kyc_pending_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            CircularProgressIndicator(
                color = WepeEmerald,
                trackColor = WepeEmeraldSoft,
                modifier = Modifier.size(46.dp)
            )
            Text(
                text = "بانتظار مراجعة واعتماد الإدارة",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Text(
                text = "تم حفظ رقم بطاقتك وصورك في لوحة الإدارة، وستصلك إشعارات فورية فور اعتمادها.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulse)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TimelineCheckRow("استلام رقم البطاقة الوطنية", completed = true)
                TimelineCheckRow("رفع وحفظ الوثائق للمراجعة", completed = true)
                TimelineCheckRow("مراجعة واعتماد فريق الأدمين", completed = false)
            }
        }
    }
}

@Composable
private fun VerifiedDigitalBadgeSection(
    session: UserSession,
    onDone: () -> Unit,
    onRequestUpdate: () -> Unit
) {
    val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = VerifiedEmeraldSoft,
            border = BorderStroke(1.5.dp, VerifiedEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kyc_verified_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(30.dp)
                    )
                    Column {
                        Text(
                            text = "تم توثيق هويتك بنجاح!",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "رقم البطاقة: ${IdentityVerificationValidator.maskCin(session.nationalIdNumber)} • ${category.nameAr}",
                            color = ObsidianBlack.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !session.idFrontPhotoUri.isNullOrBlank() ||
                        !session.idBackPhotoUri.isNullOrBlank() ||
                        !session.selfiePhotoUri.isNullOrBlank()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VerifiedMiniDocPreview("الوجه الأمامي", session.idFrontPhotoUri, Modifier.weight(1f))
                        VerifiedMiniDocPreview("الوجه الخلفي", session.idBackPhotoUri, Modifier.weight(1f))
                        VerifiedMiniDocPreview("السيلفي", session.selfiePhotoUri, Modifier.weight(1f))
                    }
                }
            }
        }

        TrustPerkRow(
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            title = "أولوية في قبول العروض",
            subtitle = "الزبائن يثقون أسرع بالحرفي الموثق ويقبلون عروضه بنسبة أعلى."
        )
        TrustPerkRow(
            icon = Icons.Rounded.Star,
            title = "شارة التوثيق الرسمية",
            subtitle = "تظهر شارة الهوية الموثقة في ملفك وفي القائمة الجانبية."
        )
        TrustPerkRow(
            icon = Icons.Rounded.Shield,
            title = "حماية المعاملات والضمانات",
            subtitle = "حسابك مؤهّل لإصدار شهادات الضمان الرقمية للزبائن."
        )

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("العودة إلى لوحة الطلبات", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }

        OutlinedButton(
            onClick = onRequestUpdate,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("kyc_reupload_button")
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("تحديث صور الوثائق", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VerifiedMiniDocPreview(
    label: String,
    uri: String?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, VerifiedEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            if (!uri.isNullOrBlank()) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald)
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
    }
}

@Composable
private fun TrustPerkRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                color = WepeEmeraldSoft,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = WepeEmeraldDark, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepHeaderRow(
    stepNumber: Int,
    title: String,
    isDone: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = if (isDone) VerifiedEmerald else WepeEmeraldSoft,
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                } else {
                    Text(
                        text = "$stepNumber",
                        color = WepeEmeraldDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Text(
            text = title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TimelineCheckRow(text: String, completed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.HourglassTop,
            contentDescription = null,
            tint = if (completed) VerifiedEmerald else StarAmber,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (completed) FontWeight.Bold else FontWeight.Medium
        )
    }
}
