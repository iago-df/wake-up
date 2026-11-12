package com.iagodf.wakeup.ui.proyectos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** paleta de un paisaje, mismos parametros que en el video */
class Look(
    val s0: Int, val s1: Int, val sun: Int, val sx: Float, val sy: Float,
    val far: Int, val near: Int, val b0: Float, val db: Float, val amp: Float, val fr: Float,
    val peak: Boolean = false, val seed: Int,
)

private fun c(hex: String) = Color.parseColor(hex)
private fun ca(hex: String, a: Float) = (c(hex) and 0x00FFFFFF) or ((a * 255).toInt() shl 24)

val LOOKS = listOf(
    // desierto naranja (OneDay)
    Look(c("#f08a4b"), c("#ffd9a0"), ca("#fffae1", .95f), .7f, .3f, c("#f2a765"), c("#6b2415"), .5f, .11f, .2f, 1.1f, seed = 5),
    // picos azules (Servidor)
    Look(c("#0a3aa6"), c("#6dbcff"), ca("#ffffff", .55f), .3f, .25f, c("#eef5ff"), c("#0c2452"), .62f, .09f, .5f, 1.6f, peak = true, seed = 23),
    // colinas verdes (Lluvia)
    Look(c("#6f97a8"), c("#d4e6e0"), ca("#ffffff", .7f), .5f, .2f, c("#9cc2a0"), c("#0f2f1b"), .5f, .11f, .22f, 1.3f, seed = 41),
    // atardecer violeta
    Look(c("#2a1466"), c("#ff9ac2"), ca("#ffdcf0", .8f), .5f, .36f, c("#b78be0"), c("#1a0a33"), .52f, .1f, .24f, 1.2f, seed = 61),
    // cañon rojo
    Look(c("#7a1420"), c("#ffb07a"), ca("#fff0d2", .85f), .25f, .3f, c("#e8805a"), c("#2b0a0c"), .5f, .11f, .34f, 1.5f, peak = true, seed = 77),
    // noche
    Look(c("#050a24"), c("#1f4a6e"), ca("#c8dcff", .5f), .75f, .22f, c("#3b6a8a"), c("#03101a"), .55f, .1f, .26f, 1.4f, seed = 93),
)

/** mulberry32, el mismo generador que usa el video para que salgan los mismos montes */
private class Rng(seed: Int) {
    private var a = seed
    fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
    }
}

private fun mixColor(a: Int, b: Int, t: Float): Int = Color.rgb(
    (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
    (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
    (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
)

fun landscape(look: Look, w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val W = w.toFloat()
    val H = h.toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val r = Rng(look.seed)

    // cielo
    paint.shader = LinearGradient(0f, 0f, 0f, H * .8f, look.s0, look.s1, Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, W, H, paint)
    // sol
    paint.shader = RadialGradient(W * look.sx, H * look.sy, W * .55f, look.sun, look.sun and 0x00FFFFFF, Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, W, H, paint)
    paint.shader = null

    // 5 capas de montes, de lejos a cerca
    val layers = 5
    val tau = 6.283
    for (i in 0 until layers) {
        val f = i / (layers - 1f)
        val base = H * (look.b0 + look.db * i)
        val amp = H * look.amp * (1 - 0.1f * i)
        val ph = r.next() * 100
        val fr = look.fr + i * 0.45
        paint.color = mixColor(look.far, look.near, f.pow(0.85f))
        val path = Path()
        path.moveTo(0f, H)
        var x = 0f
        val step = W / 340f
        while (x <= W + step) {
            val uu = (x / W).toDouble()
            var n = 0.0
            var a = 1.0
            var q = fr
            var s = 0.0
            for (o in 0 until 4) {
                val v = sin(uu * q * tau + ph + o * 1.7) + 0.5 * sin(uu * q * 2.13 * tau + ph * 1.3 + o)
                n += (if (look.peak) 1 - abs(v) / 1.5 else v / 3 + .5) * a
                s += a
                a *= .5
                q *= 2.03
            }
            path.lineTo(x, (base - amp * (n / s)).toFloat())
            x += step
        }
        path.lineTo(W, H)
        path.close()
        cv.drawPath(path, paint)
    }

    // oscurecer abajo para que se lea el nombre
    paint.shader = LinearGradient(0f, H * .5f, 0f, H, 0x00000000, 0x73000000, Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, W, H, paint)
    return bmp
}
