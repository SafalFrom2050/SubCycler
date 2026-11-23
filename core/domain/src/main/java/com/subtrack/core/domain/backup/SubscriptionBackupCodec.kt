package com.subtrack.core.domain.backup

import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import java.time.LocalDate
import java.util.Locale

/**
 * Pure-Kotlin 100% offline JSON backup/restore & CSV export codec.
 * Requires zero extra dependencies and zero network/storage permissions (works via Android SAF).
 */
object SubscriptionBackupCodec {

    private const val BACKUP_VERSION = 1

    fun toJson(subscriptions: List<Subscription>): String {
        val items = subscriptions.joinToString(",\n") { sub ->
            buildString {
                append("    {\n")
                append("      \"name\": ${jsonString(sub.name)},\n")
                append("      \"cost\": ${sub.cost},\n")
                append("      \"currency\": ${jsonString(sub.currency)},\n")
                append("      \"billingPeriod\": ${jsonString(sub.billingPeriod.name)},\n")
                append("      \"category\": ${jsonString(sub.category.name)},\n")
                append("      \"startDate\": ${jsonString(sub.startDate.toString())},\n")
                append("      \"strategy\": ${jsonString(sub.strategy.name)},\n")
                append("      \"customActiveDays\": ${sub.customActiveDays},\n")
                append("      \"customBlackoutDays\": ${sub.customBlackoutDays},\n")
                append("      \"colorHex\": ${jsonString(sub.colorHex)},\n")
                append("      \"iconName\": ${jsonString(sub.iconName)},\n")
                append("      \"notes\": ${jsonString(sub.notes)},\n")
                append("      \"isAutoRenewCancelled\": ${sub.isAutoRenewCancelled},\n")
                append("      \"isBillingDateConfirmed\": ${sub.isBillingDateConfirmed},\n")
                append("      \"isStrategyConfirmed\": ${sub.isStrategyConfirmed},\n")
                append("      \"cancelledCycleIndex\": ${sub.cancelledCycleIndex ?: "null"},\n")
                append("      \"completedBreaksCount\": ${sub.completedBreaksCount},\n")
                append("      \"earnedSavingsTotal\": ${sub.earnedSavingsTotal},\n")
                append("      \"bonusActiveDays\": ${sub.bonusActiveDays}\n")
                append("    }")
            }
        }
        return "{\n  \"app\": \"SubCycler\",\n  \"version\": $BACKUP_VERSION,\n  \"subscriptions\": [\n$items\n  ]\n}\n"
    }

    fun fromJson(rawJson: String): Result<List<Subscription>> = runCatching {
        val trimmed = rawJson.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) { "Invalid backup JSON format." }

        val arrayStart = trimmed.indexOf('[')
        val arrayEnd = trimmed.lastIndexOf(']')
        require(arrayStart in 0 until arrayEnd) { "Missing subscriptions array in backup file." }

        val arrayBody = trimmed.substring(arrayStart + 1, arrayEnd).trim()
        if (arrayBody.isEmpty()) return@runCatching emptyList()

        val objectStrings = splitTopLevelObjects(arrayBody)
        objectStrings.map { objJson ->
            val fields = parseFlatJsonObject(objJson)
            val name = fields["name"]?.takeIf { it.isNotBlank() }
                ?: error("Backup entry missing name")
            val cost = fields["cost"]?.toDoubleOrNull()?.takeIf { it >= 0.0 }
                ?: error("Backup entry '$name' has invalid cost")
            val startDate = fields["startDate"]?.let { LocalDate.parse(it) }
                ?: error("Backup entry '$name' missing startDate")

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

    fun toCsv(subscriptions: List<Subscription>): String {
        val header = "Name,Cost,Currency,BillingPeriod,Category,Strategy,ActiveDays,PauseDays,StartDate,CompletedBreaks,EarnedSavings,WatchlistNotes"
        val rows = subscriptions.map { sub ->
            listOf(
                csvCell(sub.name),
                sub.cost.toString(),
                csvCell(sub.currency),
                csvCell(sub.billingPeriod.displayName),
                csvCell(sub.category.displayName),
                csvCell(sub.strategy.name),
                sub.activeDays.toString(),
                sub.blackoutDays.toString(),
                sub.startDate.toString(),
                sub.completedBreaksCount.toString(),
                String.format(Locale.US, "%.2f", sub.earnedSavingsTotal),
                csvCell(sub.notes)
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n") + "\n"
    }

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
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
            require(i < n && inner[i] == '"') { "Expected JSON string" }
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
            error("Unterminated JSON string")
        }

        while (i < n) {
            skipWs()
            if (i >= n) break
            val key = readString()
            while (i < n && inner[i].isWhitespace()) i++
            require(i < n && inner[i] == ':') { "Expected ':' after key $key" }
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
