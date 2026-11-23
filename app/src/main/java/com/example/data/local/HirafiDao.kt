package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HirafiDao {

    @Query("SELECT * FROM service_requests ORDER BY createdAtMillis DESC")
    fun observeAllRequests(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): ServiceRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequest(request: ServiceRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequests(requests: List<ServiceRequestEntity>)

    @Query("DELETE FROM service_requests WHERE id = :requestId")
    suspend fun deleteRequestById(requestId: String)

    @Query("DELETE FROM service_requests WHERE status = 'searching' AND categoryId = :categoryId")
    suspend fun deleteSearchingRequestsByCategory(categoryId: String)

    @Transaction
    suspend fun replaceSearchingRequestsForCategory(
        categoryId: String,
        requests: List<ServiceRequestEntity>
    ) {
        deleteSearchingRequestsByCategory(categoryId)
        if (requests.isNotEmpty()) {
            upsertRequests(requests)
        }
    }

    @Query("DELETE FROM service_requests WHERE clientId = :clientId")
    suspend fun deleteRequestsByClient(clientId: String)

    @Transaction
    suspend fun replaceRequestsForClient(
        clientId: String,
        requests: List<ServiceRequestEntity>
    ) {
        deleteRequestsByClient(clientId)
        if (requests.isNotEmpty()) {
            upsertRequests(requests)
        }
    }

    @Query("SELECT * FROM service_offers WHERE requestId = :requestId ORDER BY createdAtMillis DESC")
    fun observeOffersForRequest(requestId: String): Flow<List<ServiceOfferEntity>>

    @Query("SELECT * FROM service_offers WHERE craftsmanId = :craftsmanId ORDER BY createdAtMillis DESC")
    fun observeOffersByCraftsman(craftsmanId: String): Flow<List<ServiceOfferEntity>>

    @Query("SELECT * FROM service_offers WHERE id = :offerId LIMIT 1")
    suspend fun getOfferById(offerId: String): ServiceOfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offer: ServiceOfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffers(offers: List<ServiceOfferEntity>)

    @Query("DELETE FROM service_offers WHERE id = :offerId")
    suspend fun deleteOfferById(offerId: String)

    @Query("UPDATE service_offers SET status = :newStatus WHERE requestId = :requestId AND id != :acceptedOfferId AND status = 'pending'")
    suspend fun declineOtherOffers(requestId: String, acceptedOfferId: String, newStatus: String = "declined")

    @Query("SELECT * FROM chat_messages WHERE requestId = :requestId ORDER BY createdAtMillis ASC")
    fun observeMessagesForRequest(requestId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM reviews ORDER BY createdAtMillis DESC")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Query("SELECT * FROM portfolio_items WHERE craftsmanId = :craftsmanId ORDER BY createdAtMillis DESC")
    fun observePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPortfolioItem(item: PortfolioItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPortfolioItems(items: List<PortfolioItemEntity>)

    @Query("DELETE FROM portfolio_items WHERE id = :itemId")
    suspend fun deletePortfolioItem(itemId: String)

    @Query("DELETE FROM service_requests WHERE id LIKE 'req_seed_%'")
    suspend fun clearSeededRequests()

    @Query("DELETE FROM service_requests")
    suspend fun clearAllRequests()

    @Query("DELETE FROM service_offers")
    suspend fun clearAllOffers()

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Transaction
    suspend fun clearMarketplaceCache() {
        clearAllMessages()
        clearAllOffers()
        clearAllRequests()
    }
}
