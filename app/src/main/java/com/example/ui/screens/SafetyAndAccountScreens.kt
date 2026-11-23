package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LivePulseDot
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VoltGlowAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val s = hirafiStrings()

    val policeTitle = when (s.language) {
        AppLanguage.ARABIC -> "الشرطة (19)"
        AppLanguage.FRENCH -> "Police (19)"
        AppLanguage.ENGLISH -> "Police (19)"
    }
    val rescueTitle = when (s.language) {
        AppLanguage.ARABIC -> "الوقاية المدنية (150)"
        AppLanguage.FRENCH -> "Protection Civile (150)"
        AppLanguage.ENGLISH -> "Emergency Rescue (150)"
    }
    val quickCallLabel = when (s.language) {
        AppLanguage.ARABIC -> "اتصال سريع"
        AppLanguage.FRENCH -> "Appel rapide"
        AppLanguage.ENGLISH -> "Quick Call"
    }
    val liveBadgeText = when (s.language) {
        AppLanguage.ARABIC -> "حماية نشطة 24/7"
        AppLanguage.FRENCH -> "Protection active 24/7"
        AppLanguage.ENGLISH -> "24/7 Active Protection"
    }

    val tip1Title = when (s.language) {
        AppLanguage.ARABIC -> "تحقق من شارة التوثيق"
        AppLanguage.FRENCH -> "Vérifiez le badge vérifié"
        AppLanguage.ENGLISH -> "Check the verified badge"
    }
    val tip1Body = when (s.language) {
        AppLanguage.ARABIC -> "تأكد من تطابق اسم الحرفي وصفته مع ما يظهر لك في بطاقة العرض المقبول."
        AppLanguage.FRENCH -> "Assurez-vous que le nom et le métier de l'artisan correspondent à l'offre acceptée."
        AppLanguage.ENGLISH -> "Make sure the craftsman's name and trade match the accepted offer card."
    }

    val tip2Title = when (s.language) {
        AppLanguage.ARABIC -> "اتفق على السعر قبل البدء"
        AppLanguage.FRENCH -> "Confirmez le prix avant le début"
        AppLanguage.ENGLISH -> "Agree on price before work starts"
    }
    val tip2Body = when (s.language) {
        AppLanguage.ARABIC -> "السعر الذي قبلته في التطبيق هو السعر المعتمد. أي تعديل إضافي يتم بالاتفاق المباشر."
        AppLanguage.FRENCH -> "Le tarif accepté dans l'application est officiel. Tout ajustement nécessite un accord mutuel."
        AppLanguage.ENGLISH -> "The accepted in-app price is official. Any adjustment requires mutual agreement."
    }

    val tip3Title = when (s.language) {
        AppLanguage.ARABIC -> "قيّم الخدمة بعد الانتهاء"
        AppLanguage.FRENCH -> "Évaluez le service après la fin"
        AppLanguage.ENGLISH -> "Rate the service upon completion"
    }
    val tip3Body = when (s.language) {
        AppLanguage.ARABIC -> "تقييمك الصادق يحافظ على جودة وموثوقية الحرفيين في شبكتنا."
        AppLanguage.FRENCH -> "Votre avis honnête garantit la qualité et la fiabilité des artisans sur la plateforme."
        AppLanguage.ENGLISH -> "Your honest rating keeps craftsmen quality and trust high across our network."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text(s.safetyHeroTitle, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ObsidianHeroBrush)
                    .border(1.dp, VoltGlowAccent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(34.dp))
                        Surface(
                            color = VerifiedEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                LivePulseDot(color = VerifiedEmerald, dotSize = 8.dp)
                                Text(
                                    text = liveBadgeText,
                                    color = Color(0xFF9DF8CC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(s.safetyHeroTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text(
                        text = s.safetyHeroSubtitle,
                        color = Color(0xFFD0D8E0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                EmergencyCard(
                    title = policeTitle,
                    quickCallLabel = quickCallLabel,
                    icon = Icons.Rounded.LocalPolice,
                    color = UrgentCoral,
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:19"))) },
                    modifier = Modifier.weight(1f)
                )
                EmergencyCard(
                    title = rescueTitle,
                    quickCallLabel = quickCallLabel,
                    icon = Icons.Rounded.MedicalServices,
                    color = UrgentCoral,
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:150"))) },
                    modifier = Modifier.weight(1f)
                )
            }

            SafetyTipRow(
                icon = Icons.Rounded.VerifiedUser,
                title = tip1Title,
                body = tip1Body
            )
            SafetyTipRow(
                icon = Icons.Rounded.Lock,
                title = tip2Title,
                body = tip2Body
            )
            SafetyTipRow(
                icon = Icons.Rounded.CheckCircle,
                title = tip3Title,
                body = tip3Body
            )
        }
    }
}

@Composable
private fun EmergencyCard(
    title: String,
    quickCallLabel: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            Spacer(Modifier.size(6.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(quickCallLabel, fontSize = 11.sp, color = color)
            }
        }
    }
}

@Composable
private fun SafetyTipRow(icon: ImageVector, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

