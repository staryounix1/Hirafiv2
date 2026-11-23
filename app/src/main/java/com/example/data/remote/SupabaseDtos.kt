package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseIdTokenAuthRequest(
    val provider: String = "google",
    @param:Json(name = "id_token") val idToken: String,
    val nonce: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabasePkceTokenRequest(
    @param:Json(name = "auth_code") val authCode: String,
    @param:Json(name = "code_verifier") val codeVerifier: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @param:Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRecoverRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUserDto(
    val id: String? = null,
    val email: String? = null,
    @param:Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @param:Json(name = "access_token") val accessToken: String? = null,
    @param:Json(name = "refresh_token") val refreshToken: String? = null,
    @param:Json(name = "expires_in") val expiresIn: Long? = null,
    @param:Json(name = "token_type") val tokenType: String? = null,
    @param:Json(name = "user") val user: SupabaseAuthUserDto? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
    val id: String,
    @param:Json(name = "full_name") val fullName: String = "",
    val email: String = "",
    val phone: String = "+212 612-345678",
    val city: String = "الدار البيضاء",
    val role: String = "client",
    @param:Json(name = "role_locked") val roleLocked: Boolean = false,
    @param:Json(name = "specialty_id") val specialtyId: String = "plumbing",
    val specialty: String = "سباكة",
    @param:Json(name = "avatar_uri") val avatarUri: String? = null,
    val bio: String = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
    @param:Json(name = "experience_years") val experienceYears: Int = 5,
    @param:Json(name = "base_price_dh") val basePriceDh: Int = 120,
    val rating: Double = 4.9,
    @param:Json(name = "completed_jobs") val completedJobs: Int = 0,
    @param:Json(name = "is_online") val isOnline: Boolean = false,
    @param:Json(name = "is_verified") val isVerified: Boolean = false,
    @param:Json(name = "verification_status") val verificationStatus: String = "not_submitted",
    @param:Json(name = "rejection_reason") val rejectionReason: String = "",
    @param:Json(name = "is_suspended") val isSuspended: Boolean = false,
    @param:Json(name = "fcm_token") val fcmToken: String = "",
    val lat: Double = 33.5822,
    val lng: Double = -7.6324,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileUpdatePatchDto(
    @param:Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val city: String? = null,
    @param:Json(name = "avatar_uri") val avatarUri: String? = null,
    val bio: String? = null,
    @param:Json(name = "experience_years") val experienceYears: Int? = null,
    @param:Json(name = "base_price_dh") val basePriceDh: Int? = null,
    @param:Json(name = "specialty_id") val specialtyId: String? = null,
    val specialty: String? = null,
    @param:Json(name = "is_online") val isOnline: Boolean? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val role: String? = null,
    @param:Json(name = "fcm_token") val fcmToken: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileLocationDto(
    val id: String,
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileLocationPatchDto(
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseAdminProfilePatchDto(
    @param:Json(name = "is_suspended") val isSuspended: Boolean? = null,
    @param:Json(name = "is_online") val isOnline: Boolean? = null,
    @param:Json(name = "is_verified") val isVerified: Boolean? = null,
    @param:Json(name = "verification_status") val verificationStatus: String? = null,
    @param:Json(name = "rejection_reason") val rejectionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAppConfigDto(
    val id: String = "runtime",
    @param:Json(name = "production_mode") val productionMode: Boolean = false,
    @param:Json(name = "updated_by_email") val updatedByEmail: String = "",
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthorizedAdminDto(
    val email: String,
    @param:Json(name = "added_by") val addedBy: String = "",
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseVerificationDto(
    @param:Json(name = "craftsman_id") val craftsmanId: String,
    @param:Json(name = "craftsman_name") val craftsmanName: String,
    @param:Json(name = "specialty_id") val specialtyId: String = "plumbing",
    val city: String = "الدار البيضاء",
    @param:Json(name = "national_id_number") val nationalIdNumber: String = "",
    @param:Json(name = "front_photo_uri") val frontPhotoUri: String = "",
    @param:Json(name = "back_photo_uri") val backPhotoUri: String = "",
    @param:Json(name = "selfie_photo_uri") val selfiePhotoUri: String = "",
    val status: String = "pending_review",
    @param:Json(name = "rejection_reason") val rejectionReason: String = "",
    @param:Json(name = "reviewed_by_email") val reviewedByEmail: String = "",
    @param:Json(name = "submitted_at") val submittedAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseVerificationReviewPatchDto(
    val status: String,
    @param:Json(name = "rejection_reason") val rejectionReason: String,
    @param:Json(name = "reviewed_by_email") val reviewedByEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabasePortfolioItemDto(
    val id: String,
    @param:Json(name = "craftsman_id") val craftsmanId: String,
    val title: String,
    val description: String = "",
    @param:Json(name = "category_name") val categoryName: String = "حرفي",
    @param:Json(name = "photo_uri") val photoUri: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseServiceRequestDto(
    val id: String,
    @param:Json(name = "client_id") val clientId: String? = null,
    @param:Json(name = "client_name") val clientName: String,
    @param:Json(name = "category_id") val categoryId: String,
    @param:Json(name = "category_name") val categoryName: String,
    val title: String,
    val description: String = "",
    @param:Json(name = "address_label") val addressLabel: String,
    val lat: Double,
    val lng: Double,
    @param:Json(name = "proposed_price") val proposedPrice: Int,
    @param:Json(name = "accepted_price") val acceptedPrice: Int? = null,
    @param:Json(name = "payment_method") val paymentMethod: String = "cash",
    @param:Json(name = "is_urgent") val isUrgent: Boolean = true,
    val status: String = "searching",
    @param:Json(name = "assigned_craftsman_id") val assignedCraftsmanId: String? = null,
    @param:Json(name = "assigned_craftsman_name") val assignedCraftsmanName: String? = null,
    @param:Json(name = "assigned_craftsman_phone") val assignedCraftsmanPhone: String? = null,
    @param:Json(name = "assigned_craftsman_rating") val assignedCraftsmanRating: Double? = null,
    @param:Json(name = "craftsman_lat") val craftsmanLat: Double? = null,
    @param:Json(name = "craftsman_lng") val craftsmanLng: Double? = null,
    @param:Json(name = "photo_uri") val photoUri: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseRequestPatchDto(
    @param:Json(name = "proposed_price") val proposedPrice: Int? = null,
    @param:Json(name = "accepted_price") val acceptedPrice: Int? = null,
    val status: String? = null,
    @param:Json(name = "assigned_craftsman_id") val assignedCraftsmanId: String? = null,
    @param:Json(name = "assigned_craftsman_name") val assignedCraftsmanName: String? = null,
    @param:Json(name = "assigned_craftsman_phone") val assignedCraftsmanPhone: String? = null,
    @param:Json(name = "assigned_craftsman_rating") val assignedCraftsmanRating: Double? = null,
    @param:Json(name = "craftsman_lat") val craftsmanLat: Double? = null,
    @param:Json(name = "craftsman_lng") val craftsmanLng: Double? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseServiceOfferDto(
    val id: String,
    @param:Json(name = "request_id") val requestId: String,
    @param:Json(name = "client_id") val clientId: String? = null,
    @param:Json(name = "craftsman_id") val craftsmanId: String? = null,
    @param:Json(name = "craftsman_name") val craftsmanName: String,
    @param:Json(name = "craftsman_phone") val craftsmanPhone: String,
    @param:Json(name = "craftsman_rating") val craftsmanRating: Double,
    @param:Json(name = "completed_jobs") val completedJobs: Int,
    val specialty: String,
    @param:Json(name = "offered_price") val offeredPrice: Int,
    @param:Json(name = "eta_minutes") val etaMinutes: Int,
    @param:Json(name = "distance_km") val distanceKm: Double,
    val note: String = "",
    val status: String = "pending",
    @param:Json(name = "expires_at_millis") val expiresAtMillis: Long = 0L,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseOfferPatchDto(
    val status: String,
    @param:Json(name = "offered_price") val offeredPrice: Int? = null,
    val note: String? = null,
    @param:Json(name = "expires_at_millis") val expiresAtMillis: Long? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseChatMessageDto(
    val id: String,
    @param:Json(name = "request_id") val requestId: String,
    @param:Json(name = "sender_id") val senderId: String,
    @param:Json(name = "sender_name") val senderName: String,
    @param:Json(name = "sender_role") val senderRole: String,
    @param:Json(name = "message_text") val messageText: String,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReviewDto(
    val id: String,
    @param:Json(name = "request_id") val requestId: String,
    @param:Json(name = "craftsman_id") val craftsmanId: String? = null,
    @param:Json(name = "client_id") val clientId: String? = null,
    val rating: Int,
    val tags: List<String> = emptyList(),
    val comment: String = "",
    @param:Json(name = "created_at") val createdAt: String? = null
)
