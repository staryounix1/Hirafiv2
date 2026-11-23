package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    const val STORAGE_BUCKET = "hirafi-media"
    const val KYC_BUCKET = "hirafi-kyc"
    const val KYC_PRIVATE_REF_PREFIX = "kyc://hirafi-kyc/"
    const val OAUTH_REDIRECT_URL = "hirafi://auth-callback"

    val rawUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = rawUrl.startsWith("https://") &&
            !rawUrl.contains("YOUR_SUPABASE_URL") &&
            anonKey.isNotBlank() &&
            !anonKey.contains("YOUR_SUPABASE_ANON_KEY")

    val baseUrlWithSlash: String
        get() = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"

    fun googleAuthorizeUrl(codeChallenge: String? = null): String {
        val encodedRedirect = java.net.URLEncoder.encode(OAUTH_REDIRECT_URL, "UTF-8")
        val base = "$rawUrl/auth/v1/authorize?provider=google&redirect_to=$encodedRedirect&prompt=select_account"
        return if (!codeChallenge.isNullOrBlank()) {
            val encodedChallenge = java.net.URLEncoder.encode(codeChallenge.trim(), "UTF-8")
            "$base&code_challenge=$encodedChallenge&code_challenge_method=s256"
        } else {
            base
        }
    }

    fun generatePkceVerifier(): String {
        val bytes = ByteArray(48)
        java.security.SecureRandom().nextBytes(bytes)
        return base64UrlEncodeNoPadding(bytes)
    }

    fun generatePkceChallenge(verifier: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64UrlEncodeNoPadding(digest)
    }

    private fun base64UrlEncodeNoPadding(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val hasB1 = i + 1 < bytes.size
            val hasB2 = i + 2 < bytes.size
            val b1 = if (hasB1) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) bytes[i + 2].toInt() and 0xFF else 0

            out.append(alphabet[b0 ushr 2])
            out.append(alphabet[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (hasB1) {
                out.append(alphabet[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
            }
            if (hasB2) {
                out.append(alphabet[b2 and 0x3F])
            }
            i += 3
        }
        return out.toString()
    }

    fun publicMediaUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/public/$STORAGE_BUCKET/$cleanPath"
    }

    fun objectUploadUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/$STORAGE_BUCKET/$cleanPath"
    }

    fun kycObjectUploadUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/$KYC_BUCKET/$cleanPath"
    }

    fun privateKycReference(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$KYC_PRIVATE_REF_PREFIX$cleanPath"
    }

    fun signObjectUrl(bucket: String, objectPath: String): String {
        val cleanBucket = bucket.trim('/')
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/sign/$cleanBucket/$cleanPath"
    }

    fun toAbsoluteStorageUrl(pathOrUrl: String): String {
        val clean = pathOrUrl.trim()
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }
        val normalizedPath = if (clean.startsWith("/storage/v1/")) {
            clean
        } else if (clean.startsWith("/")) {
            "/storage/v1$clean"
        } else {
            "/storage/v1/$clean"
        }
        return "$rawUrl$normalizedPath"
    }

    fun parsePrivateKycReference(uri: String): Pair<String, String>? {
        val trimmed = uri.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith(KYC_PRIVATE_REF_PREFIX)) {
            val path = trimmed.removePrefix(KYC_PRIVATE_REF_PREFIX).trimStart('/')
            return if (path.isNotBlank()) KYC_BUCKET to path else null
        }
        val authenticatedMarker = "/storage/v1/object/authenticated/$KYC_BUCKET/"
        val authIdx = trimmed.indexOf(authenticatedMarker)
        if (authIdx >= 0) {
            val path = trimmed.substring(authIdx + authenticatedMarker.length).substringBefore('?').trimStart('/')
            return if (path.isNotBlank()) KYC_BUCKET to path else null
        }
        val legacyPublicMarker = "/storage/v1/object/public/$STORAGE_BUCKET/verifications/"
        val legacyIdx = trimmed.indexOf(legacyPublicMarker)
        if (legacyIdx >= 0) {
            val path = "verifications/" + trimmed.substring(legacyIdx + legacyPublicMarker.length).substringBefore('?').trimStart('/')
            return STORAGE_BUCKET to path
        }
        return null
    }
}
