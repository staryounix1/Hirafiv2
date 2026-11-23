package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.CraftsmanSortOption
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.UserRole
import com.example.ui.components.CraftsmanRequestFilter

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val shortCode: String,
    val layoutDirection: LayoutDirection
) {
    ARABIC("ar", "العربية", "AR", LayoutDirection.Rtl),
    FRENCH("fr", "Français", "FR", LayoutDirection.Ltr),
    ENGLISH("en", "English", "EN", LayoutDirection.Ltr);

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: ARABIC
    }
}

data class HirafiStrings(
    val language: AppLanguage,
    // Common
    val appTitle: String,
    val currencyDh: String,
    val unitKm: String,
    val unitMin: String,
    val close: String,
    val cancel: String,
    val confirm: String,
    val save: String,
    val delete: String,
    val back: String,
    val search: String,
    val all: String,
    val refresh: String,
    val connected: String,
    val loading: String,
    val yes: String,
    val no: String,

    // Roles
    val roleClient: String,
    val roleCraftsman: String,
    val roleAdmin: String,

    // Payment methods
    val payCash: String,
    val payTransfer: String,
    val payCard: String,

    // Request statuses
    val statusSearching: String,
    val statusAccepted: String,
    val statusEnRoute: String,
    val statusArrived: String,
    val statusInProgress: String,
    val statusCompleted: String,
    val statusCancelled: String,

    // Offer statuses
    val offerPending: String,
    val offerAccepted: String,
    val offerDeclined: String,
    val offerCountered: String,
    val offerExpired: String,

    // Verification statuses
    val kycNotSubmitted: String,
    val kycPending: String,
    val kycVerified: String,
    val kycRejected: String,

    // Categories
    val catPlumbing: String,
    val catElectricity: String,
    val catAc: String,
    val catCarpentry: String,
    val catPainting: String,
    val catLocksmith: String,
    val catMasonry: String,
    val catCleaning: String,

    // Sort / Filters
    val sortNearest: String,
    val sortTopRated: String,
    val sortLowestPrice: String,
    val sortFastestEta: String,
    val filterUrgent: String,
    val filterHighestPrice: String,

    // Auth / Login / Reset / Strength
    val authTagline: String,
    val authSignInTab: String,
    val authSignUpTab: String,
    val authGoogleButton: String,
    val authOrEmail: String,
    val authEmailLabel: String,
    val authPasswordLabel: String,
    val authFullNameLabel: String,
    val authPhoneLabel: String,
    val authCityLabel: String,
    val authChooseRole: String,
    val authChooseSpecialty: String,
    val authSignInCta: String,
    val authSignUpCta: String,
    val authForgotPassword: String,
    val authForgotPasswordTitle: String,
    val authForgotPasswordBody: String,
    val authSendResetLink: String,
    val authResetEmailSent: String,
    val authPasswordStrengthLabel: String,
    val authPasswordWeak: String,
    val authPasswordMedium: String,
    val authPasswordStrong: String,
    val authPasswordHint: String,
    val authEmailRequiredError: String,
    val authPasswordMinError: String,
    val authSwitchAccount: String,

    // Drawer & Navigation
    val navAdminDashboard: String,
    val navCraftsmanHome: String,
    val navClientHome: String,
    val navProfile: String,
    val navPortfolioBadge: String,
    val navOrders: String,
    val navVerification: String,
    val navSafety: String,
    val navCityPrefix: String,
    val navLanguagePrefix: String,
    val chooseLanguageTitle: String,
    val chooseCityTitle: String,
    val logoutAction: String,
    val logoutTitle: String,
    val logoutMessage: String,
    val logoutConfirm: String,
    val roleBadgeAdminTitle: String,
    val roleBadgeAdminDesc: String,
    val roleBadgeCraftsmanDesc: String,
    val roleBadgeClientDesc: String,
    val accountSuspendedMsg: String,

    // Client Home & Bidding
    val clientHeroBadge: String,
    val clientWhereNeedCraftsman: String,
    val clientSearchCraftsmanHint: String,
    val clientIssueTitleHint: String,
    val clientYourProposedPrice: String,
    val clientPublishRequest: String,
    val clientOffersIncoming: String,
    val clientCounterOfferTitle: String,
    val clientCounterOfferConfirm: String,
    val clientVerificationCodeTitle: String,
    val clientCancelService: String,
    val clientOpenChat: String,
    val clientSafetyCenter: String,
    val clientReorderLabel: String,

    // Craftsman Dashboard
    val craftsmanOnline: String,
    val craftsmanOffline: String,
    val craftsmanOpenRequestsTitle: String,
    val craftsmanSendOffer: String,
    val craftsmanImEnRoute: String,
    val craftsmanImArrived: String,
    val craftsmanEnterCodeHint: String,
    val craftsmanVerifyStart: String,
    val craftsmanCompleteJob: String,
    val craftsmanLiveGpsActive: String,

    // Chat
    val chatHeaderTitle: String,
    val chatInputPlaceholder: String,
    val chatEmptyHint: String,
    val chatLiveChannel: String,
    val chatScrollToBottom: String,

    // Map & Telemetry
    val mapRecenterRoute: String,
    val mapStyleStandard: String,
    val mapStyleStreets: String,
    val mapStyleDark: String,
    val mapStyleSatellite: String,
    val mapLiveEtaPrefix: String,

    // Profile & Safety
    val profileSessionCardTitle: String,
    val profileAuthProviderLabel: String,
    val profileCloudSynced: String,
    val profileChangeLanguage: String,
    val safetyHeroTitle: String,
    val safetyHeroSubtitle: String,
    val ordersTitle: String,
    val ordersEmptyTitle: String,

    // Admin Dashboard
    val adminTitle: String,
    val adminTabOverview: String,
    val adminTabKyc: String,
    val adminTabUsers: String,
    val adminTabRequests: String,
    val adminGmvTitle: String,
    val adminCompletionRateTitle: String,
    val adminRequestsBreakdownTitle: String,
    val adminTopCategoriesTitle: String,
    val adminProductionModeTitle: String,
    val adminProductionConfirmMsg: String,
    val adminUsersSearchHint: String,
    val adminRequestsSearchHint: String,
    val adminSuspendConfirmTitle: String,
    val adminSuspendConfirmMsg: String,
    val adminCancelReqConfirmTitle: String,
    val adminCancelReqConfirmMsg: String,
    val adminRejectPresetsLabel: String,
    val adminRejectPresets: List<String>
)

object HirafiI18n {
    val arabic = HirafiStrings(
        language = AppLanguage.ARABIC,
        appTitle = "Hirafi",
        currencyDh = "درهم",
        unitKm = "كم",
        unitMin = "د",
        close = "إغلاق",
        cancel = "إلغاء",
        confirm = "تأكيد",
        save = "حفظ",
        delete = "حذف",
        back = "رجوع",
        search = "بحث",
        all = "الكل",
        refresh = "تحديث",
        connected = "متصل",
        loading = "جاري التحميل...",
        yes = "نعم",
        no = "لا",

        roleClient = "زبون",
        roleCraftsman = "حرفي",
        roleAdmin = "أدمين",

        payCash = "نقداً",
        payTransfer = "تحويل بنكي",
        payCard = "بطاقة",

        statusSearching = "جاري البحث عن حرفي",
        statusAccepted = "تم قبول العرض",
        statusEnRoute = "الحرفي في الطريق",
        statusArrived = "وصل الحرفي للموقع",
        statusInProgress = "جاري إنجاز الخدمة",
        statusCompleted = "تمت الخدمة",
        statusCancelled = "ملغاة",

        offerPending = "قيد الانتظار",
        offerAccepted = "مقبول",
        offerDeclined = "مرفوض",
        offerCountered = "مفاوضة",
        offerExpired = "منتهي",

        kycNotSubmitted = "غير موثّق بعد",
        kycPending = "قيد المراجعة الإدارية",
        kycVerified = "هوية موثّقة",
        kycRejected = "مرفوض — يرجى التحديث",

        catPlumbing = "سباكة",
        catElectricity = "كهرباء",
        catAc = "تبريد وتكييف",
        catCarpentry = "نجارة",
        catPainting = "صباغة",
        catLocksmith = "أقفال ومفاتيح",
        catMasonry = "بناء وزليج",
        catCleaning = "تنظيف",

        sortNearest = "الأقرب",
        sortTopRated = "الأعلى تقييماً",
        sortLowestPrice = "الأقل سعراً",
        sortFastestEta = "الأسرع وصولاً",
        filterUrgent = "عاجل ⚡",
        filterHighestPrice = "أعلى سعراً",

        authTagline = "حدد سعرك واختر حرفيك في دقائق",
        authSignInTab = "تسجيل الدخول",
        authSignUpTab = "حساب جديد",
        authGoogleButton = "المتابعة عبر Google",
        authOrEmail = "أو عبر البريد الإلكتروني",
        authEmailLabel = "البريد الإلكتروني",
        authPasswordLabel = "كلمة المرور",
        authFullNameLabel = "الاسم الكامل",
        authPhoneLabel = "رقم الهاتف",
        authCityLabel = "المدينة",
        authChooseRole = "اختر نوع الحساب",
        authChooseSpecialty = "التخصص الحرفي",
        authSignInCta = "دخول إلى حسابي",
        authSignUpCta = "إنشاء الحساب وتثبيت الدور",
        authForgotPassword = "نسيت كلمة المرور؟",
        authForgotPasswordTitle = "استرجاع كلمة المرور",
        authForgotPasswordBody = "أدخل بريدك الإلكتروني وسنرسل لك رابطاً آمناً لإعادة تعيين كلمة المرور.",
        authSendResetLink = "إرسال الرابط",
        authResetEmailSent = "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني",
        authPasswordStrengthLabel = "قوة كلمة المرور",
        authPasswordWeak = "ضعيفة",
        authPasswordMedium = "متوسطة",
        authPasswordStrong = "قوية وممتازة",
        authPasswordHint = "استخدم 8 أحرف على الأقل مع أرقام أو رموز لأمان أعلى",
        authEmailRequiredError = "يرجى إدخال بريد إلكتروني صحيح",
        authPasswordMinError = "كلمة المرور يجب أن لا تقل عن 6 أحرف",
        authSwitchAccount = "تبديل الحساب",

        navAdminDashboard = "لوحة تحكم الأدمين",
        navCraftsmanHome = "الطلبات الواردة والخريطة",
        navClientHome = "طلب خدمة (الخريطة)",
        navProfile = "الملف الشخصي",
        navPortfolioBadge = "معرض الأعمال",
        navOrders = "سجل الطلبات والضمانات",
        navVerification = "التحقق من الهوية",
        navSafety = "الأمان والطوارئ",
        navCityPrefix = "المدينة:",
        navLanguagePrefix = "اللغة:",
        chooseLanguageTitle = "اختر لغة التطبيق",
        chooseCityTitle = "اختر مدينتك",
        logoutAction = "تسجيل الخروج من الحساب",
        logoutTitle = "تسجيل الخروج",
        logoutMessage = "هل أنت متأكد أنك تريد تسجيل الخروج من الحساب الحالي؟ ستتم العودة إلى شاشة تسجيل الحساب.",
        logoutConfirm = "خروج",
        roleBadgeAdminTitle = "حساب أدمين رسمي",
        roleBadgeAdminDesc = "صلاحيات كاملة — إدارة المنصة والتوثيق",
        roleBadgeCraftsmanDesc = "حساب مقفل — مخصص لوضع الحرفي فقط",
        roleBadgeClientDesc = "حساب مقفل — مخصص لوضع الزبون فقط",
        accountSuspendedMsg = "الحساب معلّق مؤقتاً من الإدارة",

        clientHeroBadge = "خدمة فورية • تسعير مرن",
        clientWhereNeedCraftsman = "أين تحتاج الحرفي؟",
        clientSearchCraftsmanHint = "ابحث عن حرفي أو عنوان...",
        clientIssueTitleHint = "وصف مختصر للعطل أو الخدمة...",
        clientYourProposedPrice = "سعرك المقترح",
        clientPublishRequest = "نشر الطلب للحرفيين",
        clientOffersIncoming = "العروض الواردة",
        clientCounterOfferTitle = "اقتراح سعر مضاد",
        clientCounterOfferConfirm = "إرسال السعر المضاد",
        clientVerificationCodeTitle = "رمز التحقق لبدء الخدمة",
        clientCancelService = "إلغاء الخدمة",
        clientOpenChat = "المحادثة",
        clientSafetyCenter = "الأمان",
        clientReorderLabel = "إعادة الطلب",

        craftsmanOnline = "متصل لاستقبال الطلبات",
        craftsmanOffline = "غير متصل حالياً",
        craftsmanOpenRequestsTitle = "الطلبات القريبة منك",
        craftsmanSendOffer = "تقديم عرض سعر",
        craftsmanImEnRoute = "أنا في الطريق",
        craftsmanImArrived = "وصلت للموقع",
        craftsmanEnterCodeHint = "أدخل رمز التحقق من الزبون",
        craftsmanVerifyStart = "تأكيد وبدء الخدمة",
        craftsmanCompleteJob = "إتمام الخدمة واستلام الأجر",
        craftsmanLiveGpsActive = "بث الموقع المباشر مفعل",

        chatHeaderTitle = "المحادثة المباشرة",
        chatInputPlaceholder = "اكتب رسالتك هنا...",
        chatEmptyHint = "ابدأ المحادثة الآن لتنسيق تفاصيل الوصول والسعر.",
        chatLiveChannel = "قناة محادثة آمنة وفورية",
        chatScrollToBottom = "الأسفل",

        mapRecenterRoute = "تتبع المسار",
        mapStyleStandard = "قياسي",
        mapStyleStreets = "شوارع",
        mapStyleDark = "ليلي",
        mapStyleSatellite = "أقمار",
        mapLiveEtaPrefix = "الوصول خلال",

        profileSessionCardTitle = "حالة الحساب والاتصال",
        profileAuthProviderLabel = "طريقة الدخول",
        profileCloudSynced = "مزامنة سحابية نشطة",
        profileChangeLanguage = "تغيير اللغة",
        safetyHeroTitle = "مركز الأمان والحماية",
        safetyHeroSubtitle = "حمايتك ومراقبة جودة الخدمة أولوية في كل طلب",
        ordersTitle = "سجل الطلبات والضمانات",
        ordersEmptyTitle = "لا توجد طلبات مسجلة بعد",

        adminTitle = "مركز قيادة الإدارة المتقدم",
        adminTabOverview = "نظرة عامة",
        adminTabKyc = "التوثيق",
        adminTabUsers = "المستخدمون",
        adminTabRequests = "الطلبات",
        adminGmvTitle = "إجمالي حجم التعاملات (GMV)",
        adminCompletionRateTitle = "نسبة إنجاز الطلبات",
        adminRequestsBreakdownTitle = "توزيع حالات الطلبات",
        adminTopCategoriesTitle = "أكثر الحرف طلباً",
        adminProductionModeTitle = "تبديل وضع الإنتاج",
        adminProductionConfirmMsg = "هل أنت متأكد من تغيير وضع التشغيل للمنصة؟ يؤثر هذا على جميع المستخدمين فوراً.",
        adminUsersSearchHint = "ابحث بالاسم أو البريد أو المدينة...",
        adminRequestsSearchHint = "ابحث بالعنوان أو الزبون أو الحرفة...",
        adminSuspendConfirmTitle = "تأكيد تغيير حالة الحساب",
        adminSuspendConfirmMsg = "هل تريد بالتأكيد تنفيذ هذا الإجراء على حساب المستخدم؟",
        adminCancelReqConfirmTitle = "تأكيد إلغاء الطلب إدارياً",
        adminCancelReqConfirmMsg = "سيتم إلغاء الطلب وإشعار الطرفين فوراً. هل تود المتابعة؟",
        adminRejectPresetsLabel = "أسباب رفض سريعة:",
        adminRejectPresets = listOf(
            "صورة البطاقة الأمامية غير واضحة",
            "صورة البطاقة الخلفية غير واضحة",
            "صورة السيلفي غير مطابقة للبطاقة",
            "رقم البطاقة الوطنية غير صحيح"
        )
    )

    val french = HirafiStrings(
        language = AppLanguage.FRENCH,
        appTitle = "Hirafi",
        currencyDh = "DH",
        unitKm = "km",
        unitMin = "min",
        close = "Fermer",
        cancel = "Annuler",
        confirm = "Confirmer",
        save = "Enregistrer",
        delete = "Supprimer",
        back = "Retour",
        search = "Rechercher",
        all = "Tout",
        refresh = "Actualiser",
        connected = "Connecté",
        loading = "Chargement...",
        yes = "Oui",
        no = "Non",

        roleClient = "Client",
        roleCraftsman = "Artisan",
        roleAdmin = "Admin",

        payCash = "Espèces",
        payTransfer = "Virement",
        payCard = "Carte",

        statusSearching = "Recherche d'un artisan",
        statusAccepted = "Offre acceptée",
        statusEnRoute = "Artisan en route",
        statusArrived = "Artisan arrivé",
        statusInProgress = "Intervention en cours",
        statusCompleted = "Service terminé",
        statusCancelled = "Annulé",

        offerPending = "En attente",
        offerAccepted = "Acceptée",
        offerDeclined = "Refusée",
        offerCountered = "Contre-offre",
        offerExpired = "Expirée",

        kycNotSubmitted = "Non vérifié",
        kycPending = "En cours d'examen",
        kycVerified = "Identité vérifiée",
        kycRejected = "Refusé — à mettre à jour",

        catPlumbing = "Plomberie",
        catElectricity = "Électricité",
        catAc = "Climatisation",
        catCarpentry = "Menuiserie",
        catPainting = "Peinture",
        catLocksmith = "Serrurerie",
        catMasonry = "Maçonnerie & Zellige",
        catCleaning = "Nettoyage",

        sortNearest = "Plus proche",
        sortTopRated = "Mieux noté",
        sortLowestPrice = "Prix croissant",
        sortFastestEta = "Plus rapide",
        filterUrgent = "Urgent ⚡",
        filterHighestPrice = "Prix élevé",

        authTagline = "Proposez votre prix, choisissez votre artisan",
        authSignInTab = "Connexion",
        authSignUpTab = "Inscription",
        authGoogleButton = "Continuer avec Google",
        authOrEmail = "Ou par e-mail",
        authEmailLabel = "Adresse e-mail",
        authPasswordLabel = "Mot de passe",
        authFullNameLabel = "Nom complet",
        authPhoneLabel = "Téléphone",
        authCityLabel = "Ville",
        authChooseRole = "Type de compte",
        authChooseSpecialty = "Spécialité artisanale",
        authSignInCta = "Se connecter",
        authSignUpCta = "Créer le compte",
        authForgotPassword = "Mot de passe oublié ?",
        authForgotPasswordTitle = "Réinitialiser le mot de passe",
        authForgotPasswordBody = "Saisissez votre adresse e-mail pour recevoir un lien sécurisé de réinitialisation.",
        authSendResetLink = "Envoyer le lien",
        authResetEmailSent = "Lien de réinitialisation envoyé à votre adresse e-mail",
        authPasswordStrengthLabel = "Robustesse",
        authPasswordWeak = "Faible",
        authPasswordMedium = "Moyen",
        authPasswordStrong = "Fort",
        authPasswordHint = "Utilisez au moins 8 caractères avec chiffres ou symboles",
        authEmailRequiredError = "Veuillez saisir une adresse e-mail valide",
        authPasswordMinError = "Le mot de passe doit contenir au moins 6 caractères",
        authSwitchAccount = "Changer de compte",

        navAdminDashboard = "Panneau d'administration",
        navCraftsmanHome = "Demandes & Carte",
        navClientHome = "Commander un service",
        navProfile = "Mon profil",
        navPortfolioBadge = "Portfolio",
        navOrders = "Historique & Garanties",
        navVerification = "Vérification d'identité",
        navSafety = "Sécurité & Urgence",
        navCityPrefix = "Ville :",
        navLanguagePrefix = "Langue :",
        chooseLanguageTitle = "Choisir la langue",
        chooseCityTitle = "Choisir votre ville",
        logoutAction = "Se déconnecter",
        logoutTitle = "Déconnexion",
        logoutMessage = "Voulez-vous vraiment vous déconnecter de ce compte ?",
        logoutConfirm = "Déconnecter",
        roleBadgeAdminTitle = "Compte Admin officiel",
        roleBadgeAdminDesc = "Accès complet — gestion et modération",
        roleBadgeCraftsmanDesc = "Compte verrouillé — mode Artisan",
        roleBadgeClientDesc = "Compte verrouillé — mode Client",
        accountSuspendedMsg = "Compte temporairement suspendu",

        clientHeroBadge = "Service rapide • Prix libre",
        clientWhereNeedCraftsman = "Où avez-vous besoin d'un artisan ?",
        clientSearchCraftsmanHint = "Rechercher un artisan ou une adresse...",
        clientIssueTitleHint = "Décrivez brièvement votre besoin...",
        clientYourProposedPrice = "Votre tarif proposé",
        clientPublishRequest = "Publier la demande",
        clientOffersIncoming = "Offres reçues",
        clientCounterOfferTitle = "Proposer une contre-offre",
        clientCounterOfferConfirm = "Envoyer la contre-offre",
        clientVerificationCodeTitle = "Code de démarrage client",
        clientCancelService = "Annuler le service",
        clientOpenChat = "Discussion",
        clientSafetyCenter = "Sécurité",
        clientReorderLabel = "Recommander",

        craftsmanOnline = "En ligne — disponible",
        craftsmanOffline = "Hors ligne",
        craftsmanOpenRequestsTitle = "Demandes à proximité",
        craftsmanSendOffer = "Envoyer une offre",
        craftsmanImEnRoute = "Je suis en route",
        craftsmanImArrived = "Je suis arrivé",
        craftsmanEnterCodeHint = "Entrez le code du client",
        craftsmanVerifyStart = "Valider et démarrer",
        craftsmanCompleteJob = "Terminer l'intervention",
        craftsmanLiveGpsActive = "Suivi GPS en direct actif",

        chatHeaderTitle = "Messagerie directe",
        chatInputPlaceholder = "Écrivez votre message...",
        chatEmptyHint = "Démarrez la conversation pour coordonner l'arrivée et les détails.",
        chatLiveChannel = "Canal sécurisé en temps réel",
        chatScrollToBottom = "Bas",

        mapRecenterRoute = "Suivre l'itinéraire",
        mapStyleStandard = "Standard",
        mapStyleStreets = "Rues",
        mapStyleDark = "Nuit",
        mapStyleSatellite = "Satellite",
        mapLiveEtaPrefix = "Arrivée dans",

        profileSessionCardTitle = "État du compte et session",
        profileAuthProviderLabel = "Méthode de connexion",
        profileCloudSynced = "Synchronisation cloud active",
        profileChangeLanguage = "Changer de langue",
        safetyHeroTitle = "Centre de sécurité",
        safetyHeroSubtitle = "Votre protection et le suivi qualité à chaque mission",
        ordersTitle = "Historique des commandes",
        ordersEmptyTitle = "Aucune commande enregistrée",

        adminTitle = "Console d'administration avancée",
        adminTabOverview = "Aperçu",
        adminTabKyc = "KYC",
        adminTabUsers = "Utilisateurs",
        adminTabRequests = "Demandes",
        adminGmvTitle = "Volume brut (GMV)",
        adminCompletionRateTitle = "Taux d'achèvement",
        adminRequestsBreakdownTitle = "Répartition des statuts",
        adminTopCategoriesTitle = "Métiers les plus demandés",
        adminProductionModeTitle = "Mode Production",
        adminProductionConfirmMsg = "Confirmez-vous le basculement du mode de fonctionnement global ?",
        adminUsersSearchHint = "Nom, e-mail ou ville...",
        adminRequestsSearchHint = "Titre, client ou métier...",
        adminSuspendConfirmTitle = "Confirmer l'action utilisateur",
        adminSuspendConfirmMsg = "Voulez-vous vraiment modifier le statut de ce compte ?",
        adminCancelReqConfirmTitle = "Annuler la demande",
        adminCancelReqConfirmMsg = "La demande sera annulée et les deux parties notifiées.",
        adminRejectPresetsLabel = "Motifs rapides :",
        adminRejectPresets = listOf(
            "Photo recto de la CIN illisible",
            "Photo verso de la CIN illisible",
            "Selfie non conforme à la pièce",
            "Numéro CIN invalide"
        )
    )

    val english = HirafiStrings(
        language = AppLanguage.ENGLISH,
        appTitle = "Hirafi",
        currencyDh = "DH",
        unitKm = "km",
        unitMin = "min",
        close = "Close",
        cancel = "Cancel",
        confirm = "Confirm",
        save = "Save",
        delete = "Delete",
        back = "Back",
        search = "Search",
        all = "All",
        refresh = "Refresh",
        connected = "Connected",
        loading = "Loading...",
        yes = "Yes",
        no = "No",

        roleClient = "Client",
        roleCraftsman = "Craftsman",
        roleAdmin = "Admin",

        payCash = "Cash",
        payTransfer = "Bank Transfer",
        payCard = "Card",

        statusSearching = "Finding a craftsman",
        statusAccepted = "Offer accepted",
        statusEnRoute = "Craftsman on the way",
        statusArrived = "Craftsman arrived",
        statusInProgress = "Work in progress",
        statusCompleted = "Service completed",
        statusCancelled = "Cancelled",

        offerPending = "Pending",
        offerAccepted = "Accepted",
        offerDeclined = "Declined",
        offerCountered = "Countered",
        offerExpired = "Expired",

        kycNotSubmitted = "Not verified yet",
        kycPending = "Under review",
        kycVerified = "Verified ID",
        kycRejected = "Rejected — update required",

        catPlumbing = "Plumbing",
        catElectricity = "Electricity",
        catAc = "HVAC & AC",
        catCarpentry = "Carpentry",
        catPainting = "Painting",
        catLocksmith = "Locksmith",
        catMasonry = "Masonry & Tiling",
        catCleaning = "Cleaning",

        sortNearest = "Nearest",
        sortTopRated = "Top Rated",
        sortLowestPrice = "Lowest Price",
        sortFastestEta = "Fastest ETA",
        filterUrgent = "Urgent ⚡",
        filterHighestPrice = "Highest Price",

        authTagline = "Name your price, choose your pro",
        authSignInTab = "Sign In",
        authSignUpTab = "Create Account",
        authGoogleButton = "Continue with Google",
        authOrEmail = "Or with email",
        authEmailLabel = "Email address",
        authPasswordLabel = "Password",
        authFullNameLabel = "Full name",
        authPhoneLabel = "Phone number",
        authCityLabel = "City",
        authChooseRole = "Select account role",
        authChooseSpecialty = "Craft specialty",
        authSignInCta = "Sign In",
        authSignUpCta = "Create Account",
        authForgotPassword = "Forgot password?",
        authForgotPasswordTitle = "Reset password",
        authForgotPasswordBody = "Enter your email address and we'll send a secure reset link.",
        authSendResetLink = "Send Reset Link",
        authResetEmailSent = "Password reset link sent to your email",
        authPasswordStrengthLabel = "Password strength",
        authPasswordWeak = "Weak",
        authPasswordMedium = "Fair",
        authPasswordStrong = "Strong",
        authPasswordHint = "Use 8+ chars with numbers or symbols for stronger security",
        authEmailRequiredError = "Please enter a valid email address",
        authPasswordMinError = "Password must be at least 6 characters",
        authSwitchAccount = "Switch Account",

        navAdminDashboard = "Admin Control Center",
        navCraftsmanHome = "Incoming Jobs & Map",
        navClientHome = "Request Service (Map)",
        navProfile = "My Profile",
        navPortfolioBadge = "Portfolio",
        navOrders = "Orders & Guarantees",
        navVerification = "ID Verification",
        navSafety = "Safety & Emergency",
        navCityPrefix = "City:",
        navLanguagePrefix = "Language:",
        chooseLanguageTitle = "Choose App Language",
        chooseCityTitle = "Choose Your City",
        logoutAction = "Sign Out",
        logoutTitle = "Sign Out",
        logoutMessage = "Are you sure you want to sign out of this account?",
        logoutConfirm = "Sign Out",
        roleBadgeAdminTitle = "Official Admin Account",
        roleBadgeAdminDesc = "Full access — platform & KYC moderation",
        roleBadgeCraftsmanDesc = "Locked role — Craftsman mode only",
        roleBadgeClientDesc = "Locked role — Client mode only",
        accountSuspendedMsg = "Account temporarily suspended",

        clientHeroBadge = "On-demand • Flexible pricing",
        clientWhereNeedCraftsman = "Where do you need a pro?",
        clientSearchCraftsmanHint = "Search craftsman or address...",
        clientIssueTitleHint = "Brief description of the issue...",
        clientYourProposedPrice = "Your offered price",
        clientPublishRequest = "Publish Request",
        clientOffersIncoming = "Incoming Offers",
        clientCounterOfferTitle = "Send Counter-Offer",
        clientCounterOfferConfirm = "Send Counter-Offer",
        clientVerificationCodeTitle = "Client Start Code",
        clientCancelService = "Cancel Service",
        clientOpenChat = "Chat",
        clientSafetyCenter = "Safety",
        clientReorderLabel = "Reorder",

        craftsmanOnline = "Online — accepting jobs",
        craftsmanOffline = "Offline",
        craftsmanOpenRequestsTitle = "Nearby Requests",
        craftsmanSendOffer = "Send Price Offer",
        craftsmanImEnRoute = "I'm on the way",
        craftsmanImArrived = "I've arrived",
        craftsmanEnterCodeHint = "Enter client start code",
        craftsmanVerifyStart = "Verify & Start Job",
        craftsmanCompleteJob = "Complete Job",
        craftsmanLiveGpsActive = "Live GPS tracking active",

        chatHeaderTitle = "Direct Chat",
        chatInputPlaceholder = "Type a message...",
        chatEmptyHint = "Start the conversation to coordinate arrival and details.",
        chatLiveChannel = "Secure real-time chat",
        chatScrollToBottom = "Latest",

        mapRecenterRoute = "Track Route",
        mapStyleStandard = "Standard",
        mapStyleStreets = "Streets",
        mapStyleDark = "Night",
        mapStyleSatellite = "Satellite",
        mapLiveEtaPrefix = "Arrives in",

        profileSessionCardTitle = "Account & Sync Status",
        profileAuthProviderLabel = "Sign-in method",
        profileCloudSynced = "Cloud sync active",
        profileChangeLanguage = "Change Language",
        safetyHeroTitle = "Safety & Protection Center",
        safetyHeroSubtitle = "Your safety and service quality tracked on every job",
        ordersTitle = "Orders & Guarantees",
        ordersEmptyTitle = "No orders recorded yet",

        adminTitle = "Advanced Admin Console",
        adminTabOverview = "Overview",
        adminTabKyc = "KYC",
        adminTabUsers = "Users",
        adminTabRequests = "Requests",
        adminGmvTitle = "Gross Volume (GMV)",
        adminCompletionRateTitle = "Completion Rate",
        adminRequestsBreakdownTitle = "Request Status Breakdown",
        adminTopCategoriesTitle = "Top Demanded Trades",
        adminProductionModeTitle = "Toggle Production Mode",
        adminProductionConfirmMsg = "Are you sure you want to change the global platform environment mode?",
        adminUsersSearchHint = "Search name, email or city...",
        adminRequestsSearchHint = "Search title, client or trade...",
        adminSuspendConfirmTitle = "Confirm Account Action",
        adminSuspendConfirmMsg = "Do you want to apply this status change to the user account?",
        adminCancelReqConfirmTitle = "Confirm Admin Cancellation",
        adminCancelReqConfirmMsg = "This request will be cancelled and both parties notified.",
        adminRejectPresetsLabel = "Quick rejection reasons:",
        adminRejectPresets = listOf(
            "ID front photo is blurry or unreadable",
            "ID back photo is blurry or unreadable",
            "Selfie does not match ID photo",
            "Invalid National ID number"
        )
    )

    fun forLanguage(lang: AppLanguage): HirafiStrings = when (lang) {
        AppLanguage.ARABIC -> arabic
        AppLanguage.FRENCH -> french
        AppLanguage.ENGLISH -> english
    }
}

val LocalHirafiStrings = staticCompositionLocalOf { HirafiI18n.arabic }

@Composable
fun hirafiStrings(): HirafiStrings = LocalHirafiStrings.current

fun UserRole.localizedLabel(s: HirafiStrings): String = when (this) {
    UserRole.CLIENT -> s.roleClient
    UserRole.CRAFTSMAN -> s.roleCraftsman
    UserRole.ADMIN -> s.roleAdmin
}

fun PaymentMethod.localizedLabel(s: HirafiStrings): String = when (this) {
    PaymentMethod.CASH -> s.payCash
    PaymentMethod.TRANSFER -> s.payTransfer
    PaymentMethod.CARD -> s.payCard
}

fun RequestStatus.localizedLabel(s: HirafiStrings): String = when (this) {
    RequestStatus.SEARCHING -> s.statusSearching
    RequestStatus.ACCEPTED -> s.statusAccepted
    RequestStatus.EN_ROUTE -> s.statusEnRoute
    RequestStatus.ARRIVED -> s.statusArrived
    RequestStatus.IN_PROGRESS -> s.statusInProgress
    RequestStatus.COMPLETED -> s.statusCompleted
    RequestStatus.CANCELLED -> s.statusCancelled
}

fun OfferStatus.localizedLabel(s: HirafiStrings): String = when (this) {
    OfferStatus.PENDING -> s.offerPending
    OfferStatus.ACCEPTED -> s.offerAccepted
    OfferStatus.DECLINED -> s.offerDeclined
    OfferStatus.COUNTERED -> s.offerCountered
    OfferStatus.EXPIRED -> s.offerExpired
}

fun IdentityVerificationStatus.localizedLabel(s: HirafiStrings): String = when (this) {
    IdentityVerificationStatus.NOT_SUBMITTED -> s.kycNotSubmitted
    IdentityVerificationStatus.PENDING_REVIEW -> s.kycPending
    IdentityVerificationStatus.VERIFIED -> s.kycVerified
    IdentityVerificationStatus.REJECTED -> s.kycRejected
}

fun ServiceCategory.localizedName(s: HirafiStrings): String = when (id) {
    "plumbing" -> s.catPlumbing
    "electricity" -> s.catElectricity
    "ac" -> s.catAc
    "carpentry" -> s.catCarpentry
    "painting" -> s.catPainting
    "locksmith" -> s.catLocksmith
    "masonry" -> s.catMasonry
    "cleaning" -> s.catCleaning
    else -> nameAr
}

fun CraftsmanSortOption.localizedLabel(s: HirafiStrings): String = when (this) {
    CraftsmanSortOption.NEAREST -> s.sortNearest
    CraftsmanSortOption.TOP_RATED -> s.sortTopRated
    CraftsmanSortOption.LOWEST_PRICE -> s.sortLowestPrice
}

fun OfferSortOption.localizedLabel(s: HirafiStrings): String = when (this) {
    OfferSortOption.BEST_PRICE -> s.sortLowestPrice
    OfferSortOption.FASTEST_ETA -> s.sortFastestEta
    OfferSortOption.TOP_RATED -> s.sortTopRated
}

fun CraftsmanRequestFilter.localizedLabel(s: HirafiStrings): String = when (this) {
    CraftsmanRequestFilter.ALL -> s.all
    CraftsmanRequestFilter.URGENT -> s.filterUrgent
    CraftsmanRequestFilter.HIGHEST_PRICE -> s.filterHighestPrice
    CraftsmanRequestFilter.NEAREST -> s.sortNearest
}
