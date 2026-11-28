package com.subtrack.core.domain.backup

import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Pure-Kotlin 100% offline app-to-app stack transfer & backup codec.
 *
 * Uses a single ultra-compact binary + raw-Deflate v3 format (`subcycler://s/<token>`) shared
 * across:
 * - Short offline deep links & QR Stack Cards (`subcycler://s/<token>`, ~75% shorter than JSON+GZIP)
 * - Messaging & social media shares (`Intent.ACTION_SEND`, clipboard paste, QR card images)
 * - Single portable `.subcycler` backup files (`FileProvider` / SAF) paired with clean human captions
 *
 * Transparently decodes v3 short links (`subcycler://s/...`), v2 links (`subcycler://import?d=...`),
 * and legacy v1 local backup files with zero extra dependencies and zero network permissions.
 */
object SubscriptionBackupCodec {

    private const val BACKUP_VERSION_JSON = 2
    private const val BINARY_FORMAT_VERSION: Byte = 3
    private const val MAX_DECOMPRESSED_BYTES = 512 * 1024
    private const val MAX_ENCODED_TOKEN_CHARS = 128 * 1024
    private const val MAX_SUBSCRIPTION_COUNT = 500
    private val EPOCH_DATE: LocalDate = LocalDate.of(2020, 1, 1)

    const val DEEP_LINK_SCHEME = "subcycler"
    const val SHORT_LINK_HOST = "s"
    const val LEGACY_LINK_HOST = "import"
    const val DEEP_LINK_PARAM = "d"
    const val SHORT_LINK_PREFIX = "$DEEP_LINK_SCHEME://$SHORT_LINK_HOST/"
    const val TRANSFER_LINK_PREFIX = SHORT_LINK_PREFIX
    const val LEGACY_TRANSFER_LINK_PREFIX = "$DEEP_LINK_SCHEME://$LEGACY_LINK_HOST?$DEEP_LINK_PARAM="
    const val BACKUP_FILE_EXTENSION = "subcycler"

    // Compact binary per-subscription optional field bitmask
    private const val FLAG_CUSTOM_CURRENCY = 1 shl 0
    private const val FLAG_YEARLY = 1 shl 1
    private const val FLAG_NON_DEFAULT_CATEGORY = 1 shl 2
    private const val FLAG_NON_DEFAULT_STRATEGY = 1 shl 3
    private const val FLAG_NON_DEFAULT_ACTIVE_DAYS = 1 shl 4
    private const val FLAG_NON_DEFAULT_BLACKOUT_DAYS = 1 shl 5
    private const val FLAG_CUSTOM_COLOR = 1 shl 6
    private const val FLAG_CUSTOM_ICON = 1 shl 7
    private const val FLAG_HAS_NOTES = 1 shl 8
    private const val FLAG_AUTO_RENEW_CANCELLED = 1 shl 9
    private const val FLAG_BILLING_DATE_UNCONFIRMED = 1 shl 10
    private const val FLAG_STRATEGY_UNCONFIRMED = 1 shl 11
    private const val FLAG_HAS_CANCELLED_CYCLE = 1 shl 12
    private const val FLAG_HAS_COMPLETED_BREAKS = 1 shl 13
    private const val FLAG_HAS_EARNED_SAVINGS = 1 shl 14
    private const val FLAG_HAS_BONUS_DAYS = 1 shl 15

    private val shortLinkRegex = Regex(
        """subcycler://s/([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )
    private val legacyTransferLinkRegex = Regex(
        """subcycler://import\?(?:[^#\s]*&)?d=([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )

    fun containsTransferLink(text: String): Boolean =
        shortLinkRegex.containsMatchIn(text) || legacyTransferLinkRegex.containsMatchIn(text)

    fun extractTransferToken(rawInput: String): String? =
        shortLinkRegex.find(rawInput)?.groupValues?.getOrNull(1)
            ?: legacyTransferLinkRegex.find(rawInput)?.groupValues?.getOrNull(1)

    /**
     * Produces a sleek, ultra-compact offline deep link (`subcycler://s/<token>`) carrying the
     * stack using binary field packing + omitted defaults + raw Deflate (zero `H4sI` header).
     *
     * When [includePersonalHistory] is `false`, personal notes, earned savings, completed break
     * counts, bonus active days, and auto-renew cancellation flags are stripped so users can
     * safely share a clean subscription stack template with friends.
     */
    fun toTransferLink(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean = true
    ): String {
        val prepared = prepareForExport(subscriptions, includePersonalHistory)
        val binary = encodeBinaryV3(prepared)
        val compressed = rawDeflate(binary)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        return "$SHORT_LINK_PREFIX$token"
    }

    /**
     * Produces a clean, human-readable bulleted summary with ZERO encoded token strings,
     * ideal as a chat caption paired with a `.subcycler` file attachment or Stack Card PNG.
     */
    fun toHumanShareCaption(
        subscriptions: List<Subscription>,
        includePersonalHistory: Boolean = true,
        footerHint: String = "Open the attached file or card in SubCycler to import."
    ): String {
        val prepared = prepareForExport(subscriptions, includePersonalHistory)
        val count = prepared.size
        val noun = if (count == 1) "subscription" else "subscriptions"
        return buildString {
            append("My SubCycler stack ($count $noun)\n")
            prepared.take(8).forEach { sub ->
                val cycleText = when {
                    sub.billingPeriod == BillingPeriod.YEARLY -> "Yearly"
                    sub.strategy == RotationStrategy.CONTINUOUS -> "Always on"
                    else -> "${sub.activeDays}d on / ${sub.blackoutDays}d off"
                }
                val costText = if (sub.cost > 0.0) {
                    val formatted = if (sub.cost % 1.0 == 0.0) {
                        sub.cost.toLong().toString()
                    } else {
                        String.format(Locale.US, "%.2f", sub.cost)
                    }
                    " · ${sub.currency} $formatted${sub.billingPeriod.shortSuffix}"
                } else {
                    ""
                }
                append("• ${sub.name}$costText ($cycleText)\n")
            }
            if (count > 8) {
                append("• +${count - 8} more\n")
            }
            append("\n")
            append(footerHint)
        }
    }

    /**
     * Produces a sleek short message for messaging & social apps carrying the compact
     * `subcycler://s/...` link.
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
            append("SubCycler stack — $count $noun$namesSummary\n")
            append("Open or share this message to SubCycler:\n")
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
     * - A compact v3 deep link (`subcycler://s/<token>`)
     * - A v2 deep link (`subcycler://import?d=<token>`)
     * - A chat/social message containing either link anywhere in its body
     * - A `.subcycler` backup file
     * - A legacy v1 local backup file for backward compatibility
     */
    fun decodeTransferPayload(rawInput: String): Result<List<Subscription>> = runCatching {
        val trimmed = rawInput.trim()
        require(trimmed.isNotEmpty()) { "Nothing to import." }

        val shortToken = shortLinkRegex.find(trimmed)?.groupValues?.getOrNull(1)
        if (shortToken != null) {
            require(shortToken.length <= MAX_ENCODED_TOKEN_CHARS) { "Backup link is too large to import." }
            val compressed = Base64.getUrlDecoder().decode(shortToken)
            val decompressed = rawInflateBounded(compressed)
            return@runCatching decodeBinaryV3(decompressed)
        }

        val legacyToken = legacyTransferLinkRegex.find(trimmed)?.groupValues?.getOrNull(1)
        if (legacyToken != null) {
            require(legacyToken.length <= MAX_ENCODED_TOKEN_CHARS) { "Backup link is too large to import." }
            val compressed = Base64.getUrlDecoder().decode(legacyToken)
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
     * Retained for internal/legacy compatibility; new app flows prefer [toTransferLink],
     * [toBackupFilePayload], and [decodeTransferPayload].
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

    private fun encodeBinaryV3(subscriptions: List<Subscription>): ByteArray {
        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)
        out.writeByte(BINARY_FORMAT_VERSION.toInt())

        // Write shared primary currency once in header (3 ASCII bytes)
        val defaultCurrency = subscriptions
            .groupingBy { it.currency.uppercase(Locale.US).take(3).ifBlank { "USD" } }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: "USD"
        out.writeUTF(defaultCurrency)
        writeVarInt(out, subscriptions.size)

        subscriptions.forEach { sub ->
            val subCurrency = sub.currency.uppercase(Locale.US).take(3).ifBlank { "USD" }
            var flags = 0
            if (subCurrency != defaultCurrency) flags = flags or FLAG_CUSTOM_CURRENCY
            if (sub.billingPeriod == BillingPeriod.YEARLY) flags = flags or FLAG_YEARLY
            if (sub.category != SubscriptionCategory.STREAMING) flags = flags or FLAG_NON_DEFAULT_CATEGORY
            if (sub.strategy != RotationStrategy.ROTATION_45_DAYS) flags = flags or FLAG_NON_DEFAULT_STRATEGY
            if (sub.customActiveDays != 30) flags = flags or FLAG_NON_DEFAULT_ACTIVE_DAYS
            if (sub.customBlackoutDays != 15) flags = flags or FLAG_NON_DEFAULT_BLACKOUT_DAYS
            if (sub.colorHex != "#0C6B4E") flags = flags or FLAG_CUSTOM_COLOR
            if (sub.iconName != "Tv") flags = flags or FLAG_CUSTOM_ICON
            if (sub.notes.isNotEmpty()) flags = flags or FLAG_HAS_NOTES
            if (sub.isAutoRenewCancelled) flags = flags or FLAG_AUTO_RENEW_CANCELLED
            if (!sub.isBillingDateConfirmed) flags = flags or FLAG_BILLING_DATE_UNCONFIRMED
            if (!sub.isStrategyConfirmed) flags = flags or FLAG_STRATEGY_UNCONFIRMED
            if (sub.cancelledCycleIndex != null) flags = flags or FLAG_HAS_CANCELLED_CYCLE
            if (sub.completedBreaksCount > 0) flags = flags or FLAG_HAS_COMPLETED_BREAKS
            if (sub.earnedSavingsTotal > 0.0) flags = flags or FLAG_HAS_EARNED_SAVINGS
            if (sub.bonusActiveDays > 0) flags = flags or FLAG_HAS_BONUS_DAYS

            writeVarInt(out, flags)
            out.writeUTF(sub.name)
            // Compact cents encoding (minor units as unsigned varlong)
            val costCents = kotlin.math.round(sub.cost.coerceAtLeast(0.0) * 100.0).toLong()
            writeVarLong(out, costCents)
            // Compact day offset relative to 2020-01-01 (zig-zag varint)
            val dayOffset = ChronoUnit.DAYS.between(EPOCH_DATE, sub.startDate).toInt()
            writeSignedVarInt(out, dayOffset)

            if ((flags and FLAG_CUSTOM_CURRENCY) != 0) out.writeUTF(subCurrency)
            if ((flags and FLAG_NON_DEFAULT_CATEGORY) != 0) out.writeByte(sub.category.ordinal)
            if ((flags and FLAG_NON_DEFAULT_STRATEGY) != 0) out.writeByte(sub.strategy.ordinal)
            if ((flags and FLAG_NON_DEFAULT_ACTIVE_DAYS) != 0) writeVarInt(out, sub.customActiveDays)
            if ((flags and FLAG_NON_DEFAULT_BLACKOUT_DAYS) != 0) writeVarInt(out, sub.customBlackoutDays)
            if ((flags and FLAG_CUSTOM_COLOR) != 0) out.writeUTF(sub.colorHex)
            if ((flags and FLAG_CUSTOM_ICON) != 0) out.writeUTF(sub.iconName)
            if ((flags and FLAG_HAS_NOTES) != 0) out.writeUTF(sub.notes)
            if ((flags and FLAG_HAS_CANCELLED_CYCLE) != 0) writeSignedVarInt(out, sub.cancelledCycleIndex ?: 0)
            if ((flags and FLAG_HAS_COMPLETED_BREAKS) != 0) writeVarInt(out, sub.completedBreaksCount)
            if ((flags and FLAG_HAS_EARNED_SAVINGS) != 0) {
                writeVarLong(out, kotlin.math.round(sub.earnedSavingsTotal.coerceAtLeast(0.0) * 100.0).toLong())
            }
            if ((flags and FLAG_HAS_BONUS_DAYS) != 0) writeVarInt(out, sub.bonusActiveDays)
        }
        out.flush()
        return baos.toByteArray()
    }

    private fun decodeBinaryV3(bytes: ByteArray): List<Subscription> {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val version = input.readByte()
        require(version == BINARY_FORMAT_VERSION) { "Unsupported SubCycler backup version." }

        val defaultCurrency = input.readUTF().uppercase(Locale.US).take(3).ifBlank { "USD" }
        val count = readVarInt(input)
        require(count in 0..MAX_SUBSCRIPTION_COUNT) { "Backup contains an invalid number of entries." }

        val categories = SubscriptionCategory.entries
        val strategies = RotationStrategy.entries

        return List(count) {
            val flags = readVarInt(input)
            val name = input.readUTF().trim()
            require(name.isNotBlank()) { "A subscription entry is missing its name." }
            val costCents = readVarLong(input)
            require(costCents >= 0L) { "Subscription '$name' has an invalid price." }
            val dayOffset = readSignedVarInt(input)
            val startDate = EPOCH_DATE.plusDays(dayOffset.toLong())

            val currency = if ((flags and FLAG_CUSTOM_CURRENCY) != 0) {
                input.readUTF().uppercase(Locale.US).take(3).ifBlank { defaultCurrency }
            } else {
                defaultCurrency
            }
            val billingPeriod = if ((flags and FLAG_YEARLY) != 0) BillingPeriod.YEARLY else BillingPeriod.MONTHLY
            val category = if ((flags and FLAG_NON_DEFAULT_CATEGORY) != 0) {
                categories.getOrElse(input.readUnsignedByte()) { SubscriptionCategory.OTHER }
            } else {
                SubscriptionCategory.STREAMING
            }
            val strategy = if ((flags and FLAG_NON_DEFAULT_STRATEGY) != 0) {
                strategies.getOrElse(input.readUnsignedByte()) { RotationStrategy.ROTATION_45_DAYS }
            } else {
                RotationStrategy.ROTATION_45_DAYS
            }
            val customActiveDays = if ((flags and FLAG_NON_DEFAULT_ACTIVE_DAYS) != 0) {
                readVarInt(input).coerceAtLeast(1)
            } else {
                30
            }
            val customBlackoutDays = if ((flags and FLAG_NON_DEFAULT_BLACKOUT_DAYS) != 0) {
                readVarInt(input).coerceAtLeast(0)
            } else {
                15
            }
            val colorHex = if ((flags and FLAG_CUSTOM_COLOR) != 0) {
                input.readUTF().ifBlank { "#0C6B4E" }
            } else {
                "#0C6B4E"
            }
            val iconName = if ((flags and FLAG_CUSTOM_ICON) != 0) {
                input.readUTF().ifBlank { "Tv" }
            } else {
                "Tv"
            }
            val notes = if ((flags and FLAG_HAS_NOTES) != 0) input.readUTF() else ""
            val isAutoRenewCancelled = (flags and FLAG_AUTO_RENEW_CANCELLED) != 0
            val isBillingDateConfirmed = (flags and FLAG_BILLING_DATE_UNCONFIRMED) == 0
            val isStrategyConfirmed = (flags and FLAG_STRATEGY_UNCONFIRMED) == 0
            val cancelledCycleIndex = if ((flags and FLAG_HAS_CANCELLED_CYCLE) != 0) readSignedVarInt(input) else null
            val completedBreaksCount = if ((flags and FLAG_HAS_COMPLETED_BREAKS) != 0) {
                readVarInt(input).coerceAtLeast(0)
            } else {
                0
            }
            val earnedSavingsTotal = if ((flags and FLAG_HAS_EARNED_SAVINGS) != 0) {
                readVarLong(input).coerceAtLeast(0L) / 100.0
            } else {
                0.0
            }
            val bonusActiveDays = if ((flags and FLAG_HAS_BONUS_DAYS) != 0) {
                readVarInt(input).coerceAtLeast(0)
            } else {
                0
            }

            Subscription(
                name = name,
                cost = costCents / 100.0,
                currency = currency,
                billingPeriod = billingPeriod,
                category = category,
                startDate = startDate,
                strategy = strategy,
                customActiveDays = customActiveDays,
                customBlackoutDays = customBlackoutDays,
                colorHex = colorHex,
                iconName = iconName,
                notes = notes,
                isAutoRenewCancelled = isAutoRenewCancelled,
                isBillingDateConfirmed = isBillingDateConfirmed,
                isStrategyConfirmed = isStrategyConfirmed,
                cancelledCycleIndex = cancelledCycleIndex,
                completedBreaksCount = completedBreaksCount,
                earnedSavingsTotal = earnedSavingsTotal,
                bonusActiveDays = bonusActiveDays
            )
        }
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.writeByte(v and 0x7F)
    }

    private fun readVarInt(input: DataInputStream): Int {
        var shift = 0
        var result = 0
        while (shift < 35) {
            val b = input.readUnsignedByte()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        error("Malformed backup integer")
    }

    private fun writeSignedVarInt(out: DataOutputStream, value: Int) {
        val zigzag = (value shl 1) xor (value shr 31)
        writeVarInt(out, zigzag)
    }

    private fun readSignedVarInt(input: DataInputStream): Int {
        val raw = readVarInt(input)
        return (raw ushr 1) xor -(raw and 1)
    }

    private fun writeVarLong(out: DataOutputStream, value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.writeByte(((v and 0x7FL) or 0x80L).toInt())
            v = v ushr 7
        }
        out.writeByte((v and 0x7FL).toInt())
    }

    private fun readVarLong(input: DataInputStream): Long {
        var shift = 0
        var result = 0L
        while (shift < 70) {
            val b = input.readUnsignedByte().toLong()
            result = result or ((b and 0x7FL) shl shift)
            if ((b and 0x80L) == 0L) return result
            shift += 7
        }
        error("Malformed backup number")
    }

    private fun rawDeflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        return try {
            deflater.setInput(bytes)
            deflater.finish()
            val out = ByteArrayOutputStream(bytes.size)
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                if (count > 0) out.write(buffer, 0, count)
            }
            out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun rawInflateBounded(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(bytes)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var total = 0
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                } else {
                    total += count
                    require(total <= MAX_DECOMPRESSED_BYTES) { "Backup exceeds maximum safe size." }
                    out.write(buffer, 0, count)
                }
            }
            require(total > 0) { "Backup link is corrupted." }
            out.toByteArray()
        } finally {
            inflater.end()
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
            "{\n  \"app\": \"SubCycler\",\n  \"version\": $BACKUP_VERSION_JSON,\n  \"subscriptions\": [\n$items\n  ]\n}\n"
        } else {
            "{\"app\":\"SubCycler\",\"version\":$BACKUP_VERSION_JSON,\"subscriptions\":[$items]}"
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
