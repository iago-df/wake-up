package com.iagodf.wakeup.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class DayUsage(val minutes: Int = 0, val unlocks: Int = 0)

/** total de cada dia y, ademas, los minutos de movil de cada tramo de media hora (48 por dia) */
class UsageData(val days: Map<LocalDate, DayUsage>, val slots: Map<LocalDate, IntArray>)

const val SLOT_MIN = 30
const val SLOTS_PER_DAY = 24 * 60 / SLOT_MIN

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

    fun read(ctx: Context, days: Int = 8): UsageData {
        if (!hasAccess(ctx)) return UsageData(emptyMap(), emptyMap())
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

        val ms = HashMap<LocalDate, LongArray>()
        val unlocks = HashMap<LocalDate, Int>()
        fun day(ts: Long) = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

        // reparte un rato de uso entre los tramos de media hora que toca
        fun add(a: Long, b: Long) {
            var s = maxOf(a, from)
            val end = minOf(b, now)
            while (s < end) {
                val z = Instant.ofEpochMilli(s).atZone(zone)
                val minOfDay = z.hour * 60 + z.minute
                val into = ((minOfDay % SLOT_MIN) * 60 + z.second) * 1000L + z.nano / 1_000_000
                val cut = minOf(end, s + (SLOT_MIN * 60_000L - into))
                val arr = ms.getOrPut(z.toLocalDate()) { LongArray(SLOTS_PER_DAY) }
                arr[minOfDay / SLOT_MIN] += cut - s
                s = cut
            }
        }
        fun unlock(ts: Long) {
            if (ts >= from) unlocks[day(ts)] = (unlocks[day(ts)] ?: 0) + 1
        }

        // si el movil no tiene bloqueo no hay eventos de keyguard: cuento pantalla encendida
        var locked = hasKeyguard
        var since = -1L
        for (ev in list) {
            when (ev.type) {
                SCREEN_ON -> {
                    if (!locked && since < 0) { since = ev.ts; if (!hasKeyguard) unlock(ev.ts) }
                }
                KEYGUARD_HIDDEN -> {
                    val was = locked
                    locked = false
                    if (since < 0) since = ev.ts
                    if (was) unlock(ev.ts)
                }
                KEYGUARD_SHOWN -> {
                    locked = true
                    if (since >= 0) { add(since, ev.ts); since = -1 }
                }
                SCREEN_OFF, SHUTDOWN -> {
                    if (since >= 0) { add(since, ev.ts); since = -1 }
                }
            }
        }
        if (since >= 0) add(since, now)

        val outDays = HashMap<LocalDate, DayUsage>()
        val outSlots = HashMap<LocalDate, IntArray>()
        var d = first
        while (!d.isAfter(today)) {
            val arr = ms[d] ?: LongArray(SLOTS_PER_DAY)
            outDays[d] = DayUsage((arr.sum() / 60_000L).toInt(), unlocks[d] ?: 0)
            outSlots[d] = IntArray(SLOTS_PER_DAY) { (arr[it] / 60_000f).roundToInt().coerceIn(0, SLOT_MIN) }
            d = d.plusDays(1)
        }
        return UsageData(outDays, outSlots)
    }
}
