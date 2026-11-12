package com.iagodf.wakeup.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayUsage(val minutes: Int = 0, val unlocks: Int = 0)

/** lee del sistema cuanto tiempo ha estado el movil desbloqueado y cuantas veces lo cogi */
object UsageReader {

    fun hasAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private const val SCREEN_ON = 15      // SCREEN_INTERACTIVE
    private const val SCREEN_OFF = 16     // SCREEN_NON_INTERACTIVE
    private const val KEYGUARD_SHOWN = 17
    private const val KEYGUARD_HIDDEN = 18
    private const val SHUTDOWN = 26

    private class Ev(val type: Int, val ts: Long)

    fun read(ctx: Context, days: Int = 8): Map<LocalDate, DayUsage> {
        if (!hasAccess(ctx)) return emptyMap()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val first = today.minusDays((days - 1).toLong())
        val from = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // empiezo medio dia antes para saber en que estado estaba el movil a las 00:00
        val events = usm.queryEvents(from - 12 * 3600_000L, now)
        val list = ArrayList<Ev>()
        val e = UsageEvents.Event()
        var hasKeyguard = false
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                SCREEN_ON, SCREEN_OFF, SHUTDOWN -> list.add(Ev(e.eventType, e.timeStamp))
                KEYGUARD_SHOWN, KEYGUARD_HIDDEN -> { hasKeyguard = true; list.add(Ev(e.eventType, e.timeStamp)) }
            }
        }

        val ms = HashMap<LocalDate, Long>()
        val unlocks = HashMap<LocalDate, Int>()
        fun day(ts: Long) = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        fun add(a: Long, b: Long) {
            var s = maxOf(a, from)
            val end = minOf(b, now)
            while (s < end) {
                val d = day(s)
                val next = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val cut = minOf(end, next)
                ms[d] = (ms[d] ?: 0L) + (cut - s)
                s = cut
            }
        }
        fun unlock(ts: Long) {
            if (ts >= from) unlocks[day(ts)] = (unlocks[day(ts)] ?: 0) + 1
        }

        // si el movil no tiene bloqueo no hay eventos de keyguard: cuento pantalla encendida
        var locked = hasKeyguard
        var screenOn = false
        var since = -1L
        for (ev in list) {
            when (ev.type) {
                SCREEN_ON -> {
                    screenOn = true
                    if (!locked && since < 0) { since = ev.ts; if (!hasKeyguard) unlock(ev.ts) }
                }
                KEYGUARD_HIDDEN -> {
                    val was = locked
                    locked = false
                    if (since < 0) { since = ev.ts; screenOn = true }
                    if (was) unlock(ev.ts)
                }
                KEYGUARD_SHOWN -> {
                    locked = true
                    if (since >= 0) { add(since, ev.ts); since = -1 }
                }
                SCREEN_OFF, SHUTDOWN -> {
                    screenOn = false
                    if (since >= 0) { add(since, ev.ts); since = -1 }
                }
            }
        }
        if (since >= 0) add(since, now)

        val out = HashMap<LocalDate, DayUsage>()
        var d = first
        while (!d.isAfter(today)) {
            out[d] = DayUsage(((ms[d] ?: 0L) / 60_000L).toInt(), unlocks[d] ?: 0)
            d = d.plusDays(1)
        }
        return out
    }
}
