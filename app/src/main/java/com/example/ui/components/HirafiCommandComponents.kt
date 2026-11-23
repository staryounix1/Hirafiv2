package com.example.ui.components

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AcceptanceRadarMetrics
import com.example.data.model.FairPriceAssessment
import com.example.data.model.HirafiQuickScenarios
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.JobPriceProposal
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.SmartTriageScenario
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard
import java.util.Locale

@Composable
fun SmartTriageCarousel(
    selectedCategoryId: String,
    currentTitle: String,
    onSelectScenario: (SmartTriageScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenarios = remember(selectedCategoryId) {
        HirafiQuickScenarios.forCategory(selectedCategoryId)
    }
    if (scenarios.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_triage_carousel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = WepeEmerald,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "تشخيص سريع بنقرة واحدة",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(scenarios, key = { it.id }) { item ->
                val isSelected = currentTitle.trim() == item.titleAr
                Surface(
                    color = if (isSelected) WepeEmeraldSoft else Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) WepeEmerald else WepeHairline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectScenario(item) }
                        .testTag("triage_preset_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (item.isUrgentSuggested) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (isSelected) WepeEmeraldDark else UrgentCoral,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = item.titleAr,
                                color = if (isSelected) WepeEmeraldDark else ObsidianBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${item.recommendedPriceDh} د.م • ~${item.estimatedMinutes} د",
                            color = if (isSelected) WepeEmeraldDark.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptanceRadarGauge(
    priceDh: Int,
    recommendedMin: Int,
    recommendedMax: Int,
    isUrgent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val proposal = remember(priceDh, recommendedMin, recommendedMax, isUrgent) {
        JobPriceProposal(
            categoryId = "gauge",
            proposedPriceDh = priceDh,
            recommendedMinDh = recommendedMin,
            recommendedMaxDh = recommendedMax,
            isUrgent = isUrgent
        )
    }
    val metrics = remember(proposal) { AcceptanceRadarMetrics.compute(proposal) }
    val animatedFill by animateFloatAsState(
        targetValue = (metrics.probabilityPercent / 100f).coerceIn(0.15f, 1f),
        animationSpec = tween(350),
        label = "acceptance_fill"
    )
    val accentColor = when (metrics.fairness) {
        FairPriceAssessment.LOW -> UrgentCoral
        FairPriceAssessment.FAIR -> WepeEmerald
        FairPriceAssessment.GENEROUS -> WepeEmeraldDark
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("acceptance_radar_gauge"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = metrics.speedLabelAr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Text(
                text = "فرصة القبول ${metrics.probabilityPercent}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WepeHairline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFill)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                WepeEmerald,
                                if (metrics.fairness == FairPriceAssessment.LOW) UrgentCoral else VoltGlowAccent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun LiveDispatchSonarOverlay(
    createdAtMillis: Long,
    nowMillis: Long,
    categoryName: String,
    radiusKm: Int,
    offersCount: Int,
    modifier: Modifier = Modifier
) {
    val elapsedSec = ((nowMillis - createdAtMillis) / 1000L).coerceAtLeast(0L)
    val mins = elapsedSec / 60L
    val secs = elapsedSec % 60L
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val radiusLabel = if (radiusKm >= 20) "كامل المدينة" else "$radiusKm كم"

    val infiniteTransition = rememberInfiniteTransition(label = "sonar_pulse")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_r1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_r2"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            listOf(ring1, ring2).forEach { progress ->
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.45f
                val radius = maxRadius * progress
                drawCircle(
                    color = WepeEmerald.copy(alpha = alpha * 0.25f),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = WepeEmerald.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = WepeEmerald,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        Surface(
            color = Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(99.dp),
            shadowElevation = 10.dp,
            border = BorderStroke(1.2.dp, WepeEmerald.copy(alpha = 0.45f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 132.dp, start = 16.dp, end = 16.dp)
                .testTag("live_dispatch_sonar_hud")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                LivePulseDot(color = WepeEmerald, dotSize = 7.dp)
                Text(
                    text = "رادار $categoryName • $radiusLabel",
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = WepeEmeraldSoft,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = WepeEmeraldDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeText,
                            color = WepeEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (offersCount > 0) {
                    Surface(
                        color = WepeEmerald,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = "$offersCount عروض",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobHandshakePinBanner(
    request: ServiceRequest,
    modifier: Modifier = Modifier
) {
    val pin = remember(request.id, request.categoryId) {
        JobHandshakeSecurity.deriveSecurityPin(request)
    }
    val warrantyCode = remember(request.id) {
        JobHandshakeSecurity.deriveWarrantyCode(request)
    }

    if (request.status == RequestStatus.IN_PROGRESS) {
        Surface(
            color = WepeEmeraldSoft,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, WepeEmerald.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .testTag("active_warranty_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(19.dp))
                    Column {
                        Text(
                            text = "تم التحقق وبدء التنفيذ بأمان",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ضمان حرفي 30 يوماً ساري • $warrantyCode",
                            color = WepeEmeraldDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(18.dp))
            }
        }
        return
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.2.dp, WepeEmerald.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("client_handshake_pin_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(WepeEmeraldSoft)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = WepeEmeraldDark,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text = "رمز التحقق عند الوصول",
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "أعطِ هذا الرمز للحرفي ليبدأ العمل",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pin.forEach { digit ->
                    Surface(
                        color = WepeSoftCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, WepeEmerald.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = digit.toString(),
                            color = WepeEmeraldDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CraftsmanHandshakePinVerifier(
    expectedPin: String,
    isVerified: Boolean,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember(expectedPin) { mutableStateOf("") }
    var showMismatchError by remember(expectedPin) { mutableStateOf(false) }

    if (isVerified) {
        Surface(
            color = WepeEmeraldSoft,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, WepeEmerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .testTag("craftsman_pin_verified_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "تم تأكيد رمز الأمان — يمكنك البدء الآن",
                    color = WepeEmeraldDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    Surface(
        color = Color(0xFF20242A),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WepeEmerald.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .testTag("craftsman_pin_verifier")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "أدخل رمز التحقق المكوّن من 4 أرقام من الزبون",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { raw ->
                        val digits = JobHandshakeSecurity.normalizePinInput(raw)
                        enteredPin = digits
                        showMismatchError = false
                        if (digits == expectedPin) {
                            onVerified()
                        } else if (digits.length == 4) {
                            showMismatchError = true
                        }
                    },
                    placeholder = { Text("4 أرقام", color = Color(0xFF98A2B3), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VoltGlowAccent,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WepeEmerald,
                        unfocusedBorderColor = Color(0xFF3E444F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("craftsman_pin_input")
                )

                Button(
                    onClick = {
                        val normalized = JobHandshakeSecurity.normalizePinInput(enteredPin)
                        enteredPin = normalized
                        if (normalized.length == 4 && normalized == expectedPin) {
                            showMismatchError = false
                            onVerified()
                        } else {
                            showMismatchError = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("confirm_craftsman_pin_button")
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            }

            if (showMismatchError) {
                Text(
                    text = "الرمز غير مطابق، تأكد من الزبون",
                    color = UrgentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun WarrantyCertificateDialog(
    request: ServiceRequest,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val warrantyCode = remember(request.id) { JobHandshakeSecurity.deriveWarrantyCode(request) }
    val completedAt = remember(request.updatedAtMillis) { JobHandshakeSecurity.formatCompletionDate(request.updatedAtMillis) }
    val finalPrice = request.acceptedPrice ?: request.proposedPrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Service Complete + Close
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WepeEmeraldSoft)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "اكتملت الخدمة ($completedAt)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ObsidianBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "شهادة ضمان حرفي • 30 يوماً",
                                fontSize = 11.sp,
                                color = WepeEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "إغلاق", tint = ObsidianBlack)
                    }
                }

                // Embedded Styled Route Map Preview Banner (Screen 4 Look)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFEEF3F7))
                        .border(1.dp, WepeHairline, RoundedCornerShape(18.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = Color(0xFFDEE6EE)
                        drawLine(gridColor, Offset(0f, size.height * 0.28f), Offset(size.width, size.height * 0.42f), strokeWidth = 4.dp.toPx())
                        drawLine(gridColor, Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.62f), strokeWidth = 5.dp.toPx())
                        drawLine(gridColor, Offset(size.width * 0.3f, 0f), Offset(size.width * 0.22f, size.height), strokeWidth = 4.dp.toPx())
                        drawLine(gridColor, Offset(size.width * 0.7f, 0f), Offset(size.width * 0.78f, size.height), strokeWidth = 4.dp.toPx())

                        val start = Offset(size.width * 0.2f, size.height * 0.72f)
                        val mid = Offset(size.width * 0.52f, size.height * 0.44f)
                        val end = Offset(size.width * 0.78f, size.height * 0.26f)
                        val routePath = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                size.width * 0.32f, size.height * 0.72f,
                                size.width * 0.42f, size.height * 0.42f,
                                mid.x, mid.y
                            )
                            cubicTo(
                                size.width * 0.64f, size.height * 0.46f,
                                size.width * 0.68f, size.height * 0.26f,
                                end.x, end.y
                            )
                        }
                        drawPath(
                            path = routePath,
                            color = Color(0xFF0B6E3A),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = WepeEmerald,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawCircle(color = WepeEmeraldSoft, radius = 10.dp.toPx(), center = start)
                        drawCircle(color = ObsidianBlack, radius = 5.dp.toPx(), center = start)
                        drawCircle(color = WepeEmeraldSoft, radius = 10.dp.toPx(), center = end)
                        drawCircle(color = WepeEmerald, radius = 6.dp.toPx(), center = end)
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = warrantyCode,
                            color = WepeEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Vertical 2-dot Route / Service Timeline
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(WepeEmerald)
                        )
                        Column {
                            Text("موقع الخدمة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(request.addressLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ObsidianBlack)
                        )
                        Column {
                            Text("الخدمة المنفذة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${request.title} (${request.categoryName})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                        }
                    }
                }

                HorizontalDivider(color = WepeHairline)

                // Craftsman Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WepeEmeraldSoft)
                                .border(1.5.dp, WepeEmerald, CircleShape)
                        ) {
                            Text(
                                text = (request.assignedCraftsmanName ?: "ح").take(1),
                                color = WepeEmeraldDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = request.assignedCraftsmanName ?: "حرفي معتمد",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ObsidianBlack
                                )
                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(15.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = StarAmber, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${request.assignedCraftsmanRating ?: 4.9} • ${request.categoryName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Surface(color = WepeSoftCard, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "30 يوم ضمان",
                            color = WepeEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = WepeHairline)

                // Payment Breakdown (Screen 4 Look)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تفاصيل الدفع والضمان", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ObsidianBlack)
                    WarrantyRow("أجرة الخدمة المتفق عليها", "$finalPrice درهم")
                    WarrantyRow("حماية ضمان حرفي (30 يوماً)", "مشمولة (0 درهم)")
                    WarrantyRow("رمز الضمان المعتمد", warrantyCode)
                    HorizontalDivider(color = WepeHairline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجمالي المدفوع", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ObsidianBlack)
                        Text("$finalPrice درهم", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = WepeEmeraldDark)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.7f).height(48.dp)
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("share_warranty_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("مشاركة الشهادة", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WarrantyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF667085))
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ObsidianBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

