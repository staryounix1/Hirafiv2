package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.LiveRouteState
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard

@Composable
fun PriceProposalBar(
    priceDh: Int,
    recommendedMin: Int,
    recommendedMax: Int,
    onAdjust: (Int) -> Unit,
    onSetExact: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WepeSoftCard)
            .border(1.dp, WepeHairline, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(WepeEmerald)
                )
                Text(
                    text = s.clientYourProposedPrice,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            }
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.border(1.dp, WepeHairline, RoundedCornerShape(99.dp))
            ) {
                val recPrefix = when (s.language) {
                    AppLanguage.ARABIC -> "الموصى به"
                    AppLanguage.FRENCH -> "Conseillé"
                    AppLanguage.ENGLISH -> "Suggested"
                }
                Text(
                    text = "$recPrefix: $recommendedMin–$recommendedMax ${s.currencyDh}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, WepeHairline, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAdjust(-10) }
                    .testTag("price_minus_10")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "-10", tint = ObsidianBlack, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("10", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ObsidianBlack)
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.6.dp, WepeEmerald, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showCustomDialog = true }
                    .testTag("price_display_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "$priceDh ${s.currencyDh}",
                        color = WepeEmeraldDark,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = s.clientYourProposedPrice,
                        tint = WepeEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, WepeHairline, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAdjust(10) }
                    .testTag("price_plus_10")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "زيد 10", tint = ObsidianBlack, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("10", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ObsidianBlack)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val sliderMin = maxOf(40, recommendedMin - 40).toFloat()
        val sliderMax = (recommendedMax * 2).coerceAtLeast(320).toFloat()
        val sliderValue = priceDh.toFloat().coerceIn(sliderMin, sliderMax)
        Slider(
            value = sliderValue,
            onValueChange = { raw ->
                val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(40, 5000)
                if (snapped != priceDh) onSetExact(snapped)
            },
            valueRange = sliderMin..sliderMax,
            colors = SliderDefaults.colors(
                thumbColor = WepeEmerald,
                activeTrackColor = WepeEmerald,
                inactiveTrackColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("price_proposal_slider")
        )

        val marketMid = (((recommendedMin + recommendedMax) / 2.0) / 5.0).roundToInt() * 5
        val urgentHigh = (((recommendedMax * 1.2) / 5.0).roundToInt() * 5).coerceAtLeast(marketMid + 20)
        val presets = when (s.language) {
            AppLanguage.ARABIC -> listOf(
                "اقتصادي" to recommendedMin,
                "سعر السوق" to marketMid,
                "عاجل ⚡" to urgentHigh
            )
            AppLanguage.FRENCH -> listOf(
                "Éco" to recommendedMin,
                "Prix marché" to marketMid,
                "Urgent ⚡" to urgentHigh
            )
            AppLanguage.ENGLISH -> listOf(
                "Budget" to recommendedMin,
                "Market" to marketMid,
                "Urgent ⚡" to urgentHigh
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (presetLabel, presetPrice) ->
                val isSelected = kotlin.math.abs(priceDh - presetPrice) <= 4
                Surface(
                    color = if (isSelected) WepeEmeraldSoft else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) WepeEmerald else WepeHairline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSetExact(presetPrice) }
                        .testTag("price_preset_$presetPrice")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = presetLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) WepeEmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "$presetPrice ${s.currencyDh}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) WepeEmeraldDark else ObsidianBlack,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        AcceptanceRadarGauge(
            priceDh = priceDh,
            recommendedMin = recommendedMin,
            recommendedMax = recommendedMax
        )
    }

    if (showCustomDialog) {
        var input by remember { mutableStateOf(priceDh.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = Color.White,
            title = { Text(s.clientYourProposedPrice, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) input = it },
                    label = { Text("${s.clientYourProposedPrice} (${s.currencyDh})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        input.toIntOrNull()?.let(onSetExact)
                        showCustomDialog = false
                    },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White)
                ) {
                    Text(s.confirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
fun IncomingOfferCard(
    offer: ServiceOffer,
    clientProposedPrice: Int,
    nowMillis: Long,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounterOffer: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightBadge: String? = null
) {
    val s = hirafiStrings()
    var showCounterInput by remember { mutableStateOf(false) }
    var counterPrice by remember(offer.offeredPrice) {
        mutableIntStateOf(((offer.offeredPrice + clientProposedPrice) / 2).coerceAtLeast(50))
    }

    val totalWindow = (offer.expiresAtMillis - offer.createdAtMillis).coerceAtLeast(1L)
    val remaining = (offer.expiresAtMillis - nowMillis).coerceAtLeast(0L)
    val remainingSeconds = (remaining / 1000L).coerceAtLeast(0L)
    val progress = (remaining.toFloat() / totalWindow.toFloat()).coerceIn(0f, 1f)

    val priceDiff = offer.offeredPrice - clientProposedPrice
    val diffBadgeText = when {
        priceDiff == 0 -> when (s.language) {
            AppLanguage.ARABIC -> "بنفس سعرك"
            AppLanguage.FRENCH -> "Votre prix"
            AppLanguage.ENGLISH -> "Exact match"
        }
        priceDiff < 0 -> when (s.language) {
            AppLanguage.ARABIC -> "أقل بـ ${-priceDiff} ${s.currencyDh}"
            AppLanguage.FRENCH -> "-${-priceDiff} ${s.currencyDh}"
            AppLanguage.ENGLISH -> "-${-priceDiff} ${s.currencyDh}"
        }
        else -> "+$priceDiff ${s.currencyDh}"
    }
    val diffBadgeBg = if (priceDiff <= 0) WepeEmeraldSoft else StarAmber.copy(alpha = 0.14f)
    val diffBadgeFg = if (priceDiff <= 0) WepeEmeraldDark else Color(0xFFB36700)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (highlightBadge != null) 1.8.dp else 1.dp,
                color = if (highlightBadge != null) WepeEmerald else WepeHairline,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = WepeEmerald,
                    trackColor = WepeSoftCard,
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                if (highlightBadge != null) {
                    Surface(
                        color = WepeEmeraldSoft,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = highlightBadge,
                            color = WepeEmeraldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Surface(
                    color = if (remainingSeconds <= 10L) UrgentCoral.copy(alpha = 0.14f) else WepeSoftCard,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val secUnit = when (s.language) {
                        AppLanguage.ARABIC -> "ث"
                        else -> "s"
                    }
                    Text(
                        text = "$remainingSeconds $secUnit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSeconds <= 10L) UrgentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(48.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WepeEmeraldSoft)
                                .border(1.5.dp, WepeEmerald, CircleShape)
                        ) {
                            Text(
                                text = offer.craftsmanName.take(1),
                                color = WepeEmeraldDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = s.kycVerified,
                                tint = WepeEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = offer.craftsmanName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = StarAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " ${offer.craftsmanRating} (${offer.completedJobs}) • ${offer.etaMinutes} ${s.unitMin} • ${offer.distanceKm} ${s.unitKm}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${offer.offeredPrice} ${s.currencyDh}",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianBlack
                    )
                    Surface(
                        color = diffBadgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = diffBadgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffBadgeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (offer.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = WepeSoftCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = offer.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showCounterInput) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WepeSoftCard)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { counterPrice = (counterPrice - 10).coerceAtLeast(40) }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "-10")
                        }
                        Text(
                            text = "$counterPrice ${s.currencyDh}",
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianBlack,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { counterPrice += 10 }) {
                            Icon(Icons.Rounded.Add, contentDescription = "+10")
                        }
                        Button(
                            onClick = {
                                onCounterOffer(counterPrice)
                                showCounterInput = false
                            },
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "إرسال"
                                    AppLanguage.FRENCH -> "Envoyer"
                                    AppLanguage.ENGLISH -> "Send"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-20, -10, 10).forEach { delta ->
                            val label = if (delta > 0) "+$delta ${s.currencyDh}" else "$delta ${s.currencyDh}"
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, WepeHairline, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { counterPrice = (counterPrice + delta).coerceAtLeast(40) }
                                    .testTag("counter_preset_$delta")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val declineLabel = when (s.language) {
                AppLanguage.ARABIC -> "رفض"
                AppLanguage.FRENCH -> "Refuser"
                AppLanguage.ENGLISH -> "Decline"
            }
            val counterLabel = when (s.language) {
                AppLanguage.ARABIC -> "مفاوضة"
                AppLanguage.FRENCH -> "Négocier"
                AppLanguage.ENGLISH -> "Counter"
            }
            val acceptLabel = when (s.language) {
                AppLanguage.ARABIC -> "قبول"
                AppLanguage.FRENCH -> "Accepter"
                AppLanguage.ENGLISH -> "Accept"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    border = BorderStroke(1.dp, UrgentCoral.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .weight(0.75f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = UrgentCoral)
                    Spacer(Modifier.width(4.dp))
                    Text(declineLabel, color = UrgentCoral, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showCounterInput = !showCounterInput },
                    border = BorderStroke(1.dp, WepeHairline),
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = ObsidianBlack)
                    Spacer(Modifier.width(4.dp))
                    Text(counterLabel, color = ObsidianBlack, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("accept_offer_${offer.id}")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(acceptLabel, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun SheetDragHandleBar(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var cumulativeDrag by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onDragStart = { cumulativeDrag = 0f },
                    onDragEnd = { cumulativeDrag = 0f },
                    onDragCancel = { cumulativeDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeDrag += dragAmount
                        if (cumulativeDrag > 26f && isExpanded) {
                            onExpandChange(false)
                            cumulativeDrag = 0f
                        } else if (cumulativeDrag < -26f && !isExpanded) {
                            onExpandChange(true)
                            cumulativeDrag = 0f
                        }
                    }
                )
            }
            .testTag("sheet_drag_handle")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFD0D5DD))
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = if (isExpanded) "تصغير الشريط للأسفل" else "توسيع الشريط للأعلى",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ActiveJobSheet(
    request: ServiceRequest,
    liveRoute: LiveRouteState,
    unreadChatCount: Int = 0,
    onCallClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onSafetyClick: () -> Unit,
    onAdvanceStatus: (RequestStatus) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    var isExpanded by remember(request.id) { mutableStateOf(true) }
    val pinCode = remember(request.id, request.categoryId) {
        JobHandshakeSecurity.deriveSecurityPin(request)
    }

    val arrivingHeadline = when {
        (request.status == RequestStatus.ACCEPTED || request.status == RequestStatus.EN_ROUTE) && liveRoute.etaMinutes > 0 ->
            when (s.language) {
                AppLanguage.ARABIC -> "يصل خلال ${liveRoute.etaMinutes} دقائق"
                AppLanguage.FRENCH -> "Arrive dans ${liveRoute.etaMinutes} min"
                AppLanguage.ENGLISH -> "Arriving in ${liveRoute.etaMinutes} mins"
            }
        else -> request.status.localizedLabel(s)
    }

    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            SheetDragHandleBar(
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
                onExpandChange = { isExpanded = it }
            )

            // Screen 3 Bold Arrival Headline + Agreed Price Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = arrivingHeadline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianBlack,
                        maxLines = 1
                    )
                    if (request.status == RequestStatus.ACCEPTED || request.status == RequestStatus.EN_ROUTE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            LivePulseDot(color = WepeEmerald, dotSize = 6.dp)
                            Text(
                                text = when {
                                    liveRoute.craftsmanLat != null && liveRoute.remainingDistanceKm > 0.0 ->
                                        "${liveRoute.remainingDistanceKm} ${s.unitKm} • ${request.status.localizedLabel(s)}"
                                    liveRoute.craftsmanLat != null ->
                                        when (s.language) {
                                            AppLanguage.ARABIC -> "تتبع مباشر على الخريطة"
                                            AppLanguage.FRENCH -> "Suivi en direct sur la carte"
                                            AppLanguage.ENGLISH -> "Live GPS on map"
                                        }
                                    else -> request.status.localizedLabel(s)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WepeEmeraldDark
                            )
                        }
                    }
                }

                Surface(
                    color = WepeEmeraldSoft,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = "${request.acceptedPrice ?: request.proposedPrice} ${s.currencyDh}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WepeEmeraldDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            JobMilestoneTimeline(currentStatus = request.status)

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))

                    // Screen 3 Craftsman Profile Strip + Right Plate-Style PIN Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(50.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(WepeEmeraldSoft)
                                        .border(1.5.dp, WepeEmerald, CircleShape)
                                ) {
                                    Text(
                                        text = (request.assignedCraftsmanName ?: "H").take(1),
                                        color = WepeEmeraldDark,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = WepeEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = request.assignedCraftsmanName ?: s.roleCraftsman,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = request.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = WepeEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${request.assignedCraftsmanRating ?: 4.9}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianBlack
                                    )
                                }
                            }
                        }

                        Surface(
                            color = WepeSoftCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, WepeHairline)
                        ) {
                            Text(
                                text = "PIN-$pinCode",
                                color = ObsidianBlack,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Screen 3 Wide Chat/Note Pill + Circular Emerald Call Button + Safety Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadChatCount > 0) {
                                    Badge(containerColor = UrgentCoral, contentColor = Color.White) {
                                        Text(
                                            text = if (unreadChatCount > 9) "9+" else "$unreadChatCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = WepeSoftCard,
                                shape = RoundedCornerShape(99.dp),
                                border = BorderStroke(1.dp, WepeHairline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable(onClick = onChatClick)
                                    .testTag("open_chat_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = s.clientOpenChat,
                                        tint = ObsidianBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = when (s.language) {
                                            AppLanguage.ARABIC -> "أرسل رسالة أو ملاحظة للحرفي..."
                                            AppLanguage.FRENCH -> "Envoyer un message à l'artisan..."
                                            AppLanguage.ENGLISH -> "Any notes or chat..."
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onCallClick(request.assignedCraftsmanPhone ?: "+212 600-000000") },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(WepeEmerald)
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = s.roleCraftsman, tint = Color.White)
                        }

                        IconButton(
                            onClick = onSafetyClick,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(UrgentCoralSoft)
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = s.clientSafetyCenter, tint = UrgentCoral)
                        }
                    }

                    // Screen 3 "My Route / Service Details" Soft-Gray Card
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = WepeSoftCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, WepeHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "تفاصيل المسار والخدمة"
                                    AppLanguage.FRENCH -> "Mon itinéraire & service"
                                    AppLanguage.ENGLISH -> "My route & service"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ObsidianBlack
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(WepeEmerald)
                                )
                                Column {
                                    Text(
                                        text = when (s.language) {
                                            AppLanguage.ARABIC -> "موقع الخدمة"
                                            AppLanguage.FRENCH -> "Lieu d'intervention"
                                            AppLanguage.ENGLISH -> "Service location"
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = request.addressLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianBlack
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianBlack)
                                )
                                Column {
                                    Text(
                                        text = request.categoryName,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = request.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ObsidianBlack
                                    )
                                }
                            }
                        }
                    }

                    if (request.status == RequestStatus.ACCEPTED ||
                        request.status == RequestStatus.EN_ROUTE ||
                        request.status == RequestStatus.ARRIVED
                    ) {
                        Spacer(Modifier.height(10.dp))
                        val hasCoords = liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null
                        val coordsSummary = if (hasCoords) {
                            String.format(
                                java.util.Locale.US,
                                "%.4f, %.4f",
                                liveRoute.craftsmanLat,
                                liveRoute.craftsmanLng
                            )
                        } else {
                            ""
                        }
                        val liveInfoText = when (s.language) {
                            AppLanguage.ARABIC -> when {
                                request.status == RequestStatus.ARRIVED -> "وصل الحرفي إلى موقعك"
                                hasCoords && liveRoute.remainingDistanceKm > 0.0 -> "يبعد ${liveRoute.remainingDistanceKm} كم • يصل خلال ~${liveRoute.etaMinutes.coerceAtLeast(1)} د"
                                hasCoords -> "موقع الحرفي متصل مباشرة على الخريطة"
                                else -> "بانتظار إشارة GPS الحية من الحرفي..."
                            }
                            AppLanguage.FRENCH -> when {
                                request.status == RequestStatus.ARRIVED -> "L'artisan est arrivé sur place"
                                hasCoords && liveRoute.remainingDistanceKm > 0.0 -> "À ${liveRoute.remainingDistanceKm} km • ~${liveRoute.etaMinutes.coerceAtLeast(1)} min"
                                hasCoords -> "Position GPS de l'artisan connectée"
                                else -> "En attente du signal GPS en direct..."
                            }
                            AppLanguage.ENGLISH -> when {
                                request.status == RequestStatus.ARRIVED -> "Craftsman arrived at your location"
                                hasCoords && liveRoute.remainingDistanceKm > 0.0 -> "${liveRoute.remainingDistanceKm} km away • ~${liveRoute.etaMinutes.coerceAtLeast(1)} min"
                                hasCoords -> "Craftsman live GPS connected on map"
                                else -> "Waiting for live GPS signal..."
                            }
                        }
                        val liveBannerTitle = when (s.language) {
                            AppLanguage.ARABIC -> "تتبع الحرفي في البث المباشر"
                            AppLanguage.FRENCH -> "Suivi en direct de l'artisan"
                            AppLanguage.ENGLISH -> "Live Craftsman Tracking"
                        }
                        Surface(
                            color = WepeEmeraldSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WepeEmerald.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .testTag("client_live_craftsman_location_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (hasCoords) {
                                        LivePulseDot(color = WepeEmerald, dotSize = 7.dp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.GpsFixed,
                                            contentDescription = null,
                                            tint = WepeEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = liveBannerTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ObsidianBlack
                                        )
                                        Text(
                                            text = liveInfoText,
                                            fontSize = 11.sp,
                                            color = WepeEmeraldDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (coordsSummary.isNotEmpty()) {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = coordsSummary,
                                            color = WepeEmeraldDark,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (request.status == RequestStatus.ACCEPTED ||
                        request.status == RequestStatus.EN_ROUTE ||
                        request.status == RequestStatus.ARRIVED ||
                        request.status == RequestStatus.IN_PROGRESS
                    ) {
                        Spacer(Modifier.height(10.dp))
                        JobHandshakePinBanner(request = request)
                    }

                    Spacer(Modifier.height(14.dp))

                    var showCancelConfirmDialog by remember { mutableStateOf(false) }

                    val nextAction: Pair<String, RequestStatus>? = when (request.status) {
                        RequestStatus.ACCEPTED -> when (s.language) {
                            AppLanguage.ARABIC -> "تأكيد انطلاق الحرفي"
                            AppLanguage.FRENCH -> "Confirmer le départ"
                            AppLanguage.ENGLISH -> "Confirm En Route"
                        } to RequestStatus.EN_ROUTE
                        RequestStatus.EN_ROUTE -> when (s.language) {
                            AppLanguage.ARABIC -> "تأكيد وصول الحرفي"
                            AppLanguage.FRENCH -> "Confirmer l'arrivée"
                            AppLanguage.ENGLISH -> "Confirm Arrival"
                        } to RequestStatus.ARRIVED
                        RequestStatus.ARRIVED -> when (s.language) {
                            AppLanguage.ARABIC -> "بدء العمل الآن"
                            AppLanguage.FRENCH -> "Démarrer l'intervention"
                            AppLanguage.ENGLISH -> "Start Work Now"
                        } to RequestStatus.IN_PROGRESS
                        RequestStatus.IN_PROGRESS -> when (s.language) {
                            AppLanguage.ARABIC -> "إنهاء الخدمة وتقييم الحرفي"
                            AppLanguage.FRENCH -> "Terminer et noter"
                            AppLanguage.ENGLISH -> "Complete & Rate"
                        } to RequestStatus.COMPLETED
                        else -> null
                    }

                    val canClientCancel = request.status == RequestStatus.ACCEPTED ||
                        request.status == RequestStatus.EN_ROUTE ||
                        request.status == RequestStatus.ARRIVED ||
                        request.status == RequestStatus.IN_PROGRESS

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (canClientCancel) {
                            OutlinedButton(
                                onClick = { showCancelConfirmDialog = true },
                                border = BorderStroke(1.5.dp, UrgentCoral),
                                shape = RoundedCornerShape(99.dp),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .height(50.dp)
                                    .testTag("client_cancel_active_job_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = s.clientCancelService,
                                    tint = UrgentCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = s.clientCancelService,
                                    color = UrgentCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        if (nextAction != null) {
                            Button(
                                onClick = { onAdvanceStatus(nextAction.second) },
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1.45f)
                                    .height(50.dp)
                                    .testTag("advance_job_status_button")
                            ) {
                                Text(nextAction.first, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            }
                        }
                    }

                    if (showCancelConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showCancelConfirmDialog = false },
                            title = {
                                Text(
                                    text = when (s.language) {
                                        AppLanguage.ARABIC -> "تأكيد إلغاء الخدمة"
                                        AppLanguage.FRENCH -> "Confirmer l'annulation"
                                        AppLanguage.ENGLISH -> "Confirm Cancellation"
                                    },
                                    fontWeight = FontWeight.ExtraBold
                                )
                            },
                            text = {
                                Text(
                                    when (s.language) {
                                        AppLanguage.ARABIC -> "هل أنت متأكد من إلغاء هذه الخدمة؟ سيتم إشعار الحرفي وإغلاق الطلب."
                                        AppLanguage.FRENCH -> "Voulez-vous vraiment annuler ce service ? L'artisan sera notifié."
                                        AppLanguage.ENGLISH -> "Are you sure you want to cancel this service? The craftsman will be notified."
                                    }
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCancelConfirmDialog = false
                                        onCancel()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = UrgentCoral,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("confirm_client_cancel_job_button")
                                ) {
                                    Text(s.confirm, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCancelConfirmDialog = false }) {
                                    Text(s.cancel, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingDialog(
    request: ServiceRequest,
    onDismiss: () -> Unit,
    onSubmit: (Int, List<String>, String) -> Unit
) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    val availableTags = listOf("عمل متقن", "في الموعد", "سعر مناسب", "لباقة واحترام", "نظافة المكان")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "قيّم خدمة ${request.assignedCraftsmanName ?: "الحرفي"}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { value ->
                        IconButton(onClick = { stars = value }) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "$value نجوم",
                                tint = if (value <= stars) StarAmber else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableTags.forEach { tag ->
                        val selected = selectedTags.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WepeEmerald,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظاتك (اختياري)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(stars, selectedTags.toList(), comment) },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White)
            ) {
                Text("إرسال التقييم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لاحقاً") }
        }
    )
}
