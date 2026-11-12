package com.iagodf.wakeup.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** estado de toda la app + las cuentas del tiempo libre */
class AppState(ctx: Context) {
    private val app = ctx.applicationContext
    val store = Store(app)

    var settings by mutableStateOf(store.settings()); private set
    var projects by mutableStateOf(store.projects()); private set
    var attended by mutableStateOf(store.attended()); private set
    var usage by mutableStateOf(store.history()); private set
    var server by mutableStateOf(store.server()); private set
    var hasAccess by mutableStateOf(UsageReader.hasAccess(app)); private set
    var now: LocalDateTime by mutableStateOf(LocalDateTime.now()); private set

    val today: LocalDate get() = now.toLocalDate()

    suspend fun refresh() {
        now = LocalDateTime.now()
        hasAccess = UsageReader.hasAccess(app)
        if (!hasAccess) return
        val merged = withContext(Dispatchers.IO) { syncUsage(app, store) }
        usage = merged
    }

    fun update(s: Settings) { settings = s; store.save(s) }
    fun update(s: ServerCfg) { server = s; store.save(s) }

    fun saveProjects(list: List<Project>) { projects = list; store.save(list) }
    fun upsert(p: Project) = saveProjects(if (projects.any { it.id == p.id }) projects.map { if (it.id == p.id) p else it } else projects + p)
    fun delete(id: Long) = saveProjects(projects.filter { it.id != id })
    fun move(id: Long, dir: Int) {
        val i = projects.indexOfFirst { it.id == id }
        val j = i + dir
        if (i < 0 || j !in projects.indices) return
        val l = projects.toMutableList()
        l[i] = projects[j]; l[j] = projects[i]
        saveProjects(l)
    }

    fun toggleAttended(block: Block, date: LocalDate) {
        val k = block.key(date)
        attended = if (k in attended) attended - k else attended + k
        store.saveAttended(attended)
    }

    // ---- cuentas ----

    fun phoneMinutes(d: LocalDate): Int = usage[d]?.minutes ?: 0
    fun unlocks(d: LocalDate): Int = usage[d]?.unlocks ?: 0
    fun classMinutes(d: LocalDate): Int = Schedule.classMinutes(d, attended, now)

    /** tiempo libre del dia = 24 h - sueño - basicas - comidas - clases */
    fun freeMinutes(d: LocalDate): Int = (24 * 60 - settings.busyMinutes - classMinutes(d)).coerceAtLeast(30)

    fun pct(d: LocalDate): Int = (phoneMinutes(d) * 100f / freeMinutes(d)).roundToInt()

    val week: List<LocalDate> get() = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val weekPhone: Int get() = week.sumOf { phoneMinutes(it) }
    val weekFree: Int get() = week.sumOf { freeMinutes(it) }
    val weekClass: Int get() = week.sumOf { classMinutes(it) }
    val weekPct: Int get() = (weekPhone * 100f / weekFree.coerceAtLeast(1)).roundToInt()

    /** media diaria de movil con los dias que tienen datos */
    val dailyAvg: Float get() {
        val days = week.map { phoneMinutes(it) }.filter { it > 0 }
        return if (days.isEmpty()) 0f else days.average().toFloat()
    }

    val age: Int get() = (today.year - settings.birthYear).coerceIn(0, 79)

    /** años mirando una pantalla de aqui a los 80 si sigo a este ritmo */
    val lifeYears: Float get() = (80 - age) * dailyAvg / (24f * 60f)

    /**
     * lo que pudo ser: las horas de movil de la semana se reparten entre los proyectos
     * por prioridad, hasta llegar a las horas semanales de cada uno
     */
    fun lostHours(): Map<Long, Int> {
        var left = weekPhone / 60f
        val out = LinkedHashMap<Long, Int>()
        for (p in projects) {
            val h = minOf(p.hoursPerWeek.toFloat(), left)
            out[p.id] = h.roundToInt()
            left -= h
        }
        return out
    }

    companion object {
        /** lee el uso, lo mezcla con lo guardado y lo guarda. lo usa tambien el widget */
        fun syncUsage(ctx: Context, store: Store): Map<LocalDate, DayUsage> {
            val fresh = UsageReader.read(ctx)
            val merged = HashMap(store.history())
            val today = LocalDate.now()
            fresh.forEach { (d, u) ->
                val old = merged[d]
                // hoy siempre el dato nuevo; dias pasados me quedo con el mayor por si el sistema ya borro eventos
                merged[d] = if (d == today || old == null) u else DayUsage(maxOf(old.minutes, u.minutes), maxOf(old.unlocks, u.unlocks))
            }
            // no hace falta guardar mas de 60 dias
            val cut = today.minusDays(60)
            val trimmed = merged.filterKeys { !it.isBefore(cut) }
            store.saveHistory(trimmed)
            return trimmed
        }
    }
}

fun fmtHM(min: Int): String = if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"

/** 3,8 h  /  5 h */
fun fmtH(min: Int): String {
    val h = min / 60f
    val r = (h * 10).roundToInt() / 10f
    return (if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString().replace('.', ',')) + " h"
}
