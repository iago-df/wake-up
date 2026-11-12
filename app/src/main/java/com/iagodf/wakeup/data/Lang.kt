package com.iagodf.wakeup.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlin.math.roundToInt

/**
 * idioma de la app: ingles por defecto, se cambia a español en ajustes.
 * los textos van en el propio codigo con tr("español", "english"), asi el widget
 * (que no es compose) usa lo mismo
 */
object Lang {
    var en by mutableStateOf(true); private set

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("wakeup", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        en = prefs(ctx).getString("lang", "en") != "es"
    }

    fun set(ctx: Context, english: Boolean) {
        en = english
        prefs(ctx).edit().putString("lang", if (english) "en" else "es").apply()
    }

    val locale: Locale get() = if (en) Locale.ENGLISH else Locale.forLanguageTag("es")
}

fun tr(es: String, en: String): String = if (Lang.en) en else es

/** un decimal, con coma o punto segun el idioma y sin el ",0": 3,8 / 3.8 / 5 */
fun fmtDec(v: Float): String {
    val r = (v * 10).roundToInt() / 10f
    val s = if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
    return if (Lang.en) s else s.replace('.', ',')
}
