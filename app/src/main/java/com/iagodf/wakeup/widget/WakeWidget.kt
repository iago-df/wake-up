package com.iagodf.wakeup.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iagodf.wakeup.MainActivity
import com.iagodf.wakeup.R
import com.iagodf.wakeup.data.AppState
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class WakeWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx)
        schedule(ctx)
        // de paso releer el uso en segundo plano
        WorkManager.getInstance(ctx).enqueueUniqueWork("wakeup-now", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<WidgetWorker>().build())
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opts: Bundle?) = render(ctx)

    override fun onEnabled(ctx: Context) = schedule(ctx)

    override fun onDisabled(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC)
    }

    companion object {
        private const val PERIODIC = "wakeup-widget"

        /** el minimo que deja android son 15 min */
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        /** redibuja todos los widgets con lo que hay guardado */
        fun render(ctx: Context, state: AppState? = null) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, WakeWidget::class.java))
            if (ids.isEmpty()) return
            val st = state ?: AppState(ctx)
            val pct = st.pct(st.today)
            val now = LocalTime.now()
            val hour = now.hour + now.minute / 60f
            val dm = ctx.resources.displayMetrics
            for (id in ids) {
                val o = mgr.getAppWidgetOptions(id)
                val wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320).coerceAtLeast(120)
                val hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 156).coerceAtLeast(60)
                // no pasarse de tamaño, los bitmaps de un widget tienen limite
                val scale = minOf(dm.density, 900f / wDp)
                val w = (wDp * scale).toInt()
                val h = (hDp * scale).toInt()
                val s = wDp * dm.density / 820f

                val views = RemoteViews(ctx.packageName, R.layout.widget)
                views.setImageViewBitmap(R.id.img, Scene.render(ctx, w, h, pct, hour))
                views.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_PX, 23 * s)
                views.setViewPadding(R.id.clock, (56 * s).toInt(), (40 * s).toInt(), 0, 0)
                val open = PendingIntent.getActivity(
                    ctx, 0, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.root, open)
                mgr.updateAppWidget(id, views)
            }
        }
    }
}

/** cada 15 min: leer el uso del movil y redibujar el widget */
class WidgetWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val state = AppState(applicationContext)
        state.refresh()
        WakeWidget.render(applicationContext, state)
        return Result.success()
    }
}
