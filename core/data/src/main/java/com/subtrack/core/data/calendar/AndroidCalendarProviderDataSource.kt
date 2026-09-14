package com.subtrack.core.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.subtrack.core.common.dispatcher.IoDispatcher
import com.subtrack.core.domain.model.CalendarOccurrence
import com.subtrack.core.domain.model.DeviceCalendar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

class AndroidCalendarProviderDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CalendarProviderDataSource {
    private val resolver: ContentResolver
        get() = context.contentResolver

    override fun isReadPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    override suspend fun getCalendars(): List<DeviceCalendar> = withContext(ioDispatcher) {
        checkPermission()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val result = mutableListOf<DeviceCalendar>()
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val typeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            while (cursor.moveToNext()) {
                result += DeviceCalendar(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(nameIndex).orEmpty().ifBlank { "Unnamed calendar" },
                    accountName = cursor.getString(accountIndex).orEmpty(),
                    accountType = cursor.getString(typeIndex).orEmpty()
                )
            }
        }
        result.distinctBy { it.id }
    }

    override suspend fun getOccurrences(
        selectedCalendarIds: Set<Long>,
        windowStart: Instant,
        windowEndExclusive: Instant
    ): List<CalendarOccurrence> = withContext(ioDispatcher) {
        checkPermission()
        if (selectedCalendarIds.isEmpty()) return@withContext emptyList()

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, windowStart.toEpochMilli())
        ContentUris.appendId(uriBuilder, windowEndExclusive.toEpochMilli())
        val projection = arrayOf(
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.EVENT_TIMEZONE
        )
        val placeholders = selectedCalendarIds.joinToString(",") { "?" }
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
        val args = selectedCalendarIds.sorted().map(Long::toString).toTypedArray()
        val result = mutableListOf<CalendarOccurrence>()

        resolver.query(
            uriBuilder.build(),
            projection,
            selection,
            args,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val calendarIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val eventIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val statusIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.STATUS)
            val attendeeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
            val timeZoneIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_TIMEZONE)

            while (cursor.moveToNext() && result.size < MAX_OCCURRENCES) {
                val status = if (cursor.isNull(statusIndex)) null else cursor.getInt(statusIndex)
                val attendeeStatus = if (cursor.isNull(attendeeIndex)) null else cursor.getInt(attendeeIndex)
                if (status == CalendarContract.Events.STATUS_CANCELED ||
                    status == CalendarContract.Events.STATUS_TENTATIVE ||
                    attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                ) continue

                val calendarId = cursor.getLong(calendarIndex)
                val eventId = cursor.getLong(eventIndex)
                val beginMillis = cursor.getLong(beginIndex)
                val endMillis = cursor.getLong(endIndex).coerceAtLeast(beginMillis + 1)
                val allDay = cursor.getInt(allDayIndex) != 0
                val providerTimeZone = cursor.getString(timeZoneIndex)
                val zone = providerTimeZone.toZoneIdOrDefault()
                val startInstant = Instant.ofEpochMilli(beginMillis)
                val endInstant = Instant.ofEpochMilli(endMillis)
                val normalizedDates = normalizeOccurrenceDates(startInstant, endInstant, allDay, zone)
                result += CalendarOccurrence(
                    identity = "$calendarId:$eventId:$beginMillis",
                    calendarId = calendarId,
                    eventId = eventId,
                    title = cursor.getString(titleIndex).orEmpty(),
                    startDate = normalizedDates.startDate,
                    endDateExclusive = normalizedDates.endDateExclusive,
                    isAllDay = allDay,
                    startInstant = if (allDay) null else startInstant,
                    endInstant = if (allDay) null else endInstant,
                    timeZoneId = if (allDay) null else zone.id
                )
            }
        }
        result.distinctBy { it.identity }
    }

    override fun observeChanges(): Flow<Unit> = callbackFlow {
        if (!isReadPermissionGranted()) {
            close()
            return@callbackFlow
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observer)
        resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    private fun checkPermission() {
        if (!isReadPermissionGranted()) throw SecurityException("READ_CALENDAR is not granted")
    }

    private fun String?.toZoneIdOrDefault(): ZoneId = try {
        if (isNullOrBlank()) ZoneId.systemDefault() else ZoneId.of(this)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }

    private companion object {
        const val MAX_OCCURRENCES = 500
    }
}

internal data class NormalizedOccurrenceDates(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate
)

internal fun normalizeOccurrenceDates(
    startInstant: Instant,
    endInstant: Instant,
    allDay: Boolean,
    zone: ZoneId
): NormalizedOccurrenceDates {
    val startDate: LocalDate
    val endDateExclusive: LocalDate
    if (allDay) {
        startDate = startInstant.atZone(ZoneOffset.UTC).toLocalDate()
        endDateExclusive = endInstant.atZone(ZoneOffset.UTC).toLocalDate()
            .coerceAtLeast(startDate.plusDays(1))
    } else {
        val start = startInstant.atZone(zone)
        val end = endInstant.atZone(zone)
        startDate = start.toLocalDate()
        endDateExclusive = if (end.toLocalTime() == LocalTime.MIDNIGHT) {
            end.toLocalDate()
        } else {
            end.toLocalDate().plusDays(1)
        }.coerceAtLeast(startDate.plusDays(1))
    }
    return NormalizedOccurrenceDates(startDate, endDateExclusive)
}
