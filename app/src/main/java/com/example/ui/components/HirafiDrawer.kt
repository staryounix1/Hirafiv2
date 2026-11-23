package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountRolePolicy
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeSoftCard

@Composable
fun HirafiDrawerContent(
    session: UserSession,
    currentRoute: String,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    onNavigate: (String) -> Unit,
    onCityClick: () -> Unit,
    onLanguageClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val s = hirafiStrings()

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(305.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Authentic inDrive Clean Profile Header (Clickable -> Profile Screen)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .clickable { onNavigate("profile") }
                        .padding(14.dp)
                        .testTag("drawer_profile_header")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(WepeEmerald)
                    ) {
                        if (!session.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = session.avatarUri,
                                contentDescription = "الصورة الشخصية",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = session.fullName.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = session.fullName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (session.isCraftsmanVerified) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "هوية موثقة",
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = StarAmber, modifier = Modifier.size(14.dp))
                            Text(
                                text = "4.9 • ${session.role.localizedLabel(s)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (session.email.isNotBlank()) {
                            Text(
                                text = session.email,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        if (session.role == UserRole.CRAFTSMAN && session.isCraftsmanSpecialtyLocked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(12.dp))
                                Text(
                                    text = HirafiCategories.byId(session.craftsmanSpecialtyId).localizedName(s),
                                    color = VerifiedEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                DrawerNavItem(
                    label = when (session.role) {
                        UserRole.ADMIN -> s.navAdminDashboard
                        UserRole.CRAFTSMAN -> s.navCraftsmanHome
                        UserRole.CLIENT -> s.navClientHome
                    },
                    icon = Icons.Rounded.Home,
                    selected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                DrawerNavItem(
                    label = s.navProfile,
                    icon = Icons.Rounded.Person,
                    selected = currentRoute == "profile",
                    badgeText = if (session.role == UserRole.CRAFTSMAN) s.navPortfolioBadge else null,
                    onClick = { onNavigate("profile") },
                    testTag = "drawer_profile_item"
                )
                if (session.role != UserRole.ADMIN) {
                    DrawerNavItem(
                        label = s.navOrders,
                        icon = Icons.Rounded.History,
                        selected = currentRoute == "orders",
                        onClick = { onNavigate("orders") }
                    )
                }
                if (AccountRolePolicy.canAccessIdentityVerification(session)) {
                    val (badgeLabel, badgeContainer, badgeTextColor) = when (session.verificationStatus) {
                        IdentityVerificationStatus.VERIFIED ->
                            Triple("${s.kycVerified} ✓", VerifiedEmeraldSoft, VerifiedEmerald)
                        IdentityVerificationStatus.PENDING_REVIEW ->
                            Triple(s.kycPending, ElectricLimeSoft, ObsidianBlack)
                        IdentityVerificationStatus.REJECTED ->
                            Triple(s.kycRejected, UrgentCoralSoft, UrgentCoral)
                        IdentityVerificationStatus.NOT_SUBMITTED ->
                            Triple(s.kycNotSubmitted, UrgentCoralSoft, UrgentCoral)
                    }
                    DrawerNavItem(
                        label = s.navVerification,
                        icon = Icons.Rounded.VerifiedUser,
                        selected = currentRoute == "verification",
                        badgeText = badgeLabel,
                        badgeContainerColor = badgeContainer,
                        badgeTextColor = badgeTextColor,
                        onClick = { onNavigate("verification") },
                        testTag = "drawer_identity_verification_item"
                    )
                }
                DrawerNavItem(
                    label = s.navSafety,
                    icon = Icons.Rounded.Shield,
                    selected = currentRoute == "safety",
                    onClick = { onNavigate("safety") }
                )
                DrawerNavItem(
                    label = "${s.navCityPrefix} ${session.city}",
                    icon = Icons.Rounded.LocationCity,
                    selected = false,
                    onClick = onCityClick
                )
                DrawerNavItem(
                    label = "${s.navLanguagePrefix} ${appLanguage.nativeLabel}",
                    icon = Icons.Rounded.Language,
                    selected = false,
                    badgeText = appLanguage.shortCode,
                    onClick = onLanguageClick,
                    testTag = "drawer_language_item"
                )
            }

            // Permanent Locked Account Indicator + Logout Action
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider()
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = WepeSoftCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, WepeEmerald.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .testTag("locked_account_role_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = WepeEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (session.role) {
                                    UserRole.ADMIN -> s.roleBadgeAdminTitle
                                    UserRole.CRAFTSMAN -> "${s.roleCraftsman} (${HirafiCategories.byId(session.craftsmanSpecialtyId).localizedName(s)})"
                                    UserRole.CLIENT -> s.roleClient
                                },
                                color = WepeEmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = when {
                                    session.isSuspended -> s.accountSuspendedMsg
                                    session.role == UserRole.ADMIN -> s.roleBadgeAdminDesc
                                    session.role == UserRole.CRAFTSMAN -> s.roleBadgeCraftsmanDesc
                                    else -> s.roleBadgeClientDesc
                                },
                                color = if (session.isSuspended) UrgentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (session.isSupabaseAuthenticated) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VerifiedEmeraldSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VerifiedEmerald.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .testTag("supabase_connected_status_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            LivePulseDot(color = VerifiedEmerald, dotSize = 7.dp)
                            Text(
                                text = s.connected,
                                color = VerifiedEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("drawer_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = s.logoutAction,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = s.logoutTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(s.logoutMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text(s.logoutConfirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text(s.cancel)
                }
            }
        )
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badgeText: String? = null,
    badgeContainerColor: Color = WepeEmeraldSoft,
    badgeTextColor: Color = WepeEmeraldDark,
    testTag: String? = null
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        icon = { Icon(icon, contentDescription = null) },
        badge = if (badgeText != null) {
            {
                Surface(
                    color = badgeContainerColor,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        } else null,
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = WepeEmeraldSoft,
            selectedIconColor = WepeEmeraldDark,
            selectedTextColor = WepeEmeraldDark
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .let { mod -> if (testTag != null) mod.testTag(testTag) else mod }
    )
}
