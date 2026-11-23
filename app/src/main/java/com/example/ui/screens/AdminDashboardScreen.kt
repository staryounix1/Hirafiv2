package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.AdminUserRecord
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.VerificationSubmission
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.viewmodel.HirafiViewModel

data class AdminOverviewMetrics(
    val totalGmvDh: Int,
    val completionRatePct: Int,
    val activeCount: Int,
    val completedCount: Int,
    val cancelledCount: Int,
    val topCategories: List<Pair<String, Int>>
)

object AdminDashboardAnalytics {
    fun computeOverviewMetrics(requests: List<ServiceRequest>): AdminOverviewMetrics {
        val completed = requests.filter { it.status == RequestStatus.COMPLETED }
        val cancelled = requests.count { it.status == RequestStatus.CANCELLED }
        val active = requests.count {
            it.status != RequestStatus.COMPLETED && it.status != RequestStatus.CANCELLED
        }
        val gmv = completed.sumOf { it.acceptedPrice ?: it.proposedPrice }
        val rate = if (requests.isEmpty()) 0 else ((completed.size * 100f) / requests.size).toInt().coerceIn(0, 100)
        val topCategories = requests
            .groupBy { it.categoryId }
            .map { (categoryId, items) -> categoryId to items.size }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(5)

        return AdminOverviewMetrics(
            totalGmvDh = gmv,
            completionRatePct = rate,
            activeCount = active,
            completedCount = completed.size,
            cancelledCount = cancelled,
            topCategories = topCategories
        )
    }

    fun filterUsers(
        users: List<AdminUserRecord>,
        roleFilter: UserRole?,
        query: String
    ): List<AdminUserRecord> {
        val cleanQuery = query.trim().lowercase()
        return users.filter { user ->
            val matchesRole = roleFilter == null || user.role == roleFilter
            val matchesQuery = cleanQuery.isEmpty() ||
                user.fullName.lowercase().contains(cleanQuery) ||
                user.email.lowercase().contains(cleanQuery) ||
                user.city.lowercase().contains(cleanQuery)
            matchesRole && matchesQuery
        }
    }

    fun filterRequests(
        requests: List<ServiceRequest>,
        statusFilter: RequestStatus?,
        query: String
    ): List<ServiceRequest> {
        val cleanQuery = query.trim().lowercase()
        return requests.filter { req ->
            val matchesStatus = statusFilter == null || req.status == statusFilter
            val matchesQuery = cleanQuery.isEmpty() ||
                req.title.lowercase().contains(cleanQuery) ||
                req.clientName.lowercase().contains(cleanQuery) ||
                req.categoryName.lowercase().contains(cleanQuery) ||
                req.addressLabel.lowercase().contains(cleanQuery) ||
                (req.assignedCraftsmanName?.lowercase()?.contains(cleanQuery) == true)
            matchesStatus && matchesQuery
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: HirafiViewModel,
    onOpenDrawer: () -> Unit
) {
    val s = hirafiStrings()
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val runtimeConfig by viewModel.appRuntimeConfig.collectAsStateWithLifecycle()
    val verifications by viewModel.adminVerifications.collectAsStateWithLifecycle()
    val users by viewModel.adminUsers.collectAsStateWithLifecycle()
    val requests by viewModel.adminAllRequests.collectAsStateWithLifecycle()
    val authorizedAdmins by viewModel.authorizedAdmins.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var kycStatusFilter by remember { mutableStateOf<IdentityVerificationStatus?>(null) }
    var userRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var usersSearchQuery by remember { mutableStateOf("") }
    var requestStatusFilter by remember { mutableStateOf<RequestStatus?>(null) }
    var requestsSearchQuery by remember { mutableStateOf("") }

    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var rejectTarget by remember { mutableStateOf<VerificationSubmission?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var newAdminEmail by remember { mutableStateOf("") }

    // Confirmation dialog states for destructive / global actions
    var pendingProductionToggle by remember { mutableStateOf<Boolean?>(null) }
    var pendingRemoveAdminEmail by remember { mutableStateOf<String?>(null) }
    var pendingSuspendUser by remember { mutableStateOf<AdminUserRecord?>(null) }
    var pendingCancelRequestId by remember { mutableStateOf<String?>(null) }

    val pendingKycCount = verifications.count { it.status == IdentityVerificationStatus.PENDING_REVIEW }
    val overviewMetrics = remember(requests) { AdminDashboardAnalytics.computeOverviewMetrics(requests) }

    val menuContentDesc = when (s.language) {
        AppLanguage.ARABIC -> "القائمة"
        AppLanguage.FRENCH -> "Menu"
        AppLanguage.ENGLISH -> "Menu"
    }
    val prodModeBadgeText = if (runtimeConfig.productionMode) {
        when (s.language) {
            AppLanguage.ARABIC -> "إنتاج حقيقي"
            AppLanguage.FRENCH -> "Production"
            AppLanguage.ENGLISH -> "LIVE"
        }
    } else {
        when (s.language) {
            AppLanguage.ARABIC -> "وضع مختلط"
            AppLanguage.FRENCH -> "Hybride"
            AppLanguage.ENGLISH -> "Hybrid"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianHeroBrush)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Surface(
                        color = Color(0xFF232830),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("admin_open_drawer_button")
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = menuContentDesc, tint = Color.White)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.adminTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = session.email.ifBlank { AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL },
                            color = VoltGlowAccent,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = if (runtimeConfig.productionMode) WepeEmerald else WepeEmeraldSoft,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = prodModeBadgeText,
                            color = if (runtimeConfig.productionMode) Color.White else WepeEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF232830),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.refreshAdminData() },
                            modifier = Modifier.testTag("admin_refresh_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = s.refresh, tint = VoltGlowAccent)
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                    label = { Text(s.adminTabOverview, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                    modifier = Modifier.testTag("admin_tab_overview")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingKycCount > 0) {
                                    Badge(containerColor = UrgentCoral) {
                                        Text("$pendingKycCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null)
                        }
                    },
                    label = { Text(s.adminTabKyc, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                    modifier = Modifier.testTag("admin_tab_kyc")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Group, contentDescription = null) },
                    label = { Text(s.adminTabUsers, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null) },
                    label = { Text(s.adminTabRequests, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                    modifier = Modifier.testTag("admin_tab_requests")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> AdminOverviewTab(
                productionMode = runtimeConfig.productionMode,
                usersCount = users.size,
                craftsmenCount = users.count { it.role == UserRole.CRAFTSMAN },
                pendingKycCount = pendingKycCount,
                metrics = overviewMetrics,
                authorizedAdmins = authorizedAdmins.map { it.email },
                onRequestToggleProductionMode = { pendingProductionToggle = it },
                onOpenAddAdmin = { showAddAdminDialog = true },
                onRequestRemoveAdmin = { pendingRemoveAdminEmail = it },
                modifier = Modifier.padding(innerPadding)
            )

            1 -> AdminKycTab(
                submissions = verifications,
                statusFilter = kycStatusFilter,
                onStatusFilterChange = { kycStatusFilter = it },
                onPreviewPhoto = { previewPhotoUrl = it },
                onApprove = { sub -> viewModel.reviewVerification(sub.craftsmanId, approved = true) },
                onRejectClick = { sub ->
                    rejectTarget = sub
                    rejectionReasonText = ""
                },
                modifier = Modifier.padding(innerPadding)
            )

            2 -> AdminUsersTab(
                users = users,
                roleFilter = userRoleFilter,
                searchQuery = usersSearchQuery,
                onRoleFilterChange = { userRoleFilter = it },
                onSearchQueryChange = { usersSearchQuery = it },
                onRequestToggleSuspended = { pendingSuspendUser = it },
                modifier = Modifier.padding(innerPadding)
            )

            else -> AdminRequestsTab(
                requests = requests,
                statusFilter = requestStatusFilter,
                searchQuery = requestsSearchQuery,
                onStatusFilterChange = { requestStatusFilter = it },
                onSearchQueryChange = { requestsSearchQuery = it },
                onRequestCancel = { pendingCancelRequestId = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    previewPhotoUrl?.let { uri ->
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { previewPhotoUrl = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = s.close, tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = uri,
                        contentDescription = s.adminTabKyc,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }
    }

    rejectTarget?.let { target ->
        val rejectDialogTitle = when (s.language) {
            AppLanguage.ARABIC -> "سبب رفض التوثيق"
            AppLanguage.FRENCH -> "Motif du refus KYC"
            AppLanguage.ENGLISH -> "KYC Rejection Reason"
        }
        val rejectConfirmLabel = when (s.language) {
            AppLanguage.ARABIC -> "تأكيد الرفض"
            AppLanguage.FRENCH -> "Confirmer le refus"
            AppLanguage.ENGLISH -> "Confirm Rejection"
        }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text(rejectDialogTitle, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(target.craftsmanName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = s.adminRejectPresetsLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        s.adminRejectPresets.forEach { preset ->
                            FilterChip(
                                selected = rejectionReasonText == preset,
                                onClick = { rejectionReasonText = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UrgentCoralSoft,
                                    selectedLabelColor = UrgentCoral
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        placeholder = { Text(s.adminRejectPresets.firstOrNull().orEmpty()) },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reviewVerification(
                            craftsmanId = target.craftsmanId,
                            approved = false,
                            rejectionReason = rejectionReasonText
                        )
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentCoral, contentColor = Color.White),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(rejectConfirmLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }

    if (showAddAdminDialog) {
        val addTitle = when (s.language) {
            AppLanguage.ARABIC -> "إضافة أدمين جديد"
            AppLanguage.FRENCH -> "Ajouter un administrateur"
            AppLanguage.ENGLISH -> "Add New Admin"
        }
        val addLabel = when (s.language) {
            AppLanguage.ARABIC -> "إضافة"
            AppLanguage.FRENCH -> "Ajouter"
            AppLanguage.ENGLISH -> "Add"
        }
        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            title = { Text(addTitle, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.authEmailLabel, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        placeholder = { Text("admin@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAuthorizedAdmin(newAdminEmail)
                        newAdminEmail = ""
                        showAddAdminDialog = false
                    },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(addLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAdminDialog = false }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }

    pendingProductionToggle?.let { targetState ->
        AlertDialog(
            onDismissRequest = { pendingProductionToggle = null },
            modifier = Modifier.testTag("admin_confirm_production_dialog"),
            title = { Text(s.adminProductionModeTitle, fontWeight = FontWeight.ExtraBold) },
            text = { Text(s.adminProductionConfirmMsg, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setProductionMode(targetState)
                        pendingProductionToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (targetState) VerifiedEmerald else ObsidianBlack,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(s.confirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingProductionToggle = null }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }

    pendingRemoveAdminEmail?.let { email ->
        AlertDialog(
            onDismissRequest = { pendingRemoveAdminEmail = null },
            modifier = Modifier.testTag("admin_confirm_remove_admin_dialog"),
            title = { Text(s.delete, fontWeight = FontWeight.ExtraBold) },
            text = { Text(email, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeAuthorizedAdmin(email)
                        pendingRemoveAdminEmail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentCoral, contentColor = Color.White),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(s.delete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveAdminEmail = null }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }

    pendingSuspendUser?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { pendingSuspendUser = null },
            title = { Text(s.adminSuspendConfirmTitle, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(targetUser.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(s.adminSuspendConfirmMsg, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleUserSuspended(
                            userId = targetUser.userId,
                            isCraftsman = targetUser.role == UserRole.CRAFTSMAN,
                            suspended = !targetUser.isSuspended
                        )
                        pendingSuspendUser = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (targetUser.isSuspended) VerifiedEmerald else UrgentCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_confirm_suspend_button")
                ) {
                    Text(s.confirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSuspendUser = null }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }

    pendingCancelRequestId?.let { requestId ->
        AlertDialog(
            onDismissRequest = { pendingCancelRequestId = null },
            title = { Text(s.adminCancelReqConfirmTitle, fontWeight = FontWeight.ExtraBold) },
            text = { Text(s.adminCancelReqConfirmMsg, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelRequestAsAdmin(requestId)
                        pendingCancelRequestId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentCoral, contentColor = Color.White),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_confirm_cancel_request_button")
                ) {
                    Text(s.confirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelRequestId = null }, modifier = Modifier.height(48.dp)) {
                    Text(s.cancel)
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    productionMode: Boolean,
    usersCount: Int,
    craftsmenCount: Int,
    pendingKycCount: Int,
    metrics: AdminOverviewMetrics,
    authorizedAdmins: List<String>,
    onRequestToggleProductionMode: (Boolean) -> Unit,
    onOpenAddAdmin: () -> Unit,
    onRequestRemoveAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val craftsmenLabel = when (s.language) {
        AppLanguage.ARABIC -> "الحرفيون"
        AppLanguage.FRENCH -> "Artisans"
        AppLanguage.ENGLISH -> "Craftsmen"
    }
    val pendingKycLabel = when (s.language) {
        AppLanguage.ARABIC -> "ملفات KYC معلقة"
        AppLanguage.FRENCH -> "KYC en attente"
        AppLanguage.ENGLISH -> "Pending KYC"
    }
    val activeReqLabel = when (s.language) {
        AppLanguage.ARABIC -> "طلبات نشطة"
        AppLanguage.FRENCH -> "Demandes actives"
        AppLanguage.ENGLISH -> "Active Jobs"
    }
    val completedLabel = when (s.language) {
        AppLanguage.ARABIC -> "مكتملة"
        AppLanguage.FRENCH -> "Terminées"
        AppLanguage.ENGLISH -> "Completed"
    }
    val activeLegendLabel = when (s.language) {
        AppLanguage.ARABIC -> "نشطة"
        AppLanguage.FRENCH -> "Actives"
        AppLanguage.ENGLISH -> "Active"
    }
    val cancelledLabel = when (s.language) {
        AppLanguage.ARABIC -> "ملغاة"
        AppLanguage.FRENCH -> "Annulées"
        AppLanguage.ENGLISH -> "Cancelled"
    }
    val adminsSectionTitle = when (s.language) {
        AppLanguage.ARABIC -> "حسابات الأدمين المخولة"
        AppLanguage.FRENCH -> "Administrateurs autorisés"
        AppLanguage.ENGLISH -> "Authorized Admins"
    }
    val primaryAdminBadge = when (s.language) {
        AppLanguage.ARABIC -> "أدمين رئيسي"
        AppLanguage.FRENCH -> "Principal"
        AppLanguage.ENGLISH -> "Primary"
    }
    val addLabel = when (s.language) {
        AppLanguage.ARABIC -> "إضافة"
        AppLanguage.FRENCH -> "Ajouter"
        AppLanguage.ENGLISH -> "Add"
    }
    val prodTitle = when (s.language) {
        AppLanguage.ARABIC -> "وضع البيانات الحقيقية بالكامل"
        AppLanguage.FRENCH -> "Mode Données Réelles (Production)"
        AppLanguage.ENGLISH -> "Full Production Data Mode"
    }
    val prodDesc = if (productionMode) {
        when (s.language) {
            AppLanguage.ARABIC -> "مفعّل: يظهر الحرفيون والطلبات المسجلون في السحابة فقط."
            AppLanguage.FRENCH -> "Activé : seuls les artisans et demandes cloud sont visibles."
            AppLanguage.ENGLISH -> "Enabled: only live cloud craftsmen & requests are shown."
        }
    } else {
        when (s.language) {
            AppLanguage.ARABIC -> "معطّل: تظهر بيانات العرض بجانب الحسابات الحقيقية."
            AppLanguage.FRENCH -> "Désactivé : mode hybride avec données de démonstration."
            AppLanguage.ENGLISH -> "Disabled: hybrid mode with demo data alongside cloud records."
        }
    }
    val readinessTitle = when (s.language) {
        AppLanguage.ARABIC -> "جاهزية الإطلاق والإنتاج"
        AppLanguage.FRENCH -> "État système & cloud"
        AppLanguage.ENGLISH -> "System & Cloud Readiness"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // GMV & Completion Rate Hero Row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminStatCard(
                    label = s.adminGmvTitle,
                    value = "${metrics.totalGmvDh} ${s.currencyDh}",
                    container = ObsidianBlack,
                    accent = VoltGlowAccent,
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("admin_gmv_card")
                )
                AdminStatCard(
                    label = s.adminCompletionRateTitle,
                    value = "${metrics.completionRatePct}%",
                    container = VerifiedEmeraldSoft,
                    accent = VerifiedEmerald,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("admin_completion_rate_card")
                )
            }
        }

        // Core Platform KPIs
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminStatCard(s.adminTabUsers, "$usersCount", Color.White, ObsidianBlack, Modifier.weight(1f))
                AdminStatCard(craftsmenLabel, "$craftsmenCount", Color.White, VerifiedEmerald, Modifier.weight(1f))
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminStatCard(pendingKycLabel, "$pendingKycCount", UrgentCoralSoft, UrgentCoral, Modifier.weight(1f))
                AdminStatCard(activeReqLabel, "${metrics.activeCount}", WepeEmeraldSoft, WepeEmeraldDark, Modifier.weight(1f))
            }
        }

        // Canvas Status Breakdown Bar Chart
        item {
            val total = (metrics.completedCount + metrics.activeCount + metrics.cancelledCount).coerceAtLeast(1)
            val completedRatio = metrics.completedCount.toFloat() / total
            val activeRatio = metrics.activeCount.toFloat() / total
            val cancelledRatio = metrics.cancelledCount.toFloat() / total

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_requests_breakdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(s.adminRequestsBreakdownTitle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        drawRect(color = Color(0xFFE4E7EC), size = size)
                        var xOffset = 0f
                        if (completedRatio > 0f) {
                            val w = size.width * completedRatio
                            drawRoundRect(
                                color = VerifiedEmerald,
                                topLeft = Offset(xOffset, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            xOffset += w
                        }
                        if (activeRatio > 0f) {
                            val w = size.width * activeRatio
                            drawRoundRect(
                                color = VoltGlowAccent,
                                topLeft = Offset(xOffset, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            xOffset += w
                        }
                        if (cancelledRatio > 0f) {
                            val w = (size.width - xOffset).coerceAtLeast(0f)
                            drawRoundRect(
                                color = UrgentCoral,
                                topLeft = Offset(xOffset, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BreakdownLegendItem(VerifiedEmerald, "$completedLabel (${metrics.completedCount})")
                        BreakdownLegendItem(VoltGlowAccent, "$activeLegendLabel (${metrics.activeCount})")
                        BreakdownLegendItem(UrgentCoral, "$cancelledLabel (${metrics.cancelledCount})")
                    }
                }
            }
        }

        // Canvas Top Category Demand Chart
        item {
            val maxCategoryCount = (metrics.topCategories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_top_categories_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(s.adminTopCategoriesTitle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    if (metrics.topCategories.isEmpty()) {
                        Text(
                            text = s.ordersEmptyTitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        metrics.topCategories.forEach { (catId, count) ->
                            val category = HirafiCategories.byId(catId)
                            val ratio = count.toFloat() / maxCategoryCount.toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.localizedName(s),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$count",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = VerifiedEmerald
                                    )
                                }
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    drawRect(color = Color(0xFFEAECF0), size = size)
                                    drawRoundRect(
                                        color = WepeEmerald,
                                        topLeft = Offset.Zero,
                                        size = Size(size.width * ratio.coerceIn(0.08f, 1f), size.height),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Production Real Data Mode Card (with confirmation dialog)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, if (productionMode) VerifiedEmerald else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        color = if (productionMode) VerifiedEmeraldSoft else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                tint = if (productionMode) VerifiedEmerald else ObsidianBlack
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prodTitle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(
                            text = prodDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = productionMode,
                        onCheckedChange = onRequestToggleProductionMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VerifiedEmerald
                        ),
                        modifier = Modifier.testTag("admin_production_mode_switch")
                    )
                }
            }
        }

        // Authorized Admins Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = VerifiedEmerald)
                            Text(adminsSectionTitle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenAddAdmin,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(addLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Surface(color = ObsidianBlack, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL,
                                color = VoltGlowAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(primaryAdminBadge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    authorizedAdmins.forEach { email ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(email, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(
                                onClick = { onRequestRemoveAdmin(email) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = s.delete, tint = UrgentCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Release Checklist Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = VoltGlowAccent)
                        Text(readinessTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    Text("• Supabase Auth + PostgreSQL + Realtime", color = VoltGlowAccent, fontSize = 12.sp)
                    Text("• Storage Buckets: hirafi-media & hirafi-kyc", color = VoltGlowAccent, fontSize = 12.sp)
                    Text("• FCM / Local Notifications + Deep Linking", color = Color(0xFFD6DDE6), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BreakdownLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    container: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = container == ObsidianBlack
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = if (!isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = if (isDark) Color.White else ObsidianBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminKycTab(
    submissions: List<VerificationSubmission>,
    statusFilter: IdentityVerificationStatus?,
    onStatusFilterChange: (IdentityVerificationStatus?) -> Unit,
    onPreviewPhoto: (String) -> Unit,
    onApprove: (VerificationSubmission) -> Unit,
    onRejectClick: (VerificationSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val filtered = remember(submissions, statusFilter) {
        if (statusFilter == null) submissions else submissions.filter { it.status == statusFilter }
    }
    val emptyKycText = when (s.language) {
        AppLanguage.ARABIC -> "لا توجد طلبات توثيق هوية في هذا التصنيف"
        AppLanguage.FRENCH -> "Aucune demande KYC dans cette catégorie"
        AppLanguage.ENGLISH -> "No KYC submissions in this category"
    }
    val approveLabel = when (s.language) {
        AppLanguage.ARABIC -> "اعتماد وتوثيق"
        AppLanguage.FRENCH -> "Approuver"
        AppLanguage.ENGLISH -> "Approve"
    }
    val rejectLabel = when (s.language) {
        AppLanguage.ARABIC -> "رفض"
        AppLanguage.FRENCH -> "Refuser"
        AppLanguage.ENGLISH -> "Reject"
    }
    val frontLabel = when (s.language) {
        AppLanguage.ARABIC -> "الأمامية"
        AppLanguage.FRENCH -> "Recto"
        AppLanguage.ENGLISH -> "Front"
    }
    val backLabel = when (s.language) {
        AppLanguage.ARABIC -> "الخلفية"
        AppLanguage.FRENCH -> "Verso"
        AppLanguage.ENGLISH -> "Back"
    }
    val selfieLabel = when (s.language) {
        AppLanguage.ARABIC -> "السيلفي"
        AppLanguage.FRENCH -> "Selfie"
        AppLanguage.ENGLISH -> "Selfie"
    }
    val missingPhotoLabel = when (s.language) {
        AppLanguage.ARABIC -> "غير مرفقة"
        AppLanguage.FRENCH -> "Absente"
        AppLanguage.ENGLISH -> "Missing"
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("${s.all} (${submissions.size})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
            val filterOptions = listOf(
                IdentityVerificationStatus.PENDING_REVIEW,
                IdentityVerificationStatus.VERIFIED,
                IdentityVerificationStatus.REJECTED
            )
            items(filterOptions) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(status) },
                    label = {
                        Text(
                            text = "${status.localizedLabel(s)} (${submissions.count { it.status == status }})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(emptyKycText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.craftsmanId }) { sub ->
                    val cat = HirafiCategories.byId(sub.specialtyId)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.craftsmanName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text(
                                        text = "${cat.localizedName(s)} • ${sub.city} • CIN: ${sub.nationalIdNumber}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = when (sub.status) {
                                        IdentityVerificationStatus.VERIFIED -> VerifiedEmeraldSoft
                                        IdentityVerificationStatus.REJECTED -> UrgentCoralSoft
                                        else -> WepeEmeraldSoft
                                    },
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = sub.status.localizedLabel(s),
                                        color = when (sub.status) {
                                            IdentityVerificationStatus.VERIFIED -> VerifiedEmerald
                                            IdentityVerificationStatus.REJECTED -> UrgentCoral
                                            else -> WepeEmeraldDark
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                KycThumb(frontLabel, missingPhotoLabel, sub.frontPhotoUri, { onPreviewPhoto(sub.frontPhotoUri) }, Modifier.weight(1f))
                                KycThumb(backLabel, missingPhotoLabel, sub.backPhotoUri, { onPreviewPhoto(sub.backPhotoUri) }, Modifier.weight(1f))
                                KycThumb(selfieLabel, missingPhotoLabel, sub.selfiePhotoUri, { onPreviewPhoto(sub.selfiePhotoUri) }, Modifier.weight(1f))
                            }

                            if (sub.status == IdentityVerificationStatus.REJECTED && sub.rejectionReason.isNotBlank()) {
                                Text(sub.rejectionReason, color = UrgentCoral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onApprove(sub) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald, contentColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(approveLabel, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRejectClick(sub) },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, UrgentCoral),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentCoral),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(rejectLabel, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KycThumb(
    label: String,
    missingLabel: String,
    uri: String,
    onClick: () -> Unit,
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
                .height(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(enabled = uri.isNotBlank(), onClick = onClick)
        ) {
            if (uri.isNotBlank()) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(missingLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdminUsersTab(
    users: List<AdminUserRecord>,
    roleFilter: UserRole?,
    searchQuery: String,
    onRoleFilterChange: (UserRole?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRequestToggleSuspended: (AdminUserRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val filtered = remember(users, roleFilter, searchQuery) {
        AdminDashboardAnalytics.filterUsers(users, roleFilter, searchQuery)
    }
    val fcmEnabledText = when (s.language) {
        AppLanguage.ARABIC -> "إشعارات مفعلة"
        AppLanguage.FRENCH -> "Push activé"
        AppLanguage.ENGLISH -> "Push active"
    }
    val activateLabel = when (s.language) {
        AppLanguage.ARABIC -> "تفعيل"
        AppLanguage.FRENCH -> "Activer"
        AppLanguage.ENGLISH -> "Activate"
    }
    val suspendLabel = when (s.language) {
        AppLanguage.ARABIC -> "تعليق"
        AppLanguage.FRENCH -> "Suspendre"
        AppLanguage.ENGLISH -> "Suspend"
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(s.adminUsersSearchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = s.search) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = s.close)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("admin_users_search_input")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = roleFilter == null,
                    onClick = { onRoleFilterChange(null) },
                    label = { Text("${s.all} (${users.size})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(UserRole.entries) { role ->
                FilterChip(
                    selected = roleFilter == role,
                    onClick = { onRoleFilterChange(role) },
                    label = { Text("${role.localizedLabel(s)} (${users.count { it.role == role }})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.userId }) { u ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (u.isSuspended) UrgentCoral else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(u.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Surface(color = WepeEmeraldSoft, shape = RoundedCornerShape(99.dp)) {
                                    Text(u.role.localizedLabel(s), color = WepeEmeraldDark, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                            Text("${u.email} • ${u.city}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (u.fcmTokenPresent) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(13.dp))
                                        Text(fcmEnabledText, fontSize = 10.sp, color = VerifiedEmerald, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (u.role == UserRole.CRAFTSMAN) {
                                    Text(u.verificationStatus.localizedLabel(s), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (u.role != UserRole.ADMIN) {
                            OutlinedButton(
                                onClick = { onRequestToggleSuspended(u) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (u.isSuspended) VerifiedEmerald else UrgentCoral),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (u.isSuspended) VerifiedEmerald else UrgentCoral
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (u.isSuspended) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (u.isSuspended) activateLabel else suspendLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRequestsTab(
    requests: List<ServiceRequest>,
    statusFilter: RequestStatus?,
    searchQuery: String,
    onStatusFilterChange: (RequestStatus?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRequestCancel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val filtered = remember(requests, statusFilter, searchQuery) {
        AdminDashboardAnalytics.filterRequests(requests, statusFilter, searchQuery)
    }
    val cancelAdminLabel = when (s.language) {
        AppLanguage.ARABIC -> "إلغاء الطلب إدارياً"
        AppLanguage.FRENCH -> "Annuler la demande"
        AppLanguage.ENGLISH -> "Cancel Request"
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(s.adminRequestsSearchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = s.search) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = s.close)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("admin_requests_search_input")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("${s.all} (${requests.size})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(RequestStatus.entries) { status ->
                val count = requests.count { it.status == status }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(status) },
                    label = { Text("${status.localizedLabel(s)} ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WepeEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(s.ordersEmptyTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { req ->
                    val isTerminal = req.status == RequestStatus.COMPLETED || req.status == RequestStatus.CANCELLED
                    val cat = HirafiCategories.byId(req.categoryId)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(req.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Surface(
                                    color = if (isTerminal) MaterialTheme.colorScheme.surfaceVariant else WepeEmeraldSoft,
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = req.status.localizedLabel(s),
                                        color = if (isTerminal) MaterialTheme.colorScheme.onSurfaceVariant else WepeEmeraldDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${cat.localizedName(s)} • ${req.proposedPrice} ${s.currencyDh} • ${req.addressLabel}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${s.roleClient}: ${req.clientName}" +
                                    (req.assignedCraftsmanName?.let { " • ${s.roleCraftsman}: $it" } ?: ""),
                                fontSize = 12.sp
                            )

                            if (!isTerminal) {
                                OutlinedButton(
                                    onClick = { onRequestCancel(req.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, UrgentCoral),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentCoral),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(cancelAdminLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

