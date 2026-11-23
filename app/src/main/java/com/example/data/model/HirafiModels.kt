package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Carpenter
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.security.MessageDigest

enum class UserRole(val dbValue: String, val labelAr: String) {
    CLIENT("client", "زبون"),
    CRAFTSMAN("craftsman", "حرفي"),
    ADMIN("admin", "أدمين");

    companion object {
        fun fromDb(value: String?): UserRole =
            entries.firstOrNull { it.dbValue == value } ?: CLIENT
    }
}

enum class PaymentMethod(val dbValue: String, val labelAr: String) {
    CASH("cash", "نقداً"),
    TRANSFER("transfer", "تحويل بنكي"),
    CARD("card", "بطاقة")
}

enum class RequestStatus(val dbValue: String, val labelAr: String) {
    SEARCHING("searching", "جاري البحث عن حرفي"),
    ACCEPTED("accepted", "تم قبول العرض"),
    EN_ROUTE("en_route", "الحرفي في الطريق"),
    ARRIVED("arrived", "وصل الحرفي للموقع"),
    IN_PROGRESS("in_progress", "جاري إنجاز الخدمة"),
    COMPLETED("completed", "تمت الخدمة"),
    CANCELLED("cancelled", "ملغاة");

    companion object {
        fun fromDb(value: String): RequestStatus =
            entries.firstOrNull { it.dbValue == value } ?: SEARCHING
    }
}

enum class OfferStatus(val dbValue: String, val labelAr: String) {
    PENDING("pending", "قيد الانتظار"),
    ACCEPTED("accepted", "مقبول"),
    DECLINED("declined", "مرفوض"),
    COUNTERED("countered", "مفاوضة"),
    EXPIRED("expired", "منتهي");

    companion object {
        fun fromDb(value: String): OfferStatus =
            entries.firstOrNull { it.dbValue == value } ?: PENDING
    }
}

data class ServiceCategory(
    val id: String,
    val nameAr: String,
    val icon: ImageVector,
    val minRecommendedDh: Int,
    val maxRecommendedDh: Int,
    val defaultPriceDh: Int,
    val accentColor: Color
)

object HirafiCategories {
    val all = listOf(
        ServiceCategory("plumbing", "سباكة", Icons.Rounded.Plumbing, 100, 180, 120, Color(0xFF00A8E8)),
        ServiceCategory("electricity", "كهرباء", Icons.Rounded.ElectricBolt, 120, 220, 150, Color(0xFFFFB300)),
        ServiceCategory("ac", "تبريد وتكييف", Icons.Rounded.AcUnit, 150, 300, 200, Color(0xFF00B894)),
        ServiceCategory("carpentry", "نجارة", Icons.Rounded.Carpenter, 130, 250, 160, Color(0xFFD35400)),
        ServiceCategory("painting", "صباغة", Icons.Rounded.FormatPaint, 180, 400, 250, Color(0xFF8E44AD)),
        ServiceCategory("locksmith", "أقفال ومفاتيح", Icons.Rounded.Lock, 80, 160, 100, Color(0xFFE74C3C)),
        ServiceCategory("masonry", "بناء وزليج", Icons.Rounded.Build, 160, 350, 220, Color(0xFF5D6D7E)),
        ServiceCategory("cleaning", "تنظيف", Icons.Rounded.CleaningServices, 90, 200, 130, Color(0xFF16A085))
    )

    fun byId(id: String): ServiceCategory = all.firstOrNull { it.id == id } ?: all.first()
}

enum class CraftsmanSortOption(val labelAr: String) {
    NEAREST("الأقرب"),
    TOP_RATED("الأعلى تقييماً"),
    LOWEST_PRICE("الأقل سعراً")
}

enum class OfferSortOption(val labelAr: String) {
    BEST_PRICE("الأقل سعراً"),
    FASTEST_ETA("الأسرع وصولاً"),
    TOP_RATED("الأعلى تقييماً")
}

data class PortfolioItem(
    val id: String = "",
    val craftsmanId: String = "",
    val title: String = "",
    val description: String = "",
    val categoryName: String = "",
    val photoUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class CraftsmanPin(
    val id: String,
    val name: String,
    val specialtyId: String,
    val specialtyName: String,
    val rating: Double,
    val completedJobs: Int,
    val lat: Double,
    val lng: Double,
    val basePriceDh: Int,
    val distanceKm: Double,
    val etaMinutes: Int,
    val phone: String,
    val isVerified: Boolean = true,
    val experienceYears: Int = 6,
    val bio: String = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
    val avatarUri: String? = null,
    val portfolio: List<PortfolioItem> = emptyList(),
    val isLiveCloud: Boolean = false,
    val city: String = ""
)

data class LiveRouteState(
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val craftsmanLat: Double? = null,
    val craftsmanLng: Double? = null,
    val remainingDistanceKm: Double = 0.0,
    val etaMinutes: Int = 0
)

data class ServiceRequest(
    val id: String,
    val clientId: String,
    val clientName: String,
    val categoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val addressLabel: String,
    val lat: Double,
    val lng: Double,
    val mapX: Float,
    val mapY: Float,
    val proposedPrice: Int,
    val acceptedPrice: Int?,
    val paymentMethod: PaymentMethod,
    val isUrgent: Boolean,
    val status: RequestStatus,
    val assignedCraftsmanId: String?,
    val assignedCraftsmanName: String?,
    val assignedCraftsmanPhone: String?,
    val assignedCraftsmanRating: Double?,
    val photoUri: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class ServiceOffer(
    val id: String,
    val requestId: String,
    val craftsmanId: String,
    val craftsmanName: String,
    val craftsmanPhone: String,
    val craftsmanRating: Double,
    val completedJobs: Int,
    val specialty: String,
    val offeredPrice: Int,
    val etaMinutes: Int,
    val distanceKm: Double,
    val note: String,
    val status: OfferStatus,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

data class ChatMessage(
    val id: String,
    val requestId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val text: String,
    val createdAtMillis: Long
)

data class ReviewRecord(
    val id: String,
    val requestId: String,
    val craftsmanId: String,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
    val createdAtMillis: Long
)

enum class IdentityVerificationStatus(val dbValue: String, val labelAr: String) {
    NOT_SUBMITTED("not_submitted", "غير موثّق بعد"),
    PENDING_REVIEW("pending_review", "قيد المراجعة الإدارية"),
    VERIFIED("verified", "هوية موثّقة"),
    REJECTED("rejected", "مرفوض — يرجى التحديث");

    companion object {
        fun fromDb(value: String?): IdentityVerificationStatus =
            entries.firstOrNull { it.dbValue == value } ?: NOT_SUBMITTED
    }
}

object IdentityVerificationValidator {
    private val moroccanCinRegex = Regex("^[A-Za-z]{1,2}[0-9]{4,7}$")

    fun normalizeCin(input: String): String =
        input.trim().uppercase().replace(" ", "").replace("-", "")

    fun isValidCin(input: String): Boolean {
        val normalized = normalizeCin(input)
        return normalized.length in 5..10 && (moroccanCinRegex.matches(normalized) || normalized.length >= 6)
    }

    fun maskCin(input: String): String {
        val normalized = normalizeCin(input)
        if (normalized.length <= 4) return normalized
        return normalized.take(2) + "••••" + normalized.takeLast(2)
    }

    fun completionStepsCount(
        cin: String,
        frontUri: String?,
        backUri: String?,
        selfieUri: String?
    ): Int {
        var count = 0
        if (isValidCin(cin)) count++
        if (!frontUri.isNullOrBlank()) count++
        if (!backUri.isNullOrBlank()) count++
        if (!selfieUri.isNullOrBlank()) count++
        return count
    }
}

object LoginValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun normalizeEmail(input: String): String = input.trim().lowercase()

    fun isValidEmail(input: String): Boolean = emailRegex.matches(normalizeEmail(input))

    fun isValidFullName(input: String): Boolean = input.trim().length >= 2

    fun isValidPassword(input: String): Boolean = input.length >= 6

    fun hashPassword(email: String, password: String): String {
        val raw = "${normalizeEmail(email)}::$password"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

object PhoneValidator {
    fun normalize(input: String): String {
        val stripped = input.trim().replace(" ", "").replace("-", "")
        return if (stripped.startsWith("0") && stripped.length == 10) {
            "+212" + stripped.drop(1)
        } else {
            stripped
        }
    }

    fun isValidMoroccanPhone(input: String): Boolean {
        val normalized = normalize(input)
        return normalized.matches(Regex("^(\\+212|212)?[5-7][0-9]{8}$")) ||
            normalized.matches(Regex("^\\+?[0-9]{8,15}$"))
    }
}

data class UserSession(
    val userId: String = "local_user",
    val fullName: String = "يونس العلمي",
    val email: String = "",
    val phone: String = "+212 612-345678",
    val city: String = "الدار البيضاء",
    val role: UserRole = UserRole.CLIENT,
    val isRoleLocked: Boolean = false,
    val craftsmanSpecialtyId: String = "plumbing",
    val isCraftsmanSpecialtyLocked: Boolean = false,
    val isCraftsmanOnline: Boolean = true,
    val authProvider: String = "email",
    val avatarUri: String? = null,
    val bio: String = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
    val experienceYears: Int = 5,
    val basePriceDh: Int = 120,
    val verificationStatus: IdentityVerificationStatus = IdentityVerificationStatus.NOT_SUBMITTED,
    val nationalIdNumber: String = "",
    val idFrontPhotoUri: String? = null,
    val idBackPhotoUri: String? = null,
    val selfiePhotoUri: String? = null,
    val verificationSubmittedAtMillis: Long? = null,
    val rejectionReason: String = "",
    val fcmToken: String = "",
    val isSuspended: Boolean = false,
    val supabaseEmail: String? = null,
    val supabaseAccessToken: String? = null,
    val supabaseRefreshToken: String? = null,
    val supabaseExpiresAtMillis: Long = 0L
) {
    val isSupabaseAuthenticated: Boolean
        get() = !supabaseAccessToken.isNullOrBlank() && userId.isNotBlank() && userId != "local_user"

    val isCraftsmanVerified: Boolean
        get() = role == UserRole.CRAFTSMAN && verificationStatus == IdentityVerificationStatus.VERIFIED

    val isAdmin: Boolean
        get() = role == UserRole.ADMIN
}

object AccountRolePolicy {
    fun canSwitchRole(session: UserSession, targetRole: UserRole): Boolean {
        if (!session.isRoleLocked) return true
        return session.role == targetRole
    }

    fun canAccessMode(session: UserSession, mode: UserRole): Boolean {
        if (!session.isRoleLocked) return false
        return session.role == mode
    }

    fun canAccessIdentityVerification(session: UserSession): Boolean {
        return session.isRoleLocked && session.role == UserRole.CRAFTSMAN
    }

    fun canAccessAdminDashboard(session: UserSession): Boolean {
        return session.isRoleLocked && session.role == UserRole.ADMIN
    }
}

object AdminAuthorizationPolicy {
    const val BOOTSTRAP_ADMIN_EMAIL = "youness065220@gmail.com"
}

data class AppRuntimeConfig(
    val productionMode: Boolean = false,
    val updatedByEmail: String = "",
    val updatedAtMillis: Long = 0L
)

data class AuthorizedAdminRecord(
    val email: String,
    val addedBy: String,
    val createdAtMillis: Long
)

data class VerificationSubmission(
    val craftsmanId: String,
    val craftsmanName: String,
    val specialtyId: String,
    val city: String,
    val nationalIdNumber: String,
    val frontPhotoUri: String,
    val backPhotoUri: String,
    val selfiePhotoUri: String,
    val status: IdentityVerificationStatus,
    val rejectionReason: String = "",
    val reviewedByEmail: String = "",
    val submittedAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class AdminUserRecord(
    val userId: String,
    val fullName: String,
    val email: String,
    val city: String,
    val role: UserRole,
    val specialtyId: String,
    val verificationStatus: IdentityVerificationStatus,
    val isSuspended: Boolean,
    val fcmTokenPresent: Boolean,
    val createdAtMillis: Long
)

data class CityCoord(
    val nameAr: String,
    val lat: Double,
    val lng: Double
)

data class CityNeighborhood(
    val nameAr: String,
    val lat: Double,
    val lng: Double
)

data class SearchPlaceResult(
    val displayName: String,
    val lat: Double,
    val lng: Double
)

object MoroccanLocations {
    val cityCoords = listOf(
        CityCoord("الدار البيضاء", 33.5822, -7.6324),
        CityCoord("الرباط", 34.0209, -6.8416),
        CityCoord("مراكش", 31.6340, -8.0103),
        CityCoord("طنجة", 35.7595, -5.8340),
        CityCoord("فاس", 34.0181, -5.0078),
        CityCoord("أكادير", 30.4278, -9.5981)
    )

    val cities: List<String> = cityCoords.map { it.nameAr }

    fun coordForCity(city: String): CityCoord =
        cityCoords.firstOrNull { it.nameAr == city } ?: cityCoords.first()

    val neighborhoods = listOf(
        CityNeighborhood("المعاريف، شارع الزرقطوني، الدار البيضاء", 33.5822, -7.6324),
        CityNeighborhood("أنفا، قرب موروكو مول، الدار البيضاء", 33.5898, -7.6852),
        CityNeighborhood("سيدي معروف، التكنوبارك، الدار البيضاء", 33.5398, -7.6481),
        CityNeighborhood("عين السبع، الدار البيضاء", 33.6061, -7.5419),
        CityNeighborhood("أكدال، الرباط", 34.0011, -6.8477),
        CityNeighborhood("حي حسان، الرباط", 34.0209, -6.8310),
        CityNeighborhood("جليز، مراكش", 31.6340, -8.0103),
        CityNeighborhood("مالاباطا، طنجة", 35.7745, -5.7768)
    )
}
