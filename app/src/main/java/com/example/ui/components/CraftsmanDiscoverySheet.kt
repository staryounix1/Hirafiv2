package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkHistory
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.CraftsmanPin
import com.example.data.model.CraftsmanSortOption
import com.example.data.model.HirafiCategories
import com.example.data.model.PortfolioItem
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyCraftsmenSearchSheet(
    craftsmen: List<CraftsmanPin>,
    selectedCategoryId: String,
    searchQuery: String,
    searchRadiusKm: Int,
    selectedSort: CraftsmanSortOption,
    onlySelectedCategory: Boolean,
    onlyCloudCraftsmen: Boolean = false,
    cloudCraftsmenCount: Int = 0,
    onSearchQueryChange: (String) -> Unit,
    onSelectRadiusKm: (Int) -> Unit,
    onSelectSort: (CraftsmanSortOption) -> Unit,
    onToggleOnlyCategory: (Boolean) -> Unit,
    onToggleOnlyCloudCraftsmen: (Boolean) -> Unit = {},
    onPreviewCraftsman: (CraftsmanPin) -> Unit,
    onDirectRequestCraftsman: (CraftsmanPin) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedCategory = HirafiCategories.byId(selectedCategoryId)
    val radiusOptions = listOf(2 to "2 كم", 5 to "5 كم", 10 to "10 كم", 20 to "الكل")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = WepeEmeraldSoft,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PersonSearch,
                                contentDescription = null,
                                tint = WepeEmeraldDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "اكتشف الحرفيين القريبين (${craftsmen.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (cloudCraftsmenCount > 0) {
                                "يشمل $cloudCraftsmenCount حرفياً مسجلاً سحابياً في قاعدة البيانات"
                            } else {
                                "تصفية حسب المسافة، التقييم، والتخصص المطلوب"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (cloudCraftsmenCount > 0) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (cloudCraftsmenCount > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_discovery_sheet_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("ابحث باسم الحرفي، التخصص، أو نوع العطل...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = WepeEmeraldDark) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WepeEmerald,
                    unfocusedBorderColor = WepeHairline,
                    focusedContainerColor = WepeSoftCard,
                    unfocusedContainerColor = WepeSoftCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("craftsman_search_input")
            )

            // Radius & Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(radiusOptions) { (km, label) ->
                    FilterChip(
                        selected = searchRadiusKm == km,
                        onClick = { onSelectRadiusKm(km) },
                        leadingIcon = if (searchRadiusKm == km) {
                            { Icon(Icons.Rounded.NearMe, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WepeEmerald,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("radius_chip_$km")
                    )
                }

                item {
                    FilterChip(
                        selected = onlySelectedCategory,
                        onClick = { onToggleOnlyCategory(!onlySelectedCategory) },
                        leadingIcon = {
                            Icon(
                                imageVector = selectedCategory.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "تخصص ${selectedCategory.nameAr} فقط",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WepeEmerald,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("only_category_filter_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = onlyCloudCraftsmen,
                        onClick = { onToggleOnlyCloudCraftsmen(!onlyCloudCraftsmen) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "المسجلون سحابياً فقط ($cloudCraftsmenCount)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerifiedEmerald,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("only_cloud_craftsmen_chip")
                    )
                }
            }

            // Sort options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "الترتيب:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CraftsmanSortOption.entries) { option ->
                        FilterChip(
                            selected = selectedSort == option,
                            onClick = { onSelectSort(option) },
                            label = { Text(option.labelAr, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WepeEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("craftsman_sort_${option.name.lowercase()}")
                        )
                    }
                }
            }

            HorizontalDivider()

            // List or Empty State
            if (craftsmen.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "لم يتم العثور على حرفيين مطابقين في هذا النطاق",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "جرّب توسيع المسافة إلى الكل أو إلغاء فلتر التخصص",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            onSearchQueryChange("")
                            onToggleOnlyCategory(false)
                            onToggleOnlyCloudCraftsmen(false)
                            onSelectRadiusKm(20)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WepeEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("reset_craftsman_filters_button")
                    ) {
                        Text("عرض جميع الحرفيين القريبين", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(craftsmen, key = { it.id }) { pin ->
                        CraftsmanDiscoveryCard(
                            pin = pin,
                            onPreview = { onPreviewCraftsman(pin) },
                            onDirectRequest = { onDirectRequestCraftsman(pin) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CraftsmanDiscoveryCard(
    pin: CraftsmanPin,
    onPreview: () -> Unit,
    onDirectRequest: () -> Unit
) {
    val category = HirafiCategories.byId(pin.specialtyId)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WepeHairline, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WepeEmeraldSoft)
                    ) {
                        if (!pin.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = pin.avatarUri,
                                contentDescription = pin.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = pin.name.take(1),
                                color = WepeEmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = pin.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pin.isVerified) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "حرفي موثق",
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (pin.isLiveCloud) {
                                Surface(
                                    color = VerifiedEmeraldSoft,
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = "مباشر",
                                        color = VerifiedEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = category.accentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = pin.specialtyName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = category.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = StarAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = " ${pin.rating} (${pin.completedJobs})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (pin.city.isNotBlank()) {
                                Text(
                                    text = "• ${pin.city}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${pin.basePriceDh} د.م",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VerifiedEmerald
                    )
                    Text(
                        text = "${pin.distanceKm} كم • ${pin.etaMinutes} د",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${pin.experienceYears} سنوات خبرة • ${pin.bio}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("preview_craftsman_${pin.id}")
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("الموقع والتفاصيل", fontSize = 12.sp)
                }
                Button(
                    onClick = onDirectRequest,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WepeEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("direct_request_${pin.id}")
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("طلب مباشر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CraftsmanDetailDialog(
    pin: CraftsmanPin,
    portfolioItems: List<PortfolioItem> = emptyList(),
    onDirectRequest: (CraftsmanPin) -> Unit,
    onSelectSpecialtyOnly: (CraftsmanPin) -> Unit,
    onDismiss: () -> Unit
) {
    val category = HirafiCategories.byId(pin.specialtyId)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(WepeEmeraldSoft)
                ) {
                    if (!pin.avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = pin.avatarUri,
                            contentDescription = pin.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = pin.name.take(1),
                            color = WepeEmeraldDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pin.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (pin.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = category.accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = pin.specialtyName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (pin.city.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = pin.city,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (pin.isLiveCloud) {
                            Surface(
                                color = VerifiedEmeraldSoft,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "حساب سحابي",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedEmerald,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = WepeSoftCard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = StarAmber, modifier = Modifier.size(16.dp))
                                Text(" ${pin.rating}", fontWeight = FontWeight.Bold)
                            }
                            Text("${pin.completedJobs} خدمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WorkHistory, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                Text(" ${pin.experienceYears} سنوات", fontWeight = FontWeight.Bold)
                            }
                            Text("خبرة ميدانية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                                Text(" ${pin.etaMinutes} د", fontWeight = FontWeight.Bold)
                            }
                            Text("${pin.distanceKm} كم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text(
                    text = pin.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (portfolioItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.Workspaces, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                            Text(
                                text = "معرض الأعمال (${portfolioItems.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(portfolioItems, key = { it.id }) { work ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (!work.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = work.photoUri,
                                                contentDescription = work.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(74.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                        Text(
                                            text = work.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (work.description.isNotBlank()) {
                                            Text(
                                                text = work.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = WepeEmeraldSoft,
                    border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "السعر الابتدائي المقترح",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WepeEmeraldDark
                        )
                        Text(
                            text = "${pin.basePriceDh} درهم",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WepeEmeraldDark
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onSelectSpecialtyOnly(pin) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("تجهيز طلب في تخصص ${pin.specialtyName} فقط")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDirectRequest(pin) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WepeEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("dialog_direct_request_button")
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("طلب مباشر بـ ${pin.basePriceDh} د.م", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("إغلاق")
            }
        }
    )
}
