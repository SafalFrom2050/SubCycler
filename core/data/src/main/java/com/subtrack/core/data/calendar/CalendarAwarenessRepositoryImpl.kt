package com.subtrack.core.data.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarAwarenessSettings
import com.subtrack.core.domain.model.CalendarSuggestionDismissal
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.repository.CalendarAwarenessRepository
import com.subtrack.core.domain.usecase.GetCalendarAwareSuggestionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

val Context.calendarAwarenessDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "calendar_awareness")

@Singleton
class CalendarAwarenessRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val provider: CalendarProviderDataSource,
    private val suggestionsUseCase: GetCalendarAwareSuggestionsUseCase
) : CalendarAwarenessRepository {

    override val settings: Flow<CalendarAwarenessSettings> = safeData().map(::settingsFrom)
    override val suggestions: Flow<List<CalendarAwareSuggestion>> = safeData().map { preferences ->
        preferences[Keys.SUGGESTIONS].orEmpty().mapNotNull(::decodeSuggestion)
            .sortedWith(
                compareBy<CalendarAwareSuggestion> { it.decisionDate }
                    .thenByDescending { it.confidence }
                    .thenBy { it.identity.subscriptionId }
                    .thenBy { it.identity.occurrenceIdentity }
            )
    }

    override suspend fun isReadPermissionGranted(): Boolean = provider.isReadPermissionGranted()

    override suspend fun getAccessibleCalendars(): List<DeviceCalendar> {
        if (!provider.isReadPermissionGranted()) return emptyList()
        return provider.getCalendars()
    }

    override suspend fun enable(selectedCalendarIds: Set<Long>) {
        require(selectedCalendarIds.isNotEmpty())
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = true
            preferences[Keys.SELECTED_IDS] = selectedCalendarIds.map(Long::toString).toSet()
            preferences[Keys.GENERATION] = (preferences[Keys.GENERATION] ?: 0L) + 1L
            preferences.remove(Keys.ISSUE)
            preferences.remove(Keys.SUGGESTIONS)
        }
    }

    override suspend fun updateSelection(selectedCalendarIds: Set<Long>) {
        if (selectedCalendarIds.isEmpty()) {
            disable(CalendarAccessIssue.NO_ACCESSIBLE_CALENDARS)
            return
        }
        dataStore.edit { preferences ->
            if (preferences[Keys.ENABLED] != true) return@edit
            preferences[Keys.SELECTED_IDS] = selectedCalendarIds.map(Long::toString).toSet()
            preferences[Keys.GENERATION] = (preferences[Keys.GENERATION] ?: 0L) + 1L
            preferences.remove(Keys.ISSUE)
            preferences.remove(Keys.SUGGESTIONS)
        }
    }

    override suspend fun disable(issue: CalendarAccessIssue?) {
        dataStore.edit { preferences ->
            // One atomic edit makes OFF visible before any stale generation can commit.
            preferences[Keys.ENABLED] = false
            preferences[Keys.GENERATION] = (preferences[Keys.GENERATION] ?: 0L) + 1L
            preferences.remove(Keys.SELECTED_IDS)
            preferences.remove(Keys.SUGGESTIONS)
            preferences.remove(Keys.DISMISSALS)
            preferences.remove(Keys.NOTIFIED_IDENTITIES)
            preferences.remove(Keys.LAST_NOTIFICATION_AT)
            if (issue == null) preferences.remove(Keys.ISSUE) else preferences[Keys.ISSUE] = issue.name
        }
    }

    override suspend fun refresh(
        cycleStatuses: List<CycleStatus>,
        today: LocalDate
    ): List<CalendarAwareSuggestion> {
        var before = settings.first()
        if (!before.isEnabled) return emptyList()
        if (!provider.isReadPermissionGranted()) {
            disable(CalendarAccessIssue.PERMISSION_REVOKED)
            return emptyList()
        }

        val accessibleCalendars = try {
            provider.getCalendars()
        } catch (_: SecurityException) {
            disable(CalendarAccessIssue.PERMISSION_REVOKED)
            return emptyList()
        } catch (_: Exception) {
            disable(CalendarAccessIssue.PROVIDER_UNAVAILABLE)
            return emptyList()
        }
        val validSelection = before.selectedCalendarIds intersect accessibleCalendars.mapTo(mutableSetOf()) { it.id }
        if (validSelection.isEmpty()) {
            disable(CalendarAccessIssue.NO_ACCESSIBLE_CALENDARS)
            return emptyList()
        }
        if (validSelection != before.selectedCalendarIds) {
            updateSelection(validSelection)
            before = settings.first()
        }

        val zone = ZoneId.systemDefault()
        val windowStart = today.atStartOfDay(zone).toInstant()
        val windowEnd = today.plusDays(31).atStartOfDay(zone).toInstant()
        val occurrences = try {
            provider.getOccurrences(validSelection, windowStart, windowEnd)
        } catch (_: SecurityException) {
            disable(CalendarAccessIssue.PERMISSION_REVOKED)
            return emptyList()
        } catch (_: Exception) {
            disable(CalendarAccessIssue.PROVIDER_UNAVAILABLE)
            return emptyList()
        }

        val stored = safeData().first()
        val dismissals = stored[Keys.DISMISSALS].orEmpty().mapNotNull(::decodeDismissal)
        val freshDismissals = dismissals.filter { it.occurrenceEndDateExclusive.isAfter(today) }
        val derived = suggestionsUseCase(cycleStatuses, occurrences, freshDismissals, today)
        var committed = false
        dataStore.edit { current ->
            if (current[Keys.ENABLED] == true && (current[Keys.GENERATION] ?: 0L) == before.generation) {
                current[Keys.SUGGESTIONS] = derived.map(::encodeSuggestion).toSet()
                current[Keys.DISMISSALS] = freshDismissals.map(::encodeDismissal).toSet()
                committed = true
            }
        }
        return if (committed) derived else emptyList()
    }

    override suspend fun dismiss(identity: CalendarSuggestionIdentity) {
        dataStore.edit { preferences ->
            if (preferences[Keys.ENABLED] != true) return@edit
            val currentSuggestions = preferences[Keys.SUGGESTIONS].orEmpty().mapNotNull(::decodeSuggestion)
            val suggestion = currentSuggestions.firstOrNull { it.identity == identity } ?: return@edit
            val dismissal = CalendarSuggestionDismissal(identity, suggestion.occurrenceEndDateExclusive)
            preferences[Keys.DISMISSALS] = preferences[Keys.DISMISSALS].orEmpty() + encodeDismissal(dismissal)
            preferences[Keys.SUGGESTIONS] = currentSuggestions
                .filterNot { it.identity == identity }
                .map(::encodeSuggestion)
                .toSet()
        }
    }

    override suspend fun claimNotification(identity: CalendarSuggestionIdentity, now: Instant): Boolean {
        var claimed = false
        dataStore.edit { preferences ->
            if (preferences[Keys.ENABLED] != true) return@edit
            val key = encodeIdentity(identity)
            val notified = preferences[Keys.NOTIFIED_IDENTITIES].orEmpty()
            val last = preferences[Keys.LAST_NOTIFICATION_AT]?.let(Instant::ofEpochMilli)
            val withinCap = last != null && Duration.between(last, now) < Duration.ofHours(24)
            if (key !in notified && !withinCap) {
                preferences[Keys.NOTIFIED_IDENTITIES] = notified + key
                preferences[Keys.LAST_NOTIFICATION_AT] = now.toEpochMilli()
                claimed = true
            }
        }
        return claimed
    }

    override fun observeCalendarChanges(): Flow<Unit> = provider.observeChanges()

    private fun safeData(): Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    private fun settingsFrom(preferences: Preferences): CalendarAwarenessSettings = CalendarAwarenessSettings(
        isEnabled = preferences[Keys.ENABLED] ?: false,
        selectedCalendarIds = preferences[Keys.SELECTED_IDS].orEmpty().mapNotNull(String::toLongOrNull).toSet(),
        generation = preferences[Keys.GENERATION] ?: 0L,
        accessIssue = preferences[Keys.ISSUE]?.let { runCatching { CalendarAccessIssue.valueOf(it) }.getOrNull() }
    )

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val SELECTED_IDS = stringSetPreferencesKey("selected_calendar_ids")
        val GENERATION = longPreferencesKey("generation")
        val ISSUE = stringPreferencesKey("access_issue")
        val SUGGESTIONS = stringSetPreferencesKey("derived_suggestions")
        val DISMISSALS = stringSetPreferencesKey("dismissals")
        val NOTIFIED_IDENTITIES = stringSetPreferencesKey("notified_identities")
        val LAST_NOTIFICATION_AT = longPreferencesKey("last_notification_at")
    }

    private fun encodeSuggestion(value: CalendarAwareSuggestion): String = listOf(
        value.identity.subscriptionId.toString(),
        encode(value.identity.occurrenceIdentity),
        value.identity.suggestionType.name,
        encode(value.subscriptionName),
        value.decisionDate.toString(),
        value.occurrenceEndDateExclusive.toString(),
        value.confidence.toString()
    ).joinToString("|")

    private fun decodeSuggestion(raw: String): CalendarAwareSuggestion? = runCatching {
        val parts = raw.split('|')
        val identity = CalendarSuggestionIdentity(
            subscriptionId = parts[0].toLong(),
            occurrenceIdentity = decode(parts[1]),
            suggestionType = CalendarSuggestionType.valueOf(parts[2])
        )
        CalendarAwareSuggestion(
            identity = identity,
            subscriptionName = decode(parts[3]),
            decisionDate = LocalDate.parse(parts[4]),
            occurrenceEndDateExclusive = LocalDate.parse(parts[5]),
            confidence = parts[6].toInt()
        )
    }.getOrNull()

    private fun encodeDismissal(value: CalendarSuggestionDismissal): String =
        "${encodeIdentity(value.identity)}|${value.occurrenceEndDateExclusive}"

    private fun decodeDismissal(raw: String): CalendarSuggestionDismissal? = runCatching {
        val parts = raw.split('|')
        CalendarSuggestionDismissal(decodeIdentity(parts[0]), LocalDate.parse(parts[1]))
    }.getOrNull()

    private fun encodeIdentity(value: CalendarSuggestionIdentity): String = listOf(
        value.subscriptionId.toString(),
        encode(value.occurrenceIdentity),
        value.suggestionType.name
    ).joinToString("~")

    private fun decodeIdentity(raw: String): CalendarSuggestionIdentity {
        val parts = raw.split('~')
        return CalendarSuggestionIdentity(parts[0].toLong(), decode(parts[1]), CalendarSuggestionType.valueOf(parts[2]))
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8
    )
}
