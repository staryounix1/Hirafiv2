package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class SmartTriageScenario(
    val id: String,
    val categoryId: String,
    val titleAr: String,
    val descriptionAr: String,
    val estimatedMinutes: Int,
    val recommendedPriceDh: Int,
    val isUrgentSuggested: Boolean = false
)

object HirafiQuickScenarios {
    private val scenarios: List<SmartTriageScenario> = listOf(
        // Plumbing
        SmartTriageScenario("plumb_leak", "plumbing", "تسرب تحت الحوض", "تسرب مستمر في أنبوب الصرف أو الصنبور", 35, 120, true),
        SmartTriageScenario("plumb_drain", "plumbing", "تسليك مجرى مسدود", "انسداد في مجرى المطبخ أو الحمام", 45, 140, false),
        SmartTriageScenario("plumb_heater", "plumbing", "إصلاح سخان الماء", "فحص أو تغيير منظم الحرارة أو الأنابيب", 60, 180, false),
        // Electricity
        SmartTriageScenario("elec_breaker", "electricity", "انقطاع القاطع الرئيسي", "عطل مفاجئ في لوحة الكهرباء", 30, 150, true),
        SmartTriageScenario("elec_socket", "electricity", "تغيير مقبس أو مفتاح", "استبدال مأخذ كهربائي محترق أو تالف", 25, 110, false),
        SmartTriageScenario("elec_chandelier", "electricity", "تركيب ثريا وإنارة", "تركيب وفحص التوصيلات بأمان", 50, 170, false),
        // AC
        SmartTriageScenario("ac_gas", "ac", "شحن غاز المكيف", "ضعف التبريد وفحص تسرب الغاز", 55, 250, false),
        SmartTriageScenario("ac_clean", "ac", "تنظيف وتعقيم المكيف", "غسل الفلاتر والوحدة الداخلية", 45, 160, false),
        SmartTriageScenario("ac_fault", "ac", "توقف المكيف فجأة", "تشخيص عطل اللوحة أو الضاغط", 60, 220, true),
        // Carpentry
        SmartTriageScenario("carp_door", "carpentry", "إصلاح باب لا يغلق", "ضبط المفصلات أو الكالون الخشبي", 40, 140, false),
        SmartTriageScenario("carp_cabinet", "carpentry", "تثبيت خزانة مطبخ", "تثبيت أو تعديل أبواب الخزائن", 50, 180, false),
        SmartTriageScenario("carp_furniture", "carpentry", "تجميع أثاث منزلي", "تركيب سريع مع ضبط التوازن", 65, 200, false),
        // Painting
        SmartTriageScenario("paint_room", "painting", "صباغة غرفة واحدة", "تحضير الحائط وطلاء طبقتين", 180, 350, false),
        SmartTriageScenario("paint_humidity", "painting", "معالجة رطوبة الحائط", "تنظيف وعزل بقع الرطوبة وإعادة الطلاء", 120, 280, false),
        // Locksmith
        SmartTriageScenario("lock_open", "locksmith", "فتح باب مغلق طارئ", "فتح احترافي بدون إتلاف الباب", 20, 120, true),
        SmartTriageScenario("lock_change", "locksmith", "تغيير قفل الباب", "تركيب قلب قفل جديد مع مفاتيح", 30, 140, false),
        // Masonry
        SmartTriageScenario("mas_tiles", "masonry", "ترميم زليج مكسور", "تبديل قطع السيراميك وإعادة الفواصل", 75, 220, false),
        SmartTriageScenario("mas_seal", "masonry", "عزل تسرب السطح", "معالجة الشقوق ومنع تسرب الأمطار", 120, 320, true),
        // Cleaning
        SmartTriageScenario("clean_deep", "cleaning", "تنظيف عميق للشقة", "تنظيف شامل للأرضيات والمطبخ والحمام", 150, 200, false),
        SmartTriageScenario("clean_sofa", "cleaning", "غسيل الأرائك والسجاد", "تنظيف بالبخار وإزالة البقع", 90, 170, false)
    )

    fun forCategory(categoryId: String): List<SmartTriageScenario> =
        scenarios.filter { it.categoryId == categoryId }
}

data class AcceptanceRadarMetrics(
    val probabilityPercent: Int,
    val speedLabelAr: String,
    val fairness: FairPriceAssessment
) {
    companion object {
        fun compute(proposal: JobPriceProposal): AcceptanceRadarMetrics {
            val fairness = proposal.assessFairness()
            val span = (proposal.recommendedMaxDh - proposal.recommendedMinDh).coerceAtLeast(20)
            val normalized = (proposal.proposedPriceDh - proposal.recommendedMinDh).toFloat() / span.toFloat()
            val rawPercent = (58 + normalized * 35f).toInt()
            val urgentBonus = if (proposal.isUrgent && proposal.proposedPriceDh >= proposal.recommendedMinDh) 4 else 0
            val percent = (rawPercent + urgentBonus).coerceIn(32, 98)
            val speedLabel = when {
                percent >= 85 -> "استجابة فورية (~1 د)"
                percent >= 65 -> "قبول سريع (~3 د)"
                else -> "استجابة أبطأ — جرّب رفع السعر"
            }
            return AcceptanceRadarMetrics(
                probabilityPercent = percent,
                speedLabelAr = speedLabel,
                fairness = fairness
            )
        }
    }
}

object JobHandshakeSecurity {
    fun normalizePinInput(raw: String): String {
        val builder = StringBuilder(4)
        for (ch in raw) {
            val digit = Character.digit(ch, 10)
            if (digit in 0..9) {
                builder.append(('0'.code + digit).toChar())
                if (builder.length == 4) break
            }
        }
        return builder.toString()
    }

    fun deriveSecurityPin(request: ServiceRequest): String {
        val normalizedId = request.id.trim().lowercase(Locale.US)
        val normalizedCat = request.categoryId.trim().lowercase(Locale.US)
        val rawHash = "$normalizedId|$normalizedCat".hashCode()
        val positiveHash = rawHash.toLong() and 0x7FFFFFFFL
        val fourDigits = (positiveHash % 9000L) + 1000L
        return fourDigits.toString()
    }

    fun verifySecurityPin(rawInput: String, request: ServiceRequest): Boolean {
        val normalized = normalizePinInput(rawInput)
        return normalized.length == 4 && normalized == deriveSecurityPin(request)
    }

    fun deriveWarrantyCode(request: ServiceRequest): String {
        val positiveHash = request.id.trim().lowercase(Locale.US).hashCode().toLong() and 0x7FFFFFFFL
        val codeNum = (positiveHash % 90000L) + 10000L
        return "HRF-$codeNum-30D"
    }

    fun formatCompletionDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        return fmt.format(Date(millis))
    }

    fun buildWarrantyShareText(request: ServiceRequest): String {
        val warrantyCode = deriveWarrantyCode(request)
        val price = request.acceptedPrice ?: request.proposedPrice
        val craftsman = request.assignedCraftsmanName ?: "حرفي معتمد"
        val dateStr = formatCompletionDate(request.updatedAtMillis)
        return buildString {
            appendLine("شهادة ضمان خدمة حرفي (30 يوماً)")
            appendLine("رقم الشهادة: $warrantyCode")
            appendLine("الخدمة: ${request.title} (${request.categoryName})")
            appendLine("الحرفي المنفذ: $craftsman")
            appendLine("الموقع: ${request.addressLabel}")
            appendLine("المبلغ المعتمد: $price درهم")
            appendLine("تاريخ الإنجاز: $dateStr")
        }
    }
}
