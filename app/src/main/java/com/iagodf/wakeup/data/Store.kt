package com.iagodf.wakeup.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Settings(
    val sleepH: Float = 8f,
    val basicsH: Float = 2f,
    val mealsH: Float = 2f,
    val birthYear: Int = 2006,
) {
    val busyMinutes: Int get() = ((sleepH + basicsH + mealsH) * 60).toInt()
}

data class Project(
    val id: Long,
    val name: String,
    val hoursPerWeek: Int,
    val progress: Int = 0,
    /** que paisaje lleva la tarjeta */
    val look: Int = 0,
)

data class ServerCfg(val url: String = "", val token: String = "", val name: String = "") {
    val ready: Boolean get() = url.isNotBlank()
}

/** todo se guarda en local en SharedPreferences, no hace falta mas */
class Store(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("wakeup", Context.MODE_PRIVATE)

    fun settings() = Settings(
        sp.getFloat("sleep", 8f), sp.getFloat("basics", 2f), sp.getFloat("meals", 2f), sp.getInt("birth", 2006),
    )

    fun save(s: Settings) = sp.edit()
        .putFloat("sleep", s.sleepH).putFloat("basics", s.basicsH).putFloat("meals", s.mealsH).putInt("birth", s.birthYear)
        .apply()

    fun projects(): List<Project> {
        val raw = sp.getString("projects", null) ?: return DEFAULT_PROJECTS
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Project(o.getLong("id"), o.getString("name"), o.getInt("h"), o.optInt("p"), o.optInt("look"))
        }
    }

    fun save(list: List<Project>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("h", it.hoursPerWeek).put("p", it.progress).put("look", it.look)) }
        sp.edit().putString("projects", arr.toString()).apply()
    }

    fun attended(): Set<String> = sp.getStringSet("attended", emptySet())!!.toSet()
    fun saveAttended(s: Set<String>) = sp.edit().putStringSet("attended", s).apply()

    /** el sistema solo guarda unos dias de eventos, asi que voy guardando el total de cada dia */
    fun history(): Map<LocalDate, DayUsage> {
        val o = JSONObject(sp.getString("history", "{}")!!)
        val out = HashMap<LocalDate, DayUsage>()
        for (k in o.keys()) {
            val a = o.getJSONArray(k)
            out[LocalDate.parse(k)] = DayUsage(a.getInt(0), a.getInt(1))
        }
        return out
    }

    fun saveHistory(h: Map<LocalDate, DayUsage>) {
        val o = JSONObject()
        h.forEach { (d, u) -> o.put(d.toString(), JSONArray().put(u.minutes).put(u.unlocks)) }
        sp.edit().putString("history", o.toString()).apply()
    }

    fun server() = ServerCfg(sp.getString("srv_url", "")!!, sp.getString("srv_token", "")!!, sp.getString("srv_name", "")!!)
    fun save(s: ServerCfg) = sp.edit().putString("srv_url", s.url).putString("srv_token", s.token).putString("srv_name", s.name).apply()

    companion object {
        val DEFAULT_PROJECTS = listOf(
            Project(1, "OneDay", 6, 0, 0),
            Project(2, "Servidor", 4, 0, 1),
            Project(3, "Lluvia", 3, 0, 2),
        )
    }
}
