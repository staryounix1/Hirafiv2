package com.example.data.model

/**
 * Market fairness classification for a proposed job price or counter-offer.
 */
enum class FairPriceAssessment(val labelAr: String, val hintAr: String) {
    LOW("سعر منخفض", "قد يستغرق قبول العرض وقتاً أطول"),
    FAIR("سعر مناسب للسوق", "فرصة قبول سريعة من الحرفيين"),
    GENEROUS("سعر تنافسي ممتاز", "أولوية عالية وجذب أسرع للحرفيين")
}

/**
 * Structured price proposal submitted by the client when creating or adjusting a job request.
 */
data class JobPriceProposal(
    val requestId: String? = null,
    val categoryId: String,
    val proposedPriceDh: Int,
    val recommendedMinDh: Int,
    val recommendedMaxDh: Int,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isUrgent: Boolean = false,
    val minAllowedDh: Int = DEFAULT_MIN_PRICE_DH,
    val maxAllowedDh: Int = DEFAULT_MAX_PRICE_DH
) {
    fun assessFairness(): FairPriceAssessment = when {
        proposedPriceDh < recommendedMinDh -> FairPriceAssessment.LOW
        proposedPriceDh > recommendedMaxDh -> FairPriceAssessment.GENEROUS
        else -> FairPriceAssessment.FAIR
    }

    fun clampedPrice(candidate: Int): Int = candidate.coerceIn(minAllowedDh, maxAllowedDh)

    companion object {
        const val DEFAULT_MIN_PRICE_DH = 40
        const val DEFAULT_MAX_PRICE_DH = 5000

        fun fromRequest(request: ServiceRequest): JobPriceProposal {
            val category = HirafiCategories.byId(request.categoryId)
            return JobPriceProposal(
                requestId = request.id,
                categoryId = request.categoryId,
                proposedPriceDh = request.proposedPrice,
                recommendedMinDh = category.minRecommendedDh,
                recommendedMaxDh = category.maxRecommendedDh,
                paymentMethod = request.paymentMethod,
                isUrgent = request.isUrgent
            )
        }
    }
}

/**
 * Editable draft state for a craftsman preparing a bid or counter-offer in real time.
 */
data class CraftsmanBidDraft(
    val requestId: String? = null,
    val offeredPrice: Int = 120,
    val etaMinutes: Int = 10,
    val note: String = "",
    val isSubmitting: Boolean = false
)

/**
 * Represents a structured counter-offer submission from either the client or the craftsman.
 */
data class CounterOfferSubmission(
    val requestId: String,
    val targetOfferId: String,
    val fromRole: UserRole,
    val proposedPriceDh: Int,
    val etaMinutes: Int = 10,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Live bidding room metrics exposed to the client.
 * For craftsmen, competitor metrics are intentionally withheld (all offer stats stay empty/zero).
 */
data class BiddingSessionSummary(
    val activeOfferCount: Int = 0,
    val lowestPriceDh: Int? = null,
    val fastestEtaMinutes: Int? = null,
    val highestRating: Double? = null,
    val clientProposedPriceDh: Int = 0
) {
    companion object {
        fun fromOffers(
            role: UserRole,
            offers: List<ServiceOffer>,
            clientProposedPriceDh: Int
        ): BiddingSessionSummary {
            if (role == UserRole.CRAFTSMAN) {
                // Enforce single-craftsman privacy: never leak competitor bid count or prices
                return BiddingSessionSummary(
                    activeOfferCount = 0,
                    lowestPriceDh = null,
                    fastestEtaMinutes = null,
                    highestRating = null,
                    clientProposedPriceDh = clientProposedPriceDh
                )
            }
            return BiddingSessionSummary(
                activeOfferCount = offers.size,
                lowestPriceDh = offers.minOfOrNull { it.offeredPrice },
                fastestEtaMinutes = offers.minOfOrNull { it.etaMinutes },
                highestRating = offers.maxOfOrNull { it.craftsmanRating },
                clientProposedPriceDh = clientProposedPriceDh
            )
        }
    }
}
