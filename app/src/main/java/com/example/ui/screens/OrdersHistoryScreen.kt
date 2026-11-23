package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.HirafiCategories
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.components.WarrantyCertificateDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.HirafiStrings
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.viewmodel.HirafiViewModel

private enum class OrderFilterTab {
    ALL,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    fun localizedLabel(s: HirafiStrings): String = when (this) {
        ALL -> s.all
        ACTIVE -> when (s.language) {
            AppLanguage.ARABIC -> "جارية"
            AppLanguage.FRENCH -> "En cours"
            AppLanguage.ENGLISH -> "Active"
        }
        COMPLETED -> when (s.language) {
            AppLanguage.ARABIC -> "مكتملة"
            AppLanguage.FRENCH -> "Terminées"
            AppLanguage.ENGLISH -> "Completed"
        }
        CANCELLED -> when (s.language) {
            AppLanguage.ARABIC -> "ملغاة"
            AppLanguage.FRENCH -> "Annulées"
            AppLanguage.ENGLISH -> "Cancelled"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    viewModel: HirafiViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val s = hirafiStrings()
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(OrderFilterTab.ALL) }
    var warrantyTarget by remember { mutableStateOf<ServiceRequest?>(null) }

    val roleRequests = if (session.role == UserRole.CRAFTSMAN) {
        allRequests.filter { req ->
            req.categoryId == session.craftsmanSpecialtyId &&
                (req.assignedCraftsmanId == session.userId || req.status == RequestStatus.SEARCHING)
        }
    } else {
        allRequests.filter { it.clientId == session.userId || it.assignedCraftsmanId != null }
    }

    val filteredRequests = when (selectedFilter) {
        OrderFilterTab.ALL -> roleRequests
        OrderFilterTab.ACTIVE -> roleRequests.filter {
            it.status != RequestStatus.COMPLETED && it.status != RequestStatus.CANCELLED
        }
        OrderFilterTab.COMPLETED -> roleRequests.filter { it.status == RequestStatus.COMPLETED }
        OrderFilterTab.CANCELLED -> roleRequests.filter { it.status == RequestStatus.CANCELLED }
    }

    val completedList = roleRequests.filter { it.status == RequestStatus.COMPLETED }
    val totalAmountDh = completedList.sumOf { it.acceptedPrice ?: it.proposedPrice }

    val totalEarningsLabel = when {
        session.role == UserRole.CRAFTSMAN -> when (s.language) {
            AppLanguage.ARABIC -> "مجموع الأرباح"
            AppLanguage.FRENCH -> "Revenus totaux"
            AppLanguage.ENGLISH -> "Total earnings"
        }
        else -> when (s.language) {
            AppLanguage.ARABIC -> "إجمالي الخدمات"
            AppLanguage.FRENCH -> "Total services"
            AppLanguage.ENGLISH -> "Total spent"
        }
    }
    val completedJobsLabel = when (s.language) {
        AppLanguage.ARABIC -> "خدمات منجزة"
        AppLanguage.FRENCH -> "Interventions"
        AppLanguage.ENGLISH -> "Completed"
    }
    val warrantiesLabel = when (s.language) {
        AppLanguage.ARABIC -> "شهادات ضمان"
        AppLanguage.FRENCH -> "Garanties"
        AppLanguage.ENGLISH -> "Warranties"
    }
    val warrantyBtnLabel = when (s.language) {
        AppLanguage.ARABIC -> "شهادة الضمان"
        AppLanguage.FRENCH -> "Garantie"
        AppLanguage.ENGLISH -> "Warranty"
    }
    val pricePrefix = when (s.language) {
        AppLanguage.ARABIC -> "السعر:"
        AppLanguage.FRENCH -> "Tarif :"
        AppLanguage.ENGLISH -> "Price:"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.ordersTitle, fontWeight = FontWeight.ExtraBold)
                        if (session.role == UserRole.CRAFTSMAN) {
                            val specName = HirafiCategories.byId(session.craftsmanSpecialtyId).localizedName(s)
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "مهنتك: $specName فقط"
                                    AppLanguage.FRENCH -> "Spécialité : $specName"
                                    AppLanguage.ENGLISH -> "Trade: $specName"
                                },
                                fontSize = 11.sp,
                                color = VerifiedEmerald
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hirafi_hero),
                        contentDescription = s.appTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                    )
                }
            }

            val activeCount = roleRequests.count {
                it.status != RequestStatus.COMPLETED && it.status != RequestStatus.CANCELLED
            }
            val cancelledCount = roleRequests.count { it.status == RequestStatus.CANCELLED }

            // Obsidian Smart Ledger Summary + Visual Breakdown Bar
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianHeroBrush)
                        .border(1.dp, WepeEmerald.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .testTag("orders_analytics_ledger")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LedgerStatTile(
                                label = totalEarningsLabel,
                                value = "$totalAmountDh ${s.currencyDh}",
                                modifier = Modifier.weight(1.2f)
                            )
                            LedgerStatTile(
                                label = completedJobsLabel,
                                value = "${completedList.size}",
                                modifier = Modifier.weight(0.9f)
                            )
                            LedgerStatTile(
                                label = warrantiesLabel,
                                value = "${completedList.size}",
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        if (roleRequests.isNotEmpty()) {
                            val totalCount = roleRequests.size.toFloat().coerceAtLeast(1f)
                            val completedRatio = completedList.size / totalCount
                            val activeRatio = activeCount / totalCount
                            val cancelledRatio = cancelledCount / totalCount

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    drawRect(color = Color(0xFF282D35), size = size)
                                    var xOffset = 0f
                                    if (completedRatio > 0f) {
                                        val w = size.width * completedRatio
                                        drawRoundRect(
                                            color = WepeEmerald,
                                            topLeft = Offset(xOffset, 0f),
                                            size = Size(w, size.height),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                        xOffset += w
                                    }
                                    if (activeRatio > 0f) {
                                        val w = size.width * activeRatio
                                        drawRoundRect(
                                            color = VoltGlowAccent,
                                            topLeft = Offset(xOffset, 0f),
                                            size = Size(w, size.height),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                        xOffset += w
                                    }
                                    if (cancelledRatio > 0f) {
                                        val w = (size.width - xOffset).coerceAtLeast(0f)
                                        drawRoundRect(
                                            color = UrgentCoral,
                                            topLeft = Offset(xOffset, 0f),
                                            size = Size(w, size.height),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LedgerLegendItem(
                                        color = WepeEmerald,
                                        text = "${OrderFilterTab.COMPLETED.localizedLabel(s)} (${completedList.size})"
                                    )
                                    LedgerLegendItem(
                                        color = VoltGlowAccent,
                                        text = "${OrderFilterTab.ACTIVE.localizedLabel(s)} ($activeCount)"
                                    )
                                    LedgerLegendItem(
                                        color = UrgentCoral,
                                        text = "${OrderFilterTab.CANCELLED.localizedLabel(s)} ($cancelledCount)"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OrderFilterTab.entries) { tab ->
                        FilterChip(
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            label = { Text(tab.localizedLabel(s), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WepeEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("order_filter_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredRequests.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(s.ordersEmptyTitle, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                items(filteredRequests, key = { it.id }) { req ->
                    val category = HirafiCategories.byId(req.categoryId)
                    val statusColor = when (req.status) {
                        RequestStatus.COMPLETED -> VerifiedEmerald
                        RequestStatus.CANCELLED -> UrgentCoral
                        else -> ObsidianBlack
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(category.accentColor.copy(alpha = 0.14f))
                                    ) {
                                        Icon(category.icon, contentDescription = null, tint = category.accentColor, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = req.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(category.localizedName(s), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    color = if (req.status == RequestStatus.SEARCHING) WepeEmeraldSoft else statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = req.status.localizedLabel(s),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (req.status == RequestStatus.SEARCHING) WepeEmeraldDark else statusColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(req.addressLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$pricePrefix ${req.acceptedPrice ?: req.proposedPrice} ${s.currencyDh}",
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedEmerald
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (session.role == UserRole.CLIENT &&
                                        (req.status == RequestStatus.COMPLETED || req.status == RequestStatus.CANCELLED)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.prefillDraftFromRequest(req)
                                                onBack()
                                            },
                                            shape = RoundedCornerShape(99.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("reorder_request_${req.id}")
                                        ) {
                                            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(s.clientReorderLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (req.status == RequestStatus.COMPLETED) {
                                        OutlinedButton(
                                            onClick = { warrantyTarget = req },
                                            shape = RoundedCornerShape(99.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("open_warranty_${req.id}")
                                        ) {
                                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(warrantyBtnLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (req.assignedCraftsmanName != null) {
                                        Button(
                                            onClick = { onOpenChat(req.id) },
                                            shape = RoundedCornerShape(99.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(s.clientOpenChat, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    warrantyTarget?.let { target ->
        WarrantyCertificateDialog(
            request = target,
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, warrantyBtnLabel)
                    putExtra(Intent.EXTRA_TEXT, JobHandshakeSecurity.buildWarrantyShareText(target))
                }
                context.startActivity(Intent.createChooser(shareIntent, warrantyBtnLabel))
            },
            onDismiss = { warrantyTarget = null }
        )
    }
}

@Composable
private fun LedgerLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = Color(0xFFD0D5DD),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LedgerStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF20242A),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                color = Color(0xFF98A2B3),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = VoltGlowAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
