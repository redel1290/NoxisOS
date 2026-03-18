package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.noxis.os.ui.window.WindowEntry
import com.noxis.os.util.dpToPx
import java.text.SimpleDateFormat
import java.util.*

class TaskbarView(context: Context) : FrameLayout(context) {

    private val height = context.dpToPx(48)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    // Пейнти
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60F0F12")
        style = Paint.Style.FILL
    }
    private val separatorPaint = Paint().apply {
        color = Color.parseColor("#2A2A35")
        strokeWidth = 1f
    }

    // UI елементи
    private val startBtn: TextView
    private val clockView: TextView
    private val appsList: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 30_000)
        }
    }

    // Колбеки
    var onStartClick: (() -> Unit)? = null
    var onWindowClick: ((WindowEntry) -> Unit)? = null

    init {
        setWillNotDraw(true) // малюємо через дочірні view
        setBackgroundColor(Color.parseColor("#0F0F12"))

        // Кнопка "Start"
        startBtn = TextView(context).apply {
            text = "⊞"
            textSize = 20f
            setTextColor(Color.parseColor("#C8AAFF"))
            gravity = Gravity.CENTER
            setPadding(
                context.dpToPx(12), 0,
                context.dpToPx(12), 0
            )
            setOnClickListener { onStartClick?.invoke() }
        }

        // Список відкритих застосунків
        appsList = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Годинник
        clockView = TextView(context).apply {
            setTextColor(Color.parseColor("#F0F0F5"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(
                context.dpToPx(8), 0,
                context.dpToPx(12), 0
            )
        }

        // Компонування
        addView(startBtn, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        ).also { it.gravity = Gravity.START or Gravity.CENTER_VERTICAL })

        addView(appsList, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        ).also {
            it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            it.marginStart = context.dpToPx(56)
        })

        addView(clockView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        ).also { it.gravity = Gravity.END or Gravity.CENTER_VERTICAL })

        updateClock()
        handler.post(clockRunnable)
    }

    /**
     * Оновити список відкритих вікон у тасбарі
     */
    fun updateWindows(windows: List<WindowEntry>) {
        appsList.removeAllViews()

        windows.forEach { entry ->
            val btn = TextView(context).apply {
                text = entry.app.name.take(10)
                textSize = 11f
                setPadding(
                    context.dpToPx(10), context.dpToPx(4),
                    context.dpToPx(10), context.dpToPx(4)
                )
                gravity = Gravity.CENTER

                val isActive = entry.window.isActive
                setTextColor(if (isActive) Color.parseColor("#C8AAFF") else Color.parseColor("#8A8A9A"))
                setBackgroundResource(0)

                // Активне — з підсвіткою
                if (isActive) {
                    background = android.graphics.drawable.ShapeDrawable(
                        android.graphics.drawable.shapes.RoundRectShape(
                            FloatArray(8) { context.dpToPx(4).toFloat() }, null, null
                        )
                    ).apply {
                        paint.color = Color.parseColor("#2A1F3D")
                    }
                }

                setOnClickListener { onWindowClick?.invoke(entry) }
            }

            appsList.addView(btn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).also { it.marginEnd = context.dpToPx(2) })
        }
    }

    private fun updateClock() {
        val now = Date()
        clockView.text = "${timeFormat.format(now)}\n${dateFormat.format(now)}"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(clockRunnable)
    }
}
