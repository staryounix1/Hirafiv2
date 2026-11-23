package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.remote.MapRoutingService
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimePressed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianGradientBottom
import com.example.ui.theme.ObsidianGradientTop
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val ObsidianHeroBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(ObsidianGradientTop, ObsidianBlack, ObsidianGradientBottom)
    )

val VoltLimeBrush: Brush
    get() = Brush.horizontalGradient(
        colors = listOf(ElectricLime, ElectricLimePressed)
    )

enum class CraftsmanRequestFilter(val labelAr: String) {
    ALL("الكل"),
    URGENT("عاجل ⚡"),
    HIGHEST_PRICE("أعلى سعراً"),
    NEAREST("الأقرب")
}

object HirafiProAnalytics {

    fun filterAndSortOpenRequests(
        requests: List<ServiceRequest>,
        filter: CraftsmanRequestFilter,
        refLat: Double,
        refLng: Double
    ): List<ServiceRequest> {
        val base = when (filter) {
            CraftsmanRequestFilter.URGENT -> requests.filter { it.isUrgent }
            else -> requests
        }
        return when (filter) {
            CraftsmanRequestFilter.HIGHEST_PRICE -> base.sortedByDescending { it.proposedPrice }
            CraftsmanRequestFilter.NEAREST -> base.sortedBy {
                MapRoutingService.haversineKm(refLat, refLng, it.lat, it.lng)
            }
            CraftsmanRequestFilter.URGENT -> base.sortedByDescending { it.createdAtMillis }
            CraftsmanRequestFilter.ALL -> base.sortedWith(
                compareByDescending<ServiceRequest> { it.isUrgent }
                    .thenByDescending { it.createdAtMillis }
            )
        }
    }

    fun marketPriceDeltaPercent(
        proposedPrice: Int,
        recommendedMin: Int,
        recommendedMax: Int
    ): Int {
        val midpoint = ((recommendedMin + recommendedMax) / 2.0).coerceAtLeast(1.0)
        return (((proposedPrice - midpoint) / midpoint) * 100.0).roundToInt()
    }

    fun formatRelativeArabicAge(createdAtMillis: Long, nowMillis: Long): String =
        formatRelativeAge(createdAtMillis, nowMillis, AppLanguage.ARABIC)

    fun formatRelativeAge(
        createdAtMillis: Long,
        nowMillis: Long,
        lang: AppLanguage = AppLanguage.ARABIC
    ): String {
        val diffMinutes = ((nowMillis - createdAtMillis) / 60_000L).coerceAtLeast(0L)
        return when (lang) {
            AppLanguage.ARABIC -> when {
                diffMinutes < 1L -> "الآن"
                diffMinutes < 60L -> "منذ $diffMinutes د"
                diffMinutes < 1440L -> "منذ ${diffMinutes / 60L} س"
                else -> "منذ ${diffMinutes / 1440L} ي"
            }
            AppLanguage.FRENCH -> when {
                diffMinutes < 1L -> "À l'instant"
                diffMinutes < 60L -> "Il y a ${diffMinutes} min"
                diffMinutes < 1440L -> "Il y a ${diffMinutes / 60L} h"
                else -> "Il y a ${diffMinutes / 1440L} j"
            }
            AppLanguage.ENGLISH -> when {
                diffMinutes < 1L -> "Just now"
                diffMinutes < 60L -> "${diffMinutes}m ago"
                diffMinutes < 1440L -> "${diffMinutes / 60L}h ago"
                else -> "${diffMinutes / 1440L}d ago"
            }
        }
    }

    fun formatClockTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(Date(millis))
    }

    fun roleAwareQuickReplies(
        role: UserRole,
        lang: AppLanguage = AppLanguage.ARABIC
    ): List<String> = when (lang) {
        AppLanguage.ARABIC -> when (role) {
            UserRole.CRAFTSMAN -> listOf(
                "أنا في الطريق إليك",
                "سأصل خلال 10 دقائق",
                "وصلت أمام الموقع",
                "كم رقم الشقة أو الطابق؟"
            )
            else -> listOf(
                "أنا في الانتظار",
                "كم باقي لك؟",
                "الموقع دقيق على الخريطة",
                "تم التفاهم على السعر"
            )
        }
        AppLanguage.FRENCH -> when (role) {
            UserRole.CRAFTSMAN -> listOf(
                "Je suis en route",
                "J'arrive dans 10 min",
                "Je suis devant l'adresse",
                "Quel est le numéro / étage ?"
            )
            else -> listOf(
                "Je vous attends",
                "Dans combien de minutes ?",
                "La position est exacte sur la carte",
                "Prix confirmé, merci"
            )
        }
        AppLanguage.ENGLISH -> when (role) {
            UserRole.CRAFTSMAN -> listOf(
                "I'm on my way",
                "Arriving in 10 minutes",
                "I'm outside your location",
                "What's the apartment / floor?"
            )
            else -> listOf(
                "I'm waiting for you",
                "How far away are you?",
                "The map pin is exact",
                "Price agreed, thank you"
            )
        }
    }
}

@Composable
fun LivePulseDot(
    color: Color = VerifiedEmerald,
    dotSize: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse_dot")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(dotSize * 2.2f)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(color.copy(alpha = pulseAlpha))
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun JobMilestoneTimeline(
    currentStatus: RequestStatus,
    isDarkSurface: Boolean = false,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val steps = when (s.language) {
        AppLanguage.ARABIC -> listOf(
            Triple(RequestStatus.ACCEPTED, "مقبول", Icons.Rounded.CheckCircle),
            Triple(RequestStatus.EN_ROUTE, "في الطريق", Icons.Rounded.DirectionsCar),
            Triple(RequestStatus.ARRIVED, "وصل", Icons.Rounded.LocationOn),
            Triple(RequestStatus.IN_PROGRESS, "جاري", Icons.Rounded.Build),
            Triple(RequestStatus.COMPLETED, "مكتمل", Icons.Rounded.Verified)
        )
        AppLanguage.FRENCH -> listOf(
            Triple(RequestStatus.ACCEPTED, "Accepté", Icons.Rounded.CheckCircle),
            Triple(RequestStatus.EN_ROUTE, "En route", Icons.Rounded.DirectionsCar),
            Triple(RequestStatus.ARRIVED, "Arrivé", Icons.Rounded.LocationOn),
            Triple(RequestStatus.IN_PROGRESS, "En cours", Icons.Rounded.Build),
            Triple(RequestStatus.COMPLETED, "Terminé", Icons.Rounded.Verified)
        )
        AppLanguage.ENGLISH -> listOf(
            Triple(RequestStatus.ACCEPTED, "Accepted", Icons.Rounded.CheckCircle),
            Triple(RequestStatus.EN_ROUTE, "En Route", Icons.Rounded.DirectionsCar),
            Triple(RequestStatus.ARRIVED, "Arrived", Icons.Rounded.LocationOn),
            Triple(RequestStatus.IN_PROGRESS, "Working", Icons.Rounded.Build),
            Triple(RequestStatus.COMPLETED, "Done", Icons.Rounded.Verified)
        )
    }
    val activeIdx = steps.indexOfFirst { it.first == currentStatus }.coerceAtLeast(0)
    val inactiveTrack = if (isDarkSurface) SlateBorder else WepeHairline
    val inactiveText = if (isDarkSurface) Color(0xFF8A94A3) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkSurface) Color(0xFF1F2329) else WepeSoftCard)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("job_milestone_timeline")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (_, label, icon) ->
                val isDone = index < activeIdx
                val isActive = index == activeIdx
                val nodeColor = when {
                    isDone -> WepeEmerald
                    isActive -> WepeEmerald
                    else -> inactiveTrack
                }
                val iconTint = when {
                    isDone -> Color.White
                    isActive -> Color.White
                    else -> inactiveText
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(nodeColor)
                            .then(
                                if (isActive) {
                                    Modifier.border(2.dp, WepeEmeraldSoft, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Rounded.Check else icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = when {
                            isActive && isDarkSurface -> VoltGlowAccent
                            isActive -> WepeEmerald
                            isDone -> WepeEmerald
                            else -> inactiveText
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .height(3.dp)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index < activeIdx) WepeEmerald else inactiveTrack)
                    )
                }
            }
        }
    }
}
