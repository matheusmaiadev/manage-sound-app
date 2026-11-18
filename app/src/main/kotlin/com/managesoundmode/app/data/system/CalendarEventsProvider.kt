package com.managesoundmode.app.data.system

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.managesoundmode.app.domain.model.CalendarEventInstance
import com.managesoundmode.app.domain.model.DeviceCalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Abstração desacoplada para leitura do provedor de eventos de calendário nativo do Android
 * (`CalendarContract`) e simulação em memória para testes determinísticos.
 */
interface CalendarEventsProvider {
  val calendarChanges: Flow<Unit>
  fun hasReadCalendarPermission(): Boolean
  suspend fun queryVisibleCalendars(): List<DeviceCalendarInfo>
  suspend fun queryEventInstances(
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    calendarIds: Set<Long> = emptySet(),
  ): List<CalendarEventInstance>
}

class AndroidCalendarEventsProvider(
  private val appContext: Context,
) : CalendarEventsProvider {

  companion object {
    private const val TAG = "CalendarEventsProvider"
  }

  override val calendarChanges: Flow<Unit> = callbackFlow {
    trySend(Unit)
    if (!hasReadCalendarPermission()) {
      awaitClose { }
      return@callbackFlow
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        trySend(Unit)
      }
    }

    runCatching {
      appContext.contentResolver.registerContentObserver(
        CalendarContract.CONTENT_URI,
        true,
        observer,
      )
    }.onFailure { error ->
      Log.w(TAG, "Falha ao registrar ContentObserver para CalendarContract", error)
    }

    awaitClose {
      runCatching {
        appContext.contentResolver.unregisterContentObserver(observer)
      }.onFailure { error ->
        Log.w(TAG, "Falha ao desregistrar ContentObserver para CalendarContract", error)
      }
    }
  }

  override fun hasReadCalendarPermission(): Boolean =
    ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED

  override suspend fun queryVisibleCalendars(): List<DeviceCalendarInfo> = withContext(Dispatchers.IO) {
    if (!hasReadCalendarPermission()) return@withContext emptyList()

    val projection = arrayOf(
      CalendarContract.Calendars._ID,
      CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
      CalendarContract.Calendars.ACCOUNT_NAME,
      CalendarContract.Calendars.VISIBLE,
    )

    runCatching {
      val calendars = mutableListOf<DeviceCalendarInfo>()
      appContext.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        null,
        null,
        "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
        val visibleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idIdx)
          val displayName = cursor.getString(nameIdx) ?: ""
          val accountName = cursor.getString(accountIdx) ?: ""
          val visible = cursor.getInt(visibleIdx) == 1
          calendars += DeviceCalendarInfo(
            id = id,
            displayName = displayName,
            accountName = accountName,
            visible = visible,
          )
        }
      }
      calendars
    }.onFailure { error ->
      Log.w(TAG, "Falha ao consultar agendas visíveis do dispositivo", error)
    }.getOrDefault(emptyList())
  }

  override suspend fun queryEventInstances(
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    calendarIds: Set<Long>,
  ): List<CalendarEventInstance> = withContext(Dispatchers.IO) {
    if (!hasReadCalendarPermission()) return@withContext emptyList()
    if (rangeEndMillis <= rangeStartMillis) return@withContext emptyList()

    val projection = arrayOf(
      CalendarContract.Instances.EVENT_ID,
      CalendarContract.Instances.CALENDAR_ID,
      CalendarContract.Instances.TITLE,
      CalendarContract.Instances.DESCRIPTION,
      CalendarContract.Instances.BEGIN,
      CalendarContract.Instances.END,
      CalendarContract.Instances.AVAILABILITY,
      CalendarContract.Instances.ALL_DAY,
      CalendarContract.Instances.STATUS,
      CalendarContract.Instances.SELF_ATTENDEE_STATUS,
    )

    val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(uriBuilder, rangeStartMillis)
    ContentUris.appendId(uriBuilder, rangeEndMillis)
    val uri = uriBuilder.build()

    runCatching {
      val instances = mutableListOf<CalendarEventInstance>()
      appContext.contentResolver.query(
        uri,
        projection,
        null,
        null,
        "${CalendarContract.Instances.BEGIN} ASC",
      )?.use { cursor ->
        val eventIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val calendarIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
        val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
        val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val availIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.AVAILABILITY)
        val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val statusIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.STATUS)
        val attendeeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)

        while (cursor.moveToNext()) {
          val eventId = cursor.getLong(eventIdIdx)
          val calendarId = cursor.getLong(calendarIdIdx)
          val title = cursor.getString(titleIdx) ?: ""
          val description = cursor.getString(descIdx) ?: ""
          val begin = cursor.getLong(beginIdx)
          val end = cursor.getLong(endIdx)
          val availability = cursor.getInt(availIdx)
          val allDay = cursor.getInt(allDayIdx) == 1
          val status = cursor.getInt(statusIdx)
          val attendeeStatus = cursor.getInt(attendeeIdx)

          val isCancelledOrDeclined = status == CalendarContract.Events.STATUS_CANCELED ||
            attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED

          instances += CalendarEventInstance(
            eventId = eventId,
            calendarId = calendarId,
            title = title,
            description = description,
            beginMillis = begin,
            endMillis = end,
            availability = availability,
            allDay = allDay,
            isCancelledOrDeclined = isCancelledOrDeclined,
          )
        }
      }

      if (calendarIds.isNotEmpty()) {
        instances.filter { it.calendarId in calendarIds }
      } else {
        instances
      }
    }.onFailure { error ->
      Log.w(TAG, "Falha ao consultar instâncias de eventos de calendário", error)
    }.getOrDefault(emptyList())
  }
}

class FakeCalendarEventsProvider(
  var permissionGranted: Boolean = true,
  initialCalendars: List<DeviceCalendarInfo> = emptyList(),
  initialEvents: List<CalendarEventInstance> = emptyList(),
) : CalendarEventsProvider {
  val calendars = initialCalendars.toMutableList()
  val events = initialEvents.toMutableList()
  private val _changeCounter = MutableStateFlow(0L)
  override val calendarChanges: Flow<Unit> = _changeCounter.map { Unit }

  fun notifyChanged() {
    _changeCounter.value += 1L
  }

  override fun hasReadCalendarPermission(): Boolean = permissionGranted

  override suspend fun queryVisibleCalendars(): List<DeviceCalendarInfo> {
    if (!permissionGranted) return emptyList()
    return calendars.filter { it.visible }
  }

  override suspend fun queryEventInstances(
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    calendarIds: Set<Long>,
  ): List<CalendarEventInstance> {
    if (!permissionGranted) return emptyList()
    return events.filter { event ->
      (calendarIds.isEmpty() || event.calendarId in calendarIds) &&
        event.beginMillis < rangeEndMillis &&
        event.endMillis > rangeStartMillis
    }
  }
}
