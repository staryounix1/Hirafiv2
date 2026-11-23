package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey val id: String,
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
    val paymentMethod: String,
    val isUrgent: Boolean,
    val status: String,
    val assignedCraftsmanId: String?,
    val assignedCraftsmanName: String?,
    val assignedCraftsmanPhone: String?,
    val assignedCraftsmanRating: Double?,
    val photoUri: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "service_offers")
data class ServiceOfferEntity(
    @PrimaryKey val id: String,
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
    val status: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val createdAtMillis: Long
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val craftsmanId: String,
    val rating: Int,
    val tagsJoined: String,
    val comment: String,
    val createdAtMillis: Long
)

@Entity(tableName = "portfolio_items")
data class PortfolioItemEntity(
    @PrimaryKey val id: String,
    val craftsmanId: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val photoUri: String?,
    val createdAtMillis: Long
)
