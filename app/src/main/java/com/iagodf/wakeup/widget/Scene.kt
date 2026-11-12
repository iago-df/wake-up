package com.iagodf.wakeup.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.iagodf.wakeup.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * dibuja el widget: el % grande y la casa de campo, que cambia con la hora
 * (amanecer naranja, dia azul con sol, atardecer rojo, noche violeta con luna y ventanas encendidas).
 * las medidas son las del video, sobre un lienzo de 820x400
 */
object Scene {
    // hora -> colores del brillo (c1, c2)
    private val PAL = listOf(
        Triple(0f, intArrayOf(91, 61, 245), intArrayOf(26, 20, 102)),
        Triple(5.5f, intArrayOf(106, 70, 240), intArrayOf(34, 24, 128)),
        Triple(7f, intArrayOf(255, 150, 60), intArrayOf(224, 51, 108)),
        Triple(10f, intArrayOf(63, 164, 255), intArrayOf(21, 84, 209)),
        Triple(16.5f, intArrayOf(63, 164, 255), intArrayOf(21, 84, 209)),
        Triple(19.5f, intArrayOf(255, 90, 60), intArrayOf(179, 36, 122)),
        Triple(21.5f, intArrayOf(122, 77, 255), intArrayOf(36, 20, 138)),
        Triple(24f, intArrayOf(91, 61, 245), intArrayOf(26, 20, 102)),
    )

    private fun mix(a: IntArray, b: IntArray, t: Float) = Color.rgb(
        (a[0] + (b[0] - a[0]) * t).toInt(), (a[1] + (b[1] - a[1]) * t).toInt(), (a[2] + (b[2] - a[2]) * t).toInt(),
    )

    fun dayPal(hour: Float): Pair<Int, Int> {
        val h = ((hour % 24f) + 24f) % 24f
        for (i in 0 until PAL.size - 1) {
            val a = PAL[i]
            val b = PAL[i + 1]
            if (h >= a.first && h <= b.first) {
                val t = (h - a.first) / (b.first - a.first)
                return mix(a.second, b.second, t) to mix(a.third, b.third, t)
            }
        }
        return Color.BLACK to Color.BLACK
    }

    private fun smooth(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    private var inter: Typeface? = null
    private fun inter(ctx: Context): Typeface =
        inter ?: (ResourcesCompat.getFont(ctx, R.font.inter) ?: Typeface.DEFAULT_BOLD).also { inter = it }

    fun render(ctx: Context, w: Int, h: Int, pct: Int, hour: Float): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val s = w / 820f
        val W = w.toFloat()
        val H = h.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val (c1, c2) = dayPal(hour)

        // todo va recortado a la tarjeta redondeada
        val card = Path().apply { addRoundRect(RectF(0f, 0f, W, H), 70 * s, 70 * s, Path.Direction.CW) }
        cv.save()
        cv.clipPath(card)
        cv.drawColor(Color.rgb(9, 9, 13))

        // brillo de abajo
        val rx = W * .75f
        val ry = H * .6f
        cv.save()
        cv.translate(W / 2f, H * 1.02f)
        cv.scale(1f, ry / rx)
        p.shader = RadialGradient(
            0f, 0f, rx,
            intArrayOf(Color.WHITE, Color.WHITE, c1, c2, c2 and 0x00FFFFFF),
            floatArrayOf(0f, .14f, .40f, .68f, 1f), Shader.TileMode.CLAMP,
        )
        cv.drawCircle(0f, 0f, rx, p)
        cv.restore()
        p.shader = null

        // la escena va pegada abajo
        cv.save()
        cv.translate(0f, H - 400 * s)
        cv.scale(s, s)

        // sol de 6:12 a 20:48, luna el resto
        val ps = (hour - 6.2f) / (20.8f - 6.2f)
        val hn = if (hour < 6.2f) hour + 24f else hour
        val pm = (hn - 20.6f) / 9.6f
        if (ps in 0f..1f) {
            orb(cv, p, 430 + 340 * ps, 345 - sin(PI * ps).toFloat() * 270, 32f, Color.rgb(255, 231, 166), Color.argb(190, 255, 220, 150), 1f)
        } else if (pm in 0f..1f) {
            orb(cv, p, 430 + 340 * pm, 345 - sin(PI * pm).toFloat() * 270, 23f, Color.rgb(207, 216, 255), Color.argb(150, 190, 205, 255), smooth(0f, .1f, pm))
        }

        // colina, casa con chimenea y arbol
        p.color = Color.rgb(5, 5, 8)
        val hill = Path().apply {
            moveTo(0f, 400f); lineTo(0f, 338f)
            cubicTo(140f, 296f, 270f, 304f, 400f, 338f)
            cubicTo(520f, 366f, 650f, 304f, 820f, 322f)
            lineTo(820f, 400f); close()
        }
        cv.drawPath(hill, p)
        cv.drawRect(596f, 262f, 692f, 328f, p)
        cv.drawPath(Path().apply { moveTo(582f, 266f); lineTo(644f, 212f); lineTo(706f, 266f); close() }, p)
        cv.drawRect(668f, 218f, 683f, 252f, p)
        cv.drawRect(524f, 276f, 533f, 332f, p)
        cv.drawCircle(528f, 254f, 32f, p)
        cv.drawCircle(506f, 274f, 23f, p)
        cv.drawCircle(550f, 274f, 23f, p)

        // ventanas encendidas de noche
        val night = 1 - smooth(7f, 8.5f, hour) * (1 - smooth(19.3f, 20.8f, hour))
        if (night > 0f) {
            p.color = Color.argb((night * 255).toInt(), 255, 207, 112)
            cv.drawRoundRect(612f, 282f, 634f, 304f, 2f, 2f, p)
            cv.drawRoundRect(654f, 282f, 676f, 304f, 2f, 2f, p)
        }
        cv.restore()

        // textos (la hora de arriba la pone un TextClock para que vaya en directo)
        p.color = Color.WHITE
        // la fuente es variable, el peso se pide en el paint
        p.typeface = inter(ctx)
        p.fontVariationSettings = "'wght' 600"
        p.textSize = 196 * s
        p.letterSpacing = -.046f
        cv.drawText("$pct%", 50 * s, 246 * s, p)
        p.color = Color.rgb(230, 230, 239)
        p.fontVariationSettings = "'wght' 500"
        p.textSize = 29 * s
        p.letterSpacing = 0f
        cv.drawText("de tu tiempo libre", 56 * s, 290 * s, p)
        cv.restore()

        // borde fino
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * s
        p.color = Color.argb(26, 255, 255, 255)
        cv.drawRoundRect(RectF(s, s, W - s, H - s), 70 * s, 70 * s, p)
        return bmp
    }

    private fun orb(cv: Canvas, p: Paint, x: Float, y: Float, r: Float, edge: Int, glow: Int, alpha: Float) {
        val a = (alpha * 255).toInt()
        p.alpha = a
        p.shader = RadialGradient(x, y, r * 3.4f, intArrayOf(glow, glow and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        cv.drawCircle(x, y, r * 3.4f, p)
        p.shader = RadialGradient(x, y, r, intArrayOf(Color.WHITE, Color.WHITE, edge), floatArrayOf(0f, .3f, 1f), Shader.TileMode.CLAMP)
        cv.drawCircle(x, y, r, p)
        p.shader = null
        p.alpha = 255
    }
}
