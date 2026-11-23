package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    // ---- Auth (GoTrue) ----

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithIdToken(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseIdTokenAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=pkce")
    suspend fun exchangePkceCode(
        @Header("apikey") apiKey: String,
        @Body body: SupabasePkceTokenRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseRefreshTokenRequest
    ): Response<SupabaseAuthResponse>

    @GET("auth/v1/user")
    suspend fun getAuthUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String
    ): Response<SupabaseAuthUserDto>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String
    ): Response<Unit>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseRecoverRequest
    ): Response<Unit>

    // ---- Profiles ----

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfilesByFilter(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String? = null,
        @Query("role") roleFilter: String? = null,
        @Query("is_online") onlineFilter: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/craftsman_discovery_view?select=id,full_name,city,role,specialty_id,specialty,avatar_uri,bio,experience_years,base_price_dh,rating,completed_jobs,is_online,is_verified,verification_status,is_suspended,lat,lng,created_at")
    suspend fun getCraftsmanDiscoveryPins(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String? = null,
        @Query("is_online") onlineFilter: String? = "eq.true",
        @Query("order") order: String = "rating.desc"
    ): Response<List<SupabaseProfileDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body profile: SupabaseProfileDto
    ): Response<List<SupabaseProfileDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/profiles")
    suspend fun patchProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseProfileUpdatePatchDto
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/profiles")
    suspend fun patchProfileAsAdmin(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseAdminProfilePatchDto
    ): Response<Unit>

    @GET("rest/v1/profiles?select=id,lat,lng")
    suspend fun getProfileLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String
    ): Response<List<SupabaseProfileLocationDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/profiles")
    suspend fun patchProfileLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseProfileLocationPatchDto
    ): Response<Unit>

    // ---- App Config & Authorized Admins ----

    @GET("rest/v1/app_config?select=*")
    suspend fun getAppConfig(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String = "eq.runtime"
    ): Response<List<SupabaseAppConfigDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/app_config")
    suspend fun upsertAppConfig(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body config: SupabaseAppConfigDto
    ): Response<Unit>

    @GET("rest/v1/authorized_admins?select=*&order=email.asc")
    suspend fun getAuthorizedAdmins(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("email") emailFilter: String? = null
    ): Response<List<SupabaseAuthorizedAdminDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/authorized_admins")
    suspend fun upsertAuthorizedAdmin(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseAuthorizedAdminDto
    ): Response<Unit>

    @DELETE("rest/v1/authorized_admins")
    suspend fun deleteAuthorizedAdmin(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("email") emailFilter: String
    ): Response<Unit>

    // ---- KYC Verification Submissions ----

    @GET("rest/v1/verification_submissions?select=*&order=updated_at.desc")
    suspend fun getVerificationSubmissions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("craftsman_id") craftsmanIdFilter: String? = null
    ): Response<List<SupabaseVerificationDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/verification_submissions")
    suspend fun upsertVerificationSubmission(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseVerificationDto
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/verification_submissions")
    suspend fun patchVerificationSubmission(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("craftsman_id") craftsmanIdFilter: String,
        @Body patch: SupabaseVerificationReviewPatchDto
    ): Response<Unit>

    // ---- Portfolio Items ----

    @GET("rest/v1/portfolio_items?select=*&order=created_at.desc")
    suspend fun getPortfolioItems(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("craftsman_id") craftsmanIdFilter: String
    ): Response<List<SupabasePortfolioItemDto>>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/portfolio_items")
    suspend fun createPortfolioItem(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabasePortfolioItemDto
    ): Response<Unit>

    @DELETE("rest/v1/portfolio_items")
    suspend fun deletePortfolioItem(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // ---- Service Requests ----

    @GET("rest/v1/service_requests?select=*&order=created_at.desc")
    suspend fun getServiceRequests(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String? = null,
        @Query("client_id") clientIdFilter: String? = null,
        @Query("assigned_craftsman_id") assignedCraftsmanIdFilter: String? = null,
        @Query("status") statusFilter: String? = null,
        @Query("category_id") categoryIdFilter: String? = null
    ): Response<List<SupabaseServiceRequestDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/service_requests")
    suspend fun createServiceRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseServiceRequestDto
    ): Response<List<SupabaseServiceRequestDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/service_requests")
    suspend fun patchServiceRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseRequestPatchDto
    ): Response<Unit>

    // ---- Service Offers ----

    @GET("rest/v1/service_offers?select=*&order=created_at.desc")
    suspend fun getOffersForRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("request_id") requestIdFilter: String? = null,
        @Query("craftsman_id") craftsmanIdFilter: String? = null
    ): Response<List<SupabaseServiceOfferDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/service_offers")
    suspend fun createOffer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseServiceOfferDto
    ): Response<List<SupabaseServiceOfferDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/service_offers")
    suspend fun patchOffer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseOfferPatchDto
    ): Response<Unit>

    // ---- Chat Messages ----

    @GET("rest/v1/chat_messages?select=*&order=created_at.asc")
    suspend fun getChatMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("request_id") requestIdFilter: String
    ): Response<List<SupabaseChatMessageDto>>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/chat_messages")
    suspend fun postChatMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseChatMessageDto
    ): Response<Unit>

    // ---- Reviews ----

    @GET("rest/v1/reviews?select=*&order=created_at.desc")
    suspend fun getReviews(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("client_id") clientIdFilter: String? = null,
        @Query("craftsman_id") craftsmanIdFilter: String? = null
    ): Response<List<SupabaseReviewDto>>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/reviews")
    suspend fun postReview(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseReviewDto
    ): Response<Unit>
}
