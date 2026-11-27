package com.subtrack.core.domain.backup

import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Base64
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Pure-Kotlin 100% offline app-to-app stack transfer & backup codec.
 *
 * Uses a single unified compressed payload shared across:
 * - Offline custom deep links (`subcycler://import?d=<token>`)
 * - Messaging & social media shares (`Intent.ACTION_SEND`, clipboard paste)
 * - Single portable `.subcycler` backup files (`FileProvider` / SAF)
 *
 * Also retains transparent read-compatibility for legacy v1 backup files while requiring
 * zero extra dependencies and zero network or storage permissions.
 */
object SubscriptionBackupCodec {

    private const val BACKUP_VERSION = 2
    private const val MAX_DECOMPRESSED_BYTES = 512 * 1024
    private const val MAX_ENCODED_TOKEN_CHARS = 128 * 1024

    const val DEEP_LINK_SCHEME = "subcycler"
    const val DEEP_LINK_HOST = "import"
    const val DEEP_LINK_PARAM = "d"
    const val TRANSFER_LINK_PREFIX = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST?$DEEP_LINK_PARAM="
    const val BACKUP_FILE_EXTENSION = "subcycler"

    private val transferLinkRegex = Regex(
        """subcycler://import\?(?:[^#\s]*&)?d=([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )

    fun containsTransferLink(text: String): Boolean = transferLinkRegex.containsMatchIn(text)

    fun extractTransferToken(rawInput: String): String? =
        transferLinkRegex.find(rawInput)?.groupValues?.getOrNull(1)

    /**
     * Produces a compact offline deep link (`subcycler://import?d=...`) carrying the stack.
     * When [includePersonalHistory] is `false`, personal notes, earned savings, completed break
     * counts, bonus active days, and auto-renew cancellation flags are stripped so users can
     * safely share a clean subscription stack template with friends.
     */
    fun toTransferLink(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean = true
    ): String {
        val prepared = prepareForExport(subscriptions, includePersonalHistory)
        val compactPayload = encodePayload(prepared, pretty = false)
        val compressed = gzipCompress(compactPayload.toByteArray(Charsets.UTF_8))
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        return "$TRANSFER_LINK_PREFIX$token"
    }

    /**
     * Produces a clean, jargon-free share message ready for WhatsApp, Telegram, LINE, Discord,
     * Messages, Notes, or social apps, embedding the offline `subcycler://import?d=...` link.
     */
    fun toShareMessage(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean = true
    ): String {
        val link = toTransferLink(subscriptions, includePersonalHistory)
        val count = subscriptions.size
        val previewNames = subscriptions
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .take(4)
        val namesSummary = when {
            previewNames.isEmpty() -> ""
            count > previewNames.size -> " (${previewNames.joinToString(", ")} +${count - previewNames.size} more)"
            else -> " (${previewNames.joinToString(", ")})"
        }
        val noun = if (count == 1) "subscription" else "subscriptions"
        return buildString {
            append("SubCycler stack — $count $noun$namesSummary\n\n")
            append("Tap the link or share this message to SubCycler to import:\n")
            append(link)
        }
    }

    /**
     * Produces the single unified `.subcycler` portable backup file payload.
     */
    fun toBackupFilePayload(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean = true
    ): String {
        val link = toTransferLink(subscriptions, includePersonalHistory)
        val count = subscriptions.size
        val noun = if (count == 1) "subscription" else "subscriptions"
        return "# SubCycler Backup ($count $noun)\n$link\n"
    }

    /**
     * Decodes any SubCycler transfer input:
     * - A `subcycler://import?d=...` deep link
     * - A chat/social message containing the link anywhere in its body
     * - A `.subcycler` backup file
     * - A legacy v1 backup file for backward compatibility
     */
    fun decodeTransferPayload(rawInput: String): Result<List<Subscription>> = runCatching {
        val trimmed = rawInput.trim()
        require(trimmed.isNotEmpty()) { "Nothing to import." }

        val token = extractTransferToken(trimmed)
        if (token != null) {
            require(token.length <= MAX_ENCODED_TOKEN_CHARS) { "Backup link is too large to import." }
            val compressed = Base64.getUrlDecoder().decode(token)
            val decompressed = gzipDecompressBounded(compressed)
            return@runCatching decodeEnvelope(decompressed.toString(Charsets.UTF_8))
        }

        // Backward-compatibility path for older v1 local backup files
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return@runCatching decodeEnvelope(trimmed)
        }

        error("This doesn't look like a SubCycler transfer link or backup file.")
    }

    /**
     * Retained for internal/legacy compatibility; new app flows prefer [toBackupFilePayload],
     * [toTransferLink], and [decodeTransferPayload].
     */
    fun toJson(subscriptions: List<Subscription>): String = encodePayload(subscriptions, pretty = true)

    fun fromJson(rawJson: String): Result<List<Subscription>> = decodeTransferPayload(rawJson)

    private fun prepareForExport(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean
    ): List<Subscription> {
        if (includePersonalHistory) return subscriptions
        return subscriptions.map { sub ->
            sub.copy(
                notes = "",
                isAutoRenewCancelled = false,
                cancelledCycleIndex = null,
                completedBreaksCount = 0,
                earnedSavingsTotal = 0.0,
                bonusActiveDays = 0
            )
        }
    }

    private fun encodePayload(subscriptions: List<Subscription>, pretty: Boolean): String {
        val sep = if (pretty) ",\n" else ","
        val items = subscriptions.joinToString(sep) { sub ->
            buildString {
                if (pretty) append("    {") else append('{')
                append("\"name\":${jsonString(sub.name)},")
                append("\"cost\":${sub.cost},")
                append("\"currency\":${jsonString(sub.currency)},")
                append("\"billingPeriod\":${jsonString(sub.billingPeriod.name)},")
                append("\"category\":${jsonString(sub.category.name)},")
                append("\"startDate\":${jsonString(sub.startDate.toString())},")
                append("\"strategy\":${jsonString(sub.strategy.name)},")
                append("\"customActiveDays\":${sub.customActiveDays},")
                append("\"customBlackoutDays\":${sub.customBlackoutDays},")
                append("\"colorHex\":${jsonString(sub.colorHex)},")
                append("\"iconName\":${jsonString(sub.iconName)},")
                append("\"notes\":${jsonString(sub.notes)},")
                append("\"isAutoRenewCancelled\":${sub.isAutoRenewCancelled},")
                append("\"isBillingDateConfirmed\":${sub.isBillingDateConfirmed},")
                append("\"isStrategyConfirmed\":${sub.isStrategyConfirmed},")
                append("\"cancelledCycleIndex\":${sub.cancelledCycleIndex ?: "null"},")
                append("\"completedBreaksCount\":${sub.completedBreaksCount},")
                append("\"earnedSavingsTotal\":${sub.earnedSavingsTotal},")
                append("\"bonusActiveDays\":${sub.bonusActiveDays}")
                append('}')
            }
        }
        return if (pretty) {
            "{\n  \"app\": \"SubCycler\",\n  \"version\": $BACKUP_VERSION,\n  \"subscriptions\": [\n$items\n  ]\n}\n"
        } else {
            "{\"app\":\"SubCycler\",\"version\":$BACKUP_VERSION,\"subscriptions\":[$items]}"
        }
    }

    private fun decodeEnvelope(rawEnvelope: String): List<Subscription> {
        val trimmed = rawEnvelope.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
            "This doesn't look like a valid SubCycler backup."
        }

        val arrayStart = trimmed.indexOf('[')
        val arrayEnd = trimmed.lastIndexOf(']')
        require(arrayStart in 0 until arrayEnd) { "Backup is missing its subscriptions list." }

        val arrayBody = trimmed.substring(arrayStart + 1, arrayEnd).trim()
        if (arrayBody.isEmpty()) return emptyList()

        val objectStrings = splitTopLevelObjects(arrayBody)
        return objectStrings.map { objJson ->
            val fields = parseFlatJsonObject(objJson)
            val name = fields["name"]?.takeIf { it.isNotBlank() }
                ?: error("A subscription entry is missing its name.")
            val cost = fields["cost"]?.toDoubleOrNull()?.takeIf { it >= 0.0 }
                ?: error("Subscription '$name' has an invalid price.")
            val startDate = fields["startDate"]?.let { LocalDate.parse(it) }
                ?: error("Subscription '$name' is missing a start date.")

            Subscription(
                name = name,
                cost = cost,
                currency = fields["currency"]?.uppercase(Locale.US)?.take(3)?.ifBlank { "USD" } ?: "USD",
                billingPeriod = fields["billingPeriod"]
                    ?.let { runCatching { BillingPeriod.valueOf(it) }.getOrNull() }
                    ?: BillingPeriod.MONTHLY,
                category = fields["category"]
                    ?.let { runCatching { SubscriptionCategory.valueOf(it) }.getOrNull() }
                    ?: SubscriptionCategory.OTHER,
                startDate = startDate,
                strategy = fields["strategy"]
                    ?.let { runCatching { RotationStrategy.valueOf(it) }.getOrNull() }
                    ?: RotationStrategy.ROTATION_45_DAYS,
                customActiveDays = fields["customActiveDays"]?.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                customBlackoutDays = fields["customBlackoutDays"]?.toIntOrNull()?.coerceAtLeast(0) ?: 15,
                colorHex = fields["colorHex"]?.ifBlank { "#0C6B4E" } ?: "#0C6B4E",
                iconName = fields["iconName"]?.ifBlank { "Tv" } ?: "Tv",
                notes = fields["notes"].orEmpty(),
                isAutoRenewCancelled = fields["isAutoRenewCancelled"]?.toBooleanStrictOrNull() ?: false,
                isBillingDateConfirmed = fields["isBillingDateConfirmed"]?.toBooleanStrictOrNull() ?: true,
                isStrategyConfirmed = fields["isStrategyConfirmed"]?.toBooleanStrictOrNull() ?: true,
                cancelledCycleIndex = fields["cancelledCycleIndex"]?.toIntOrNull(),
                completedBreaksCount = fields["completedBreaksCount"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                earnedSavingsTotal = fields["earnedSavingsTotal"]?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                bonusActiveDays = fields["bonusActiveDays"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
        }
    }

    private fun gzipCompress(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { gzip -> gzip.write(bytes) }
        return out.toByteArray()
    }

    private fun gzipDecompressBounded(bytes: ByteArray): ByteArray {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var total = 0
            while (true) {
                val read = gzip.read(buffer)
                if (read == -1) break
                total += read
                require(total <= MAX_DECOMPRESSED_BYTES) { "Backup exceeds maximum safe size." }
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun splitTopLevelObjects(body: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inString = false
        var escape = false
        var objStart = -1

        for (i in body.indices) {
            val c = body[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\' && inString) {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (inString) continue

            if (c == '{') {
                if (depth == 0) objStart = i
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0 && objStart >= 0) {
                    result.add(body.substring(objStart, i + 1))
                    objStart = -1
                }
            }
        }
        return result
    }

    private fun parseFlatJsonObject(obj: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        val inner = obj.trim().removePrefix("{").removeSuffix("}")
        var i = 0
        val n = inner.length

        fun skipWs() {
            while (i < n && (inner[i].isWhitespace() || inner[i] == ',')) i++
        }

        fun readString(): String {
            require(i < n && inner[i] == '"') { "Expected quoted text" }
            i++
            val sb = StringBuilder()
            while (i < n) {
                val c = inner[i++]
                if (c == '"') return sb.toString()
                if (c == '\\' && i < n) {
                    when (val esc = inner[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = inner.substring(i, (i + 4).coerceAtMost(n))
                            hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                            i += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            error("Unterminated text in backup")
        }

        while (i < n) {
            skipWs()
            if (i >= n) break
            val key = readString()
            while (i < n && inner[i].isWhitespace()) i++
            require(i < n && inner[i] == ':') { "Malformed entry near $key" }
            i++
            while (i < n && inner[i].isWhitespace()) i++
            if (i >= n) break
            val value: String? = if (inner[i] == '"') {
                readString()
            } else {
                val start = i
                while (i < n && inner[i] != ',' && !inner[i].isWhitespace()) i++
                val token = inner.substring(start, i)
                if (token == "null") null else token
            }
            map[key] = value
        }
        return map
    }
}
