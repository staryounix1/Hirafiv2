package com.example

import androidx.compose.ui.unit.LayoutDirection
import com.example.data.location.RealTimeLocationService
import com.example.data.model.AcceptanceRadarMetrics
import com.example.data.model.AccountRolePolicy
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.BiddingSessionSummary
import com.example.data.model.ChatMessage
import com.example.data.model.CounterOfferSubmission
import com.example.data.model.CraftsmanPin
import com.example.data.model.CraftsmanSortOption
import com.example.data.model.FairPriceAssessment
import com.example.data.model.HirafiCategories
import com.example.data.model.HirafiQuickScenarios
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.IdentityVerificationValidator
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.JobPriceProposal
import com.example.data.model.LoginValidator
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PhoneValidator
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.remote.GoogleIdTokenFallbackToBrowserException
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseRecoverRequest
import com.example.data.remote.SupabaseSessionManager
import com.example.data.remote.SupabaseTimestampParser
import com.example.data.repository.HirafiRepository
import com.example.ui.components.CraftsmanRequestFilter
import com.example.ui.components.HirafiProAnalytics
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.HirafiI18n
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.screens.AdminDashboardAnalytics
import com.example.ui.viewmodel.HirafiViewModel
import com.example.ui.viewmodel.RealtimeBiddingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun `haversine distance between Casablanca neighborhoods is realistic`() {
    val km = MapRoutingService.haversineKm(33.5883, -7.6328, 33.5950, -7.6187)
    assertTrue("Expected positive short distance, got $km", km in 1.0..3.0)
  }

  @Test
  fun `moroccan city coordinate lookup resolves Marrakech and falls back to Casablanca`() {
    val marrakech = MoroccanLocations.coordForCity("مراكش")
    assertEquals(31.6340, marrakech.lat, 0.01)
    val unknown = MoroccanLocations.coordForCity("مدينة غير معروفة")
    assertEquals("الدار البيضاء", unknown.nameAr)
  }

  @Test
  fun `nearby craftsmen cover all 8 categories with valid distances and ETAs`() {
    val craftsmen = HirafiRepository.computeCraftsmenAround(33.5822, -7.6324)
    assertEquals(16, craftsmen.size)
    val coveredCategories = craftsmen.map { it.specialtyId }.toSet()
    HirafiCategories.all.forEach { category ->
      assertTrue("Missing craftsman for category ${category.id}", category.id in coveredCategories)
    }
    craftsmen.forEach { pin ->
      assertTrue("Distance must be positive for ${pin.id}", pin.distanceKm > 0.0)
      assertTrue("ETA must be positive for ${pin.id}", pin.etaMinutes > 0)
      assertTrue("Bio must not be blank for ${pin.id}", pin.bio.isNotBlank())
    }
  }

  @Test
  fun `realtime bidding sorts active offers and excludes expired or declined bids`() {
    val now = 100_000L
    val offers = listOf(
      ServiceOffer("o1", "r1", "c1", "كريم", "0600", 4.7, 50, "سباكة", 140, 8, 1.2, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o2", "r1", "c2", "عمر", "0601", 4.95, 120, "سباكة", 110, 15, 2.5, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o3", "r1", "c3", "رشيد", "0602", 4.8, 90, "سباكة", 125, 12, 1.8, "", OfferStatus.COUNTERED, now - 1000, now - 500),
      ServiceOffer("o4", "r1", "c4", "سليم", "0603", 5.0, 200, "سباكة", 90, 5, 0.5, "", OfferStatus.DECLINED, now - 1000, now + 30_000),
      ServiceOffer("o5", "r1", "c5", "مراد", "0604", 4.6, 30, "سباكة", 95, 6, 0.9, "", OfferStatus.PENDING, now - 5000, now - 100)
    )

    val byPrice = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.BEST_PRICE, now)
    assertEquals(listOf("o2", "o3", "o1"), byPrice.map { it.id })

    val byEta = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.FASTEST_ETA, now)
    assertEquals(listOf("o1", "o3", "o2"), byEta.map { it.id })

    val byRating = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.TOP_RATED, now)
    assertEquals(listOf("o2", "o3", "o1"), byRating.map { it.id })
  }

  @Test
  fun `craftsman only sees open requests matching their single locked profession and only their own offers`() {
    val requests = listOf(
      ServiceRequest(
        id = "r_plumb",
        clientId = "u1",
        clientName = "ياسين",
        categoryId = "plumbing",
        categoryName = "السباكة",
        title = "تسرب مياه",
        description = "",
        addressLabel = "المعاريف",
        lat = 33.58,
        lng = -7.63,
        mapX = 0.5f,
        mapY = 0.5f,
        proposedPrice = 150,
        acceptedPrice = null,
        paymentMethod = PaymentMethod.CASH,
        isUrgent = false,
        status = RequestStatus.SEARCHING,
        assignedCraftsmanId = null,
        assignedCraftsmanName = null,
        assignedCraftsmanPhone = null,
        assignedCraftsmanRating = null,
        photoUri = null,
        createdAtMillis = 1000L,
        updatedAtMillis = 1000L
      ),
      ServiceRequest(
        id = "r_elec",
        clientId = "u2",
        clientName = "سلمى",
        categoryId = "electrical",
        categoryName = "الكهرباء",
        title = "انقطاع كهرباء",
        description = "",
        addressLabel = "أنفا",
        lat = 33.59,
        lng = -7.64,
        mapX = 0.5f,
        mapY = 0.5f,
        proposedPrice = 180,
        acceptedPrice = null,
        paymentMethod = PaymentMethod.CASH,
        isUrgent = false,
        status = RequestStatus.SEARCHING,
        assignedCraftsmanId = null,
        assignedCraftsmanName = null,
        assignedCraftsmanPhone = null,
        assignedCraftsmanRating = null,
        photoUri = null,
        createdAtMillis = 2000L,
        updatedAtMillis = 2000L
      )
    )
    val lockedSpecialtyId = "plumbing"
    val visibleToPlumber = requests.filter {
      it.status == RequestStatus.SEARCHING && it.categoryId == lockedSpecialtyId
    }
    assertEquals(listOf("r_plumb"), visibleToPlumber.map { it.id })

    val myCraftsmanId = "c_me"
    val allOffers = listOf(
      ServiceOffer("o_mine", "r_plumb", myCraftsmanId, "أنا", "0611", 4.9, 10, "سباك", 140, 6, 1.0, "", OfferStatus.PENDING, 1000L, 50_000L),
      ServiceOffer("o_other", "r_plumb", "c_competitor", "حرفي منافس", "0622", 4.8, 25, "سباك", 120, 4, 0.7, "", OfferStatus.PENDING, 1000L, 50_000L)
    )
    val visibleToCraftsman = allOffers.filter { it.craftsmanId == myCraftsmanId }
    assertEquals(listOf("o_mine"), visibleToCraftsman.map { it.id })
  }

  @Test
  fun `real time location service computes dynamic ETA and validates coordinates`() {
    assertTrue(RealTimeLocationService.isCoordinateValid(33.5822, -7.6324))
    assertFalse(RealTimeLocationService.isCoordinateValid(0.0, 0.0))
    assertFalse(RealTimeLocationService.isCoordinateValid(Double.NaN, -7.6))
    assertFalse(RealTimeLocationService.isCoordinateValid(95.0, 10.0))

    assertEquals(0, RealTimeLocationService.computeEtaMinutes(0.05, 5.0f))
    // 10 m/s = 36 km/h -> 6.0 km takes 10 minutes
    assertEquals(10, RealTimeLocationService.computeEtaMinutes(6.0, 10.0f))
    // Stationary (0 m/s) uses 24 km/h urban baseline -> 4.0 km takes 10 minutes
    assertEquals(10, RealTimeLocationService.computeEtaMinutes(4.0, 0.0f))
  }

  @Test
  fun `job price proposal evaluates market fairness and clamps counter offers`() {
    val low = JobPriceProposal(
      categoryId = "plumbing",
      proposedPriceDh = 70,
      recommendedMinDh = 100,
      recommendedMaxDh = 180
    )
    assertEquals(FairPriceAssessment.LOW, low.assessFairness())
    assertEquals(40, low.clampedPrice(10))
    assertEquals(5000, low.clampedPrice(9999))

    val fair = low.copy(proposedPriceDh = 140)
    assertEquals(FairPriceAssessment.FAIR, fair.assessFairness())

    val generous = low.copy(proposedPriceDh = 220)
    assertEquals(FairPriceAssessment.GENEROUS, generous.assessFairness())

    val counter = CounterOfferSubmission(
      requestId = "req_1",
      targetOfferId = "off_1",
      fromRole = UserRole.CRAFTSMAN,
      proposedPriceDh = 160,
      etaMinutes = 12,
      note = "شامل القطع"
    )
    assertEquals(160, counter.proposedPriceDh)
    assertEquals(UserRole.CRAFTSMAN, counter.fromRole)
  }

  @Test
  fun `bidding session summary exposes metrics to client and hides competitor bids from craftsman`() {
    val now = 200_000L
    val offers = listOf(
      ServiceOffer("o1", "r1", "c1", "كريم", "0600", 4.7, 50, "سباكة", 140, 8, 1.2, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o2", "r1", "c2", "عمر", "0601", 4.95, 120, "سباكة", 110, 15, 2.5, "", OfferStatus.PENDING, now - 1000, now + 30_000)
    )

    val clientSummary = BiddingSessionSummary.fromOffers(UserRole.CLIENT, offers, 120)
    assertEquals(2, clientSummary.activeOfferCount)
    assertEquals(110, clientSummary.lowestPriceDh)
    assertEquals(8, clientSummary.fastestEtaMinutes)
    assertEquals(4.95, clientSummary.highestRating!!, 0.001)

    val craftsmanSummary = BiddingSessionSummary.fromOffers(UserRole.CRAFTSMAN, offers, 120)
    assertEquals(0, craftsmanSummary.activeOfferCount)
    assertNull(craftsmanSummary.lowestPriceDh)
    assertNull(craftsmanSummary.fastestEtaMinutes)
    assertNull(craftsmanSummary.highestRating)
  }

  @Test
  fun `quick triage scenarios cover all categories and handshake security generates valid 4-digit pin and warranty`() {
    HirafiCategories.all.forEach { category ->
      val presets = HirafiQuickScenarios.forCategory(category.id)
      assertTrue("Expected presets for ${category.id}", presets.isNotEmpty())
      presets.forEach { preset ->
        assertTrue(preset.recommendedPriceDh >= 40)
        assertTrue(preset.estimatedMinutes > 0)
      }
    }

    val req = ServiceRequest(
      id = "req_security_test_1",
      clientId = "client_42",
      clientName = "أنس",
      categoryId = "plumbing",
      categoryName = "سباكة",
      title = "تسرب تحت الحوض",
      description = "",
      addressLabel = "المعاريف، الدار البيضاء",
      lat = 33.5822,
      lng = -7.6324,
      mapX = 0.5f,
      mapY = 0.5f,
      proposedPrice = 150,
      acceptedPrice = 150,
      paymentMethod = PaymentMethod.CASH,
      isUrgent = true,
      status = RequestStatus.COMPLETED,
      assignedCraftsmanId = "craft_7",
      assignedCraftsmanName = "كمال السباك",
      assignedCraftsmanPhone = "+212600112233",
      assignedCraftsmanRating = 4.9,
      photoUri = null,
      createdAtMillis = 1_700_000_000_000L,
      updatedAtMillis = 1_700_003_600_000L
    )

    val pin = JobHandshakeSecurity.deriveSecurityPin(req)
    assertEquals(4, pin.length)
    assertTrue(pin.all { it.isDigit() })
    assertEquals(pin, JobHandshakeSecurity.deriveSecurityPin(req))
    assertEquals(pin, JobHandshakeSecurity.deriveSecurityPin(req.copy(clientId = "")))

    val arabicDigits = mapOf(
      '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
      '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩'
    )
    val arabicPin = pin.map { arabicDigits[it] ?: it }.joinToString("")
    assertEquals(pin, JobHandshakeSecurity.normalizePinInput(arabicPin))
    assertTrue(JobHandshakeSecurity.verifySecurityPin(arabicPin, req))
    assertFalse(JobHandshakeSecurity.verifySecurityPin("000", req))

    val warranty = JobHandshakeSecurity.deriveWarrantyCode(req)
    assertTrue(warranty.startsWith("HRF-") && warranty.endsWith("-30D"))

    val shareText = JobHandshakeSecurity.buildWarrantyShareText(req)
    assertTrue(shareText.contains(warranty))
    assertTrue(shareText.contains("كمال السباك"))

    val radarHigh = AcceptanceRadarMetrics.compute(
      JobPriceProposal(categoryId = "plumbing", proposedPriceDh = 210, recommendedMinDh = 100, recommendedMaxDh = 180, isUrgent = true)
    )
    val radarLow = AcceptanceRadarMetrics.compute(
      JobPriceProposal(categoryId = "plumbing", proposedPriceDh = 60, recommendedMinDh = 100, recommendedMaxDh = 180, isUrgent = false)
    )
    assertTrue(radarHigh.probabilityPercent > radarLow.probabilityPercent)
  }

  @Test
  fun `permanent role lock blocks client from craftsman mode and craftsman from client mode`() {
    val unregistered = UserSession(role = UserRole.CLIENT, isRoleLocked = false)
    assertFalse(AccountRolePolicy.canAccessMode(unregistered, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canAccessMode(unregistered, UserRole.CRAFTSMAN))
    assertTrue(AccountRolePolicy.canSwitchRole(unregistered, UserRole.CRAFTSMAN))

    val lockedClient = UserSession(role = UserRole.CLIENT, isRoleLocked = true)
    assertTrue(AccountRolePolicy.canAccessMode(lockedClient, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canAccessMode(lockedClient, UserRole.CRAFTSMAN))
    assertFalse(AccountRolePolicy.canSwitchRole(lockedClient, UserRole.CRAFTSMAN))
    assertTrue(AccountRolePolicy.canSwitchRole(lockedClient, UserRole.CLIENT))

    val lockedCraftsman = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      craftsmanSpecialtyId = "electricity",
      isCraftsmanSpecialtyLocked = true
    )
    assertTrue(AccountRolePolicy.canAccessMode(lockedCraftsman, UserRole.CRAFTSMAN))
    assertFalse(AccountRolePolicy.canAccessMode(lockedCraftsman, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canSwitchRole(lockedCraftsman, UserRole.CLIENT))
    assertTrue(AccountRolePolicy.canSwitchRole(lockedCraftsman, UserRole.CRAFTSMAN))
  }

  @Test
  fun `identity verification is restricted to craftsmen and validates moroccan CIN steps`() {
    val client = UserSession(role = UserRole.CLIENT, isRoleLocked = true)
    val unregisteredCraftsman = UserSession(role = UserRole.CRAFTSMAN, isRoleLocked = false)
    val craftsman = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      craftsmanSpecialtyId = "plumbing",
      isCraftsmanSpecialtyLocked = true,
      verificationStatus = IdentityVerificationStatus.VERIFIED
    )

    assertFalse(AccountRolePolicy.canAccessIdentityVerification(client))
    assertFalse(AccountRolePolicy.canAccessIdentityVerification(unregisteredCraftsman))
    assertTrue(AccountRolePolicy.canAccessIdentityVerification(craftsman))
    assertTrue(craftsman.isCraftsmanVerified)
    assertFalse(client.isCraftsmanVerified)

    assertTrue(IdentityVerificationValidator.isValidCin("bk 654321"))
    assertFalse(IdentityVerificationValidator.isValidCin("12"))
    assertEquals("BK••••21", IdentityVerificationValidator.maskCin("bk654321"))
    assertEquals(
      4,
      IdentityVerificationValidator.completionStepsCount(
        cin = "BK654321",
        frontUri = "content://front",
        backUri = "content://back",
        selfieUri = "content://selfie"
      )
    )
    assertEquals(
      2,
      IdentityVerificationValidator.completionStepsCount(
        cin = "BK654321",
        frontUri = "content://front",
        backUri = null,
        selfieUri = null
      )
    )
  }

  @Test
  fun `login validator checks full name email password and produces deterministic hash`() {
    assertTrue(LoginValidator.isValidFullName("يونس العلمي"))
    assertFalse(LoginValidator.isValidFullName("A"))

    assertTrue(LoginValidator.isValidEmail("Youness.User@gmail.com"))
    assertEquals("youness.user@gmail.com", LoginValidator.normalizeEmail("  Youness.User@Gmail.com "))
    assertFalse(LoginValidator.isValidEmail("invalid-email"))

    assertTrue(LoginValidator.isValidPassword("secret123"))
    assertFalse(LoginValidator.isValidPassword("12345"))

    val h1 = LoginValidator.hashPassword("User@Gmail.com", "secret123")
    val h2 = LoginValidator.hashPassword("user@gmail.com ", "secret123")
    assertEquals(64, h1.length)
    assertEquals(h1, h2)
  }

  @Test
  fun `phone validator normalizes moroccan numbers and portfolio items hold craftsman metadata`() {
    assertEquals("+212612345678", PhoneValidator.normalize("0612-345 678"))
    assertTrue(PhoneValidator.isValidMoroccanPhone("0612345678"))
    assertTrue(PhoneValidator.isValidMoroccanPhone("+212 661-234510"))
    assertFalse(PhoneValidator.isValidMoroccanPhone("123"))

    val work = PortfolioItem(
      id = "p1",
      craftsmanId = "uid_123",
      title = "تركيب سخان ماء",
      description = "تبديل كامل مع ضمان",
      categoryName = "سباكة",
      photoUri = "content://work_1",
      createdAtMillis = 12345L
    )
    assertEquals("uid_123", work.craftsmanId)
    assertEquals("تركيب سخان ماء", work.title)

    val session = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      experienceYears = 8,
      basePriceDh = 160,
      bio = "سباك محترف"
    )
    assertEquals(8, session.experienceYears)
    assertEquals(160, session.basePriceDh)
  }

  @Test
  fun `craftsman search finds distant cloud craftsman by name or city and supports cloud filter`() {
    val pins = listOf(
      CraftsmanPin(
        id = "local_1",
        name = "رشيد المحلي",
        specialtyId = "plumbing",
        specialtyName = "سباكة",
        rating = 4.6,
        completedJobs = 30,
        lat = 33.58,
        lng = -7.63,
        basePriceDh = 140,
        distanceKm = 2.0,
        etaMinutes = 6,
        phone = "+212600000001",
        experienceYears = 4,
        bio = "سباك في المعاريف",
        isLiveCloud = false,
        city = "الدار البيضاء"
      ),
      CraftsmanPin(
        id = "cloud_distant",
        name = "يوسف الحرفي",
        specialtyId = "electricity",
        specialtyName = "كهرباء",
        rating = 4.95,
        completedJobs = 85,
        lat = 34.02,
        lng = -6.84,
        basePriceDh = 110,
        distanceKm = 85.0,
        etaMinutes = 120,
        phone = "+212600000002",
        experienceYears = 9,
        bio = "كهربائي منازل محترف",
        isLiveCloud = true,
        city = "الرباط"
      ),
      CraftsmanPin(
        id = "cloud_near",
        name = "سعيد السحابي",
        specialtyId = "plumbing",
        specialtyName = "سباكة",
        rating = 4.8,
        completedJobs = 45,
        lat = 33.58,
        lng = -7.62,
        basePriceDh = 120,
        distanceKm = 2.0,
        etaMinutes = 7,
        phone = "+212600000003",
        experienceYears = 6,
        bio = "إصلاح فوري",
        isLiveCloud = true,
        city = "الدار البيضاء"
      )
    )

    // Without text query or cloud filter, 5 km cutoff hides distant craftsman; cloud tie-breaker wins at equal 2.0 km
    val nearbyOnly = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "",
      radiusKm = 5,
      sort = CraftsmanSortOption.NEAREST,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = false
    )
    assertEquals(listOf("cloud_near", "local_1"), nearbyOnly.map { it.id })

    // Text search by name finds distant cloud craftsman even when radius is 5 km
    val byName = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "يوسف",
      radiusKm = 5,
      sort = CraftsmanSortOption.NEAREST,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = false
    )
    assertEquals(listOf("cloud_distant"), byName.map { it.id })

    // Text search by city also finds distant cloud craftsman across cities
    val byCity = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "الرباط",
      radiusKm = 5,
      sort = CraftsmanSortOption.NEAREST,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = false
    )
    assertEquals(listOf("cloud_distant"), byCity.map { it.id })

    // Cloud-only filter shows all cloud craftsmen regardless of 5 km cutoff and supports TOP_RATED / LOWEST_PRICE sorting
    val cloudTopRated = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "",
      radiusKm = 5,
      sort = CraftsmanSortOption.TOP_RATED,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = true
    )
    assertEquals(listOf("cloud_distant", "cloud_near"), cloudTopRated.map { it.id })

    val cloudLowestPrice = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "",
      radiusKm = 5,
      sort = CraftsmanSortOption.LOWEST_PRICE,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = true
    )
    assertEquals(listOf("cloud_distant", "cloud_near"), cloudLowestPrice.map { it.id })
  }

  @Test
  fun `admin role policy authorizes admin dashboard only for locked admin and supports rejected kyc status`() {
    val adminSession = UserSession(
      role = UserRole.ADMIN,
      isRoleLocked = true,
      email = AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL
    )
    val clientSession = UserSession(role = UserRole.CLIENT, isRoleLocked = true)
    val craftsmanSession = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      verificationStatus = IdentityVerificationStatus.REJECTED,
      rejectionReason = "البطاقة غير واضحة"
    )

    assertTrue(AccountRolePolicy.canAccessAdminDashboard(adminSession))
    assertTrue(adminSession.isAdmin)
    assertFalse(AccountRolePolicy.canAccessAdminDashboard(clientSession))
    assertFalse(AccountRolePolicy.canAccessAdminDashboard(craftsmanSession))
    assertFalse(AccountRolePolicy.canAccessIdentityVerification(adminSession))

    assertEquals(UserRole.ADMIN, UserRole.fromDb("admin"))
    assertEquals(UserRole.CLIENT, UserRole.fromDb("unknown_role"))
    assertEquals(IdentityVerificationStatus.REJECTED, IdentityVerificationStatus.fromDb("rejected"))
    assertEquals("youness065220@gmail.com", AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL)

    val defaultConfig = AppRuntimeConfig()
    assertFalse(defaultConfig.productionMode)
    val prodConfig = AppRuntimeConfig(productionMode = true, updatedByEmail = adminSession.email)
    assertTrue(prodConfig.productionMode)
  }

  @Test
  fun `supabase session auth timestamp parser and private kyc storage references behave securely`() {
    val unauthenticated = UserSession(userId = "local_user", supabaseAccessToken = null)
    assertFalse(unauthenticated.isSupabaseAuthenticated)

    val authenticated = UserSession(
      userId = "a1b2c3d4-0000-0000-0000-000000000001",
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      supabaseAccessToken = "jwt.token.value"
    )
    assertTrue(authenticated.isSupabaseAuthenticated)

    val utcMillis = SupabaseTimestampParser.parseIsoToMillis("2026-11-18T10:15:30.123456Z")
    val offsetMillis = SupabaseTimestampParser.parseIsoToMillis("2026-11-18T11:15:30+01:00")
    assertEquals(utcMillis, offsetMillis)

    // KYC photos must generate private kyc:// references and never public hirafi-media URLs
    val privateRef = SupabaseConfig.privateKycReference("uid-123/front_1000.jpg")
    assertTrue(privateRef.startsWith("kyc://hirafi-kyc/"))
    assertFalse(privateRef.contains("/object/public/"))

    val parsedPrivate = SupabaseConfig.parsePrivateKycReference(privateRef)
    assertEquals("hirafi-kyc" to "uid-123/front_1000.jpg", parsedPrivate)

    val parsedAuthUrl = SupabaseConfig.parsePrivateKycReference(
      "https://example.supabase.co/storage/v1/object/authenticated/hirafi-kyc/uid-123/selfie_2.jpg"
    )
    assertEquals("hirafi-kyc" to "uid-123/selfie_2.jpg", parsedAuthUrl)

    assertNull(SupabaseConfig.parsePrivateKycReference("https://example.supabase.co/storage/v1/object/public/hirafi-media/users/uid-123/avatar.jpg"))
  }

  @Test
  fun `service requests and chat messages preserve participant roles and lifecycle states`() {
    assertEquals(RequestStatus.SEARCHING, RequestStatus.fromDb("searching"))
    assertEquals(RequestStatus.IN_PROGRESS, RequestStatus.fromDb("in_progress"))
    assertEquals(RequestStatus.COMPLETED, RequestStatus.fromDb("completed"))
    assertEquals(RequestStatus.SEARCHING, RequestStatus.fromDb("invalid_status"))

    val clientMessage = ChatMessage(
      id = "msg_1",
      requestId = "req_1",
      senderId = "client_uuid",
      senderName = "ياسين",
      senderRole = UserRole.CLIENT,
      text = "أين وصلت الآن؟",
      createdAtMillis = 1_700_000_000_000L
    )
    val craftsmanMessage = ChatMessage(
      id = "msg_2",
      requestId = "req_1",
      senderId = "craftsman_uuid",
      senderName = "عمر السباك",
      senderRole = UserRole.CRAFTSMAN,
      text = "أنا على بعد 5 دقائق",
      createdAtMillis = 1_700_000_010_000L
    )
    assertEquals(UserRole.CLIENT, clientMessage.senderRole)
    assertEquals(UserRole.CRAFTSMAN, craftsmanMessage.senderRole)
    assertTrue(craftsmanMessage.createdAtMillis > clientMessage.createdAtMillis)
  }

  @Test
  fun `same city live cloud craftsmen remain visible even when distance exceeds 5 km radius`() {
    val pins = listOf(
      CraftsmanPin(
        id = "cloud_same_city_far",
        name = "رشيد الدار البيضاء",
        specialtyId = "plumbing",
        specialtyName = "سباكة",
        rating = 4.9,
        completedJobs = 20,
        lat = 33.61,
        lng = -7.49,
        basePriceDh = 140,
        distanceKm = 18.5,
        etaMinutes = 25,
        phone = "+212600000010",
        experienceYears = 7,
        bio = "سباك محترف",
        isLiveCloud = true,
        city = "الدار البيضاء"
      ),
      CraftsmanPin(
        id = "cloud_other_city_far",
        name = "كريم طنجة",
        specialtyId = "plumbing",
        specialtyName = "سباكة",
        rating = 4.9,
        completedJobs = 30,
        lat = 35.76,
        lng = -5.83,
        basePriceDh = 130,
        distanceKm = 340.0,
        etaMinutes = 180,
        phone = "+212600000011",
        experienceYears = 8,
        bio = "سباك طنجة",
        isLiveCloud = true,
        city = "طنجة"
      )
    )

    val filtered = HirafiViewModel.filterAndSortCraftsmen(
      pins = pins,
      query = "",
      radiusKm = 5,
      sort = CraftsmanSortOption.NEAREST,
      onlySelectedCategory = false,
      selectedCategoryId = "plumbing",
      onlyCloudCraftsmen = false,
      clientCity = "الدار البيضاء"
    )
    assertEquals(listOf("cloud_same_city_far"), filtered.map { it.id })
  }

  @Test
  fun `google oauth callback parser extracts fragment tokens and error descriptions cleanly`() {
    val successUri = "hirafi://auth-callback#access_token=jwt.access.token&refresh_token=ref_123&expires_in=3600&token_type=bearer"
    val params = SupabaseSessionManager.parseOAuthCallbackParams(successUri)
    assertEquals("jwt.access.token", params["access_token"])
    assertEquals("ref_123", params["refresh_token"])
    assertEquals("3600", params["expires_in"])

    val errorUri = "hirafi://auth-callback?error=access_denied&error_description=User%20cancelled%20login"
    val errParams = SupabaseSessionManager.parseOAuthCallbackParams(errorUri)
    assertEquals("access_denied", errParams["error"])
    assertEquals("User cancelled login", errParams["error_description"])

    val authorizeUrl = SupabaseConfig.googleAuthorizeUrl()
    assertTrue(authorizeUrl.contains("provider=google"))
    assertTrue(authorizeUrl.contains("hirafi%3A%2F%2Fauth-callback"))

    val verifier = SupabaseConfig.generatePkceVerifier()
    val challenge = SupabaseConfig.generatePkceChallenge(verifier)
    assertTrue(verifier.length >= 43)
    assertTrue(challenge.isNotBlank() && !challenge.contains("="))
    val pkceAuthorizeUrl = SupabaseConfig.googleAuthorizeUrl(challenge)
    assertTrue(pkceAuthorizeUrl.contains("code_challenge="))
    assertTrue(pkceAuthorizeUrl.contains("code_challenge_method=s256"))

    val pkceUri = "hirafi://auth-callback?code=auth_code_xyz_123"
    val pkceParams = SupabaseSessionManager.parseOAuthCallbackParams(pkceUri)
    assertEquals("auth_code_xyz_123", pkceParams["code"])

    val fallbackEx = GoogleIdTokenFallbackToBrowserException("audience mismatch")
    assertNotNull(fallbackEx)
    assertEquals("audience mismatch", fallbackEx.message)
  }

  @Test
  fun `pro analytics filters sorts craftsman requests and formats market delta and quick replies`() {
    val now = 1_000_000_000L
    val base = ServiceRequest(
      id = "r1",
      clientId = "c1",
      clientName = "يونس",
      categoryId = "plumbing",
      categoryName = "سباكة",
      title = "تسرب",
      description = "",
      addressLabel = "المعاريف",
      lat = 33.5822,
      lng = -7.6324,
      mapX = 0.5f,
      mapY = 0.5f,
      proposedPrice = 140,
      acceptedPrice = null,
      paymentMethod = PaymentMethod.CASH,
      isUrgent = false,
      status = RequestStatus.SEARCHING,
      assignedCraftsmanId = null,
      assignedCraftsmanName = null,
      assignedCraftsmanPhone = null,
      assignedCraftsmanRating = null,
      photoUri = null,
      createdAtMillis = now - 5 * 60_000L,
      updatedAtMillis = now
    )
    val reqs = listOf(
      base,
      base.copy(id = "r2_high", proposedPrice = 240, isUrgent = false, lat = 33.6500, lng = -7.5000, createdAtMillis = now - 60_000L),
      base.copy(id = "r3_urgent_near", proposedPrice = 175, isUrgent = true, lat = 33.5830, lng = -7.6320, createdAtMillis = now - 10 * 60_000L)
    )

    val urgentOnly = HirafiProAnalytics.filterAndSortOpenRequests(reqs, CraftsmanRequestFilter.URGENT, 33.5822, -7.6324)
    assertEquals(listOf("r3_urgent_near"), urgentOnly.map { it.id })

    val highest = HirafiProAnalytics.filterAndSortOpenRequests(reqs, CraftsmanRequestFilter.HIGHEST_PRICE, 33.5822, -7.6324)
    assertEquals(listOf("r2_high", "r3_urgent_near", "r1"), highest.map { it.id })

    val nearest = HirafiProAnalytics.filterAndSortOpenRequests(reqs, CraftsmanRequestFilter.NEAREST, 33.5822, -7.6324)
    assertEquals("r1", nearest.first().id)

    assertEquals(25, HirafiProAnalytics.marketPriceDeltaPercent(175, 100, 180))
    assertEquals("الآن", HirafiProAnalytics.formatRelativeArabicAge(now - 20_000L, now))
    assertEquals("منذ 5 د", HirafiProAnalytics.formatRelativeArabicAge(now - 5 * 60_000L, now))
    assertEquals("منذ 2 س", HirafiProAnalytics.formatRelativeArabicAge(now - 2 * 3_600_000L, now))

    val craftsmanReplies = HirafiProAnalytics.roleAwareQuickReplies(UserRole.CRAFTSMAN)
    val clientReplies = HirafiProAnalytics.roleAwareQuickReplies(UserRole.CLIENT)
    assertTrue(craftsmanReplies.any { it.contains("في الطريق") })
    assertTrue(clientReplies.any { it.contains("في الانتظار") })
    assertFalse(craftsmanReplies == clientReplies)
  }

  @Test
  fun `multilingual i18n engine provides arabic rtl french and english ltr strings and localized analytics`() {
    assertEquals(LayoutDirection.Rtl, AppLanguage.ARABIC.layoutDirection)
    assertEquals(LayoutDirection.Ltr, AppLanguage.FRENCH.layoutDirection)
    assertEquals(LayoutDirection.Ltr, AppLanguage.ENGLISH.layoutDirection)
    assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("FR"))
    assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("unknown"))

    AppLanguage.entries.forEach { lang ->
      val s = HirafiI18n.forLanguage(lang)
      assertEquals(lang, s.language)
      assertTrue(s.appTitle.isNotBlank())
      assertTrue(s.adminTitle.isNotBlank())
      assertTrue(s.authForgotPassword.isNotBlank())
      assertTrue(s.adminRejectPresets.size >= 3)
      assertTrue(UserRole.CRAFTSMAN.localizedLabel(s).isNotBlank())
      assertTrue(RequestStatus.EN_ROUTE.localizedLabel(s).isNotBlank())
      assertTrue(HirafiCategories.byId("plumbing").localizedName(s).isNotBlank())

      val now = 2_000_000_000L
      assertTrue(HirafiProAnalytics.formatRelativeAge(now - 10_000L, now, lang).isNotBlank())
      assertTrue(HirafiProAnalytics.roleAwareQuickReplies(UserRole.CLIENT, lang).isNotEmpty())
      assertTrue(HirafiProAnalytics.roleAwareQuickReplies(UserRole.CRAFTSMAN, lang).isNotEmpty())
    }
  }

  @Test
  fun `password recovery dto and login validator enforce email and password constraints`() {
    val recoverReq = SupabaseRecoverRequest(email = "craftsman@hirafi.ma")
    assertEquals("craftsman@hirafi.ma", recoverReq.email)

    assertTrue(LoginValidator.isValidEmail("pro@hirafi.ma"))
    assertFalse(LoginValidator.isValidEmail("not-an-email"))
    assertFalse(LoginValidator.isValidPassword("123"))
    assertTrue(LoginValidator.isValidPassword("StrongPass!9"))
  }

  @Test
  fun `admin dashboard analytics computes GMV completion rate top categories and filters users and requests`() {
    val baseReq = ServiceRequest(
      id = "r1",
      clientId = "u1",
      clientName = "يوسف",
      categoryId = "plumbing",
      categoryName = "سباكة",
      title = "إصلاح تسرب",
      description = "",
      addressLabel = "المعاريف",
      lat = 33.58,
      lng = -7.63,
      mapX = 0.5f,
      mapY = 0.5f,
      proposedPrice = 150,
      acceptedPrice = 180,
      paymentMethod = PaymentMethod.CASH,
      isUrgent = false,
      status = RequestStatus.COMPLETED,
      assignedCraftsmanId = "c1",
      assignedCraftsmanName = "كمال",
      assignedCraftsmanPhone = null,
      assignedCraftsmanRating = 4.9,
      photoUri = null,
      createdAtMillis = 1000L,
      updatedAtMillis = 2000L
    )
    val requests = listOf(
      baseReq,
      baseReq.copy(id = "r2", categoryId = "plumbing", status = RequestStatus.COMPLETED, acceptedPrice = 120),
      baseReq.copy(id = "r3", categoryId = "electricity", status = RequestStatus.EN_ROUTE, acceptedPrice = 200),
      baseReq.copy(id = "r4", categoryId = "ac", status = RequestStatus.CANCELLED, acceptedPrice = null, proposedPrice = 300)
    )

    val metrics = AdminDashboardAnalytics.computeOverviewMetrics(requests)
    assertEquals(300, metrics.totalGmvDh)
    assertEquals(50, metrics.completionRatePct)
    assertEquals(2, metrics.completedCount)
    assertEquals(1, metrics.activeCount)
    assertEquals(1, metrics.cancelledCount)
    assertEquals("plumbing", metrics.topCategories.first().first)
    assertEquals(2, metrics.topCategories.first().second)

    val users = listOf(
      AdminUserRecord("u1", "أنس العلوي", "anas@hirafi.ma", "الدار البيضاء", UserRole.CLIENT, "plumbing", IdentityVerificationStatus.NOT_SUBMITTED, false, true, 100L),
      AdminUserRecord("u2", "كريم الحرفي", "karim@hirafi.ma", "مراكش", UserRole.CRAFTSMAN, "plumbing", IdentityVerificationStatus.VERIFIED, false, false, 200L)
    )
    assertEquals(listOf("u2"), AdminDashboardAnalytics.filterUsers(users, UserRole.CRAFTSMAN, "مراكش").map { it.userId })
    assertEquals(listOf("u1"), AdminDashboardAnalytics.filterUsers(users, null, "anas@").map { it.userId })

    assertEquals(listOf("r3"), AdminDashboardAnalytics.filterRequests(requests, RequestStatus.EN_ROUTE, "كمال").map { it.id })
    assertTrue(AdminDashboardAnalytics.filterRequests(requests, null, "غير موجود").isEmpty())
  }
}
