package com.sunodo.app.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/**
 * docs/blueprint.md's OSActionBridge module: native Android Intents only —
 * ACTION_INSERT for Calendar, ACTION_SEND for reply/reminder handoff. No
 * WhatsApp API, no proprietary integration, no extra permission beyond what
 * these two stable, long-documented Intent actions already grant implicitly.
 *
 * ACTION_INSERT against CalendarContract.Events.CONTENT_URI opens the
 * calendar app's own "new event" screen for the user to confirm — it does
 * NOT insert a row directly into the calendar provider, which is why this
 * needs no READ_CALENDAR/WRITE_CALENDAR permission. That trade-off (one
 * extra confirmation tap, zero permissions) matches the rest of this app's
 * permission stance.
 */
object OSActionBridge {

    /** Opens the calendar app's "new event" screen pre-filled as an all-day event on `dueEpochDay`. */
    fun addToCalendar(context: Context, title: String, dueEpochDay: Long): Boolean {
        val startOfDay = LocalDate.ofEpochDay(dueEpochDay).atStartOfDay(ZoneId.systemDefault())
        val beginMillis = startOfDay.toInstant().toEpochMilli()
        val endMillis = startOfDay.plusDays(1).toInstant().toEpochMilli()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        return startActivitySafely(context, intent)
    }

    /**
     * Hands `text` off to whatever app the user picks via the system share
     * sheet — a reminders app, a notes app, or straight back into WhatsApp
     * as a reply. This is the same mechanism for both jobs on purpose:
     * SunoDo doesn't need to know which reminders app exists on this
     * device, only that the user has one they'll recognize in the chooser.
     */
    fun shareText(context: Context, text: String, chooserTitle: String): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle)
        return startActivitySafely(context, chooser)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun startActivitySafely(context: Context, intent: Intent): Boolean = try {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
