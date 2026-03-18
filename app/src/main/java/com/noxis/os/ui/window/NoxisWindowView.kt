package com.noxis.os.ui.window

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.max

/**
 * Вікно застосунку всередині Noxis OS.
 * Підтримує: drag по тайтлбару, resize за правий-нижній кут,
 * мінімізацію, закриття.
 */
class NoxisWindowView(
    context: Context,
    val app: AppInfo
) : FrameLayout(context) {

    // Розмір і позиція
    var winX = 40f
    var winY = 80f
    var winWidth = context.dpToPx(NoxisConstants.WINDOW_MIN_WIDTH_DP + 80)
    var winHeight = context.dpToPx(NoxisConstants.WINDOW_MIN_HEIGHT_DP + 60)

    private val titlebarHeight = context.dpToPx(NoxisConstants.WINDOW_TITLEBAR_HEIGHT_DP)
    private val resizeHandle = context.dpToPx(NoxisConstants.WINDOW_RESIZE_HANDLE_DP)
    private val minWidth = context.dpToPx(NoxisConstants.WINDOW_MIN_WIDTH_DP)
    private val minHeight = context.dpToPx(NoxisConstants.WINDOW_MIN_HEIGHT_DP)
    private val cornerRadius = context.dpToPx(10).toFloat()

    // Пейнти
    private val titlebarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141417")
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1F")
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A35")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7B5EA7")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = context.dpToPx(13).toFloat()
        textAlign = Paint.Align.LEFT
    }
    private val resizeHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A3A50")
        style = Paint.Style.FILL
    }

    // Кнопки тайтлбара
    private val btnClose = WindowButton(context, "#FF5F57")
    private val btnMinimize = WindowButton(context, "#FFBD2E")

    // Стан drag/resize
    private enum class TouchMode { NONE, DRAG, RESIZE }
    private var touchMode = TouchMode.NONE
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var startWinX = 0f
    private var startWinY = 0f
    private var startWinW = 0
    private var startWinH = 0

    var isActive = false
        set(value) { field = value; invalidateTitlebar() }

    var isMinimized = false
        private set

    // Контентна область
    val contentArea = FrameLayout(context)

    // Колбеки
    var onClose: ((NoxisWindowView) -> Unit)? = null
    var onMinimize: ((NoxisWindowView) -> Unit)? = null
    var onFocus: ((NoxisWindowView) -> Unit)? = null

    init {
        setWillNotDraw(false)
        elevation = 8f

        // Контент розміщуємо нижче тайтлбара
        addView(contentArea, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).also { it.topMargin = titlebarHeight })

        // Кнопки
        btnClose.setOnClickListener { onClose?.invoke(this) }
        btnMinimize.setOnClickListener { toggleMinimize() }

        addView(btnClose)
        addView(btnMinimize)

        applyLayout()
    }

    private fun applyLayout() {
        val lp = LayoutParams(winWidth, if (isMinimized) titlebarHeight else winHeight)
        layoutParams = lp
        x = winX
        y = winY
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val tb = titlebarHeight.toFloat()

        val path = Path()
        val rect = RectF(0f, 0f, w, h)

        // Тіло вікна
        if (isMinimized) {
            path.addRoundRect(RectF(0f, 0f, w, tb), cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.drawPath(path, titlebarPaint)
        } else {
            // Тайтлбар (округлений тільки зверху)
            val titlePath = Path()
            titlePath.addRoundRect(
                RectF(0f, 0f, w, tb),
                floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
            canvas.drawPath(titlePath, titlebarPaint)

            // Тіло (округлене тільки знизу)
            val bodyPath = Path()
            bodyPath.addRoundRect(
                RectF(0f, tb, w, h),
                floatArrayOf(0f, 0f, 0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius),
                Path.Direction.CW
            )
            canvas.drawPath(bodyPath, bodyPaint)

            // Resize handle
            val rh = resizeHandle.toFloat()
            canvas.drawRoundRect(
                RectF(w - rh, h - rh, w, h),
                cornerRadius * 0.5f, cornerRadius * 0.5f,
                resizeHandlePaint
            )
            // Лінії resize handle
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7B5EA7")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            for (i in 1..3) {
                val offset = i * 4f
                canvas.drawLine(w - rh + offset, h, w, h - rh + offset, linePaint)
            }
        }

        // Рамка
        val bp = Path()
        bp.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(bp, if (isActive) activeBorderPaint else borderPaint)

        // Заголовок
        val titleX = context.dpToPx(48).toFloat()
        val titleY = tb / 2 + titlePaint.textSize / 3
        canvas.drawText(app.name, titleX, titleY, titlePaint)

        // Позиціонуємо кнопки
        val btnSize = context.dpToPx(14)
        val btnY = (tb - btnSize) / 2
        btnClose.apply {
            layout(
                context.dpToPx(8), btnY.toInt(),
                context.dpToPx(8) + btnSize, btnY.toInt() + btnSize
            )
        }
        btnMinimize.apply {
            layout(
                context.dpToPx(26), btnY.toInt(),
                context.dpToPx(26) + btnSize, btnY.toInt() + btnSize
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onFocus?.invoke(this)
                touchStartX = rawX
                touchStartY = rawY
                startWinX = winX
                startWinY = winY
                startWinW = winWidth
                startWinH = winHeight

                touchMode = when {
                    isInResizeZone(event.x, event.y) -> TouchMode.RESIZE
                    isInTitlebar(event.y) -> TouchMode.DRAG
                    else -> TouchMode.NONE
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - touchStartX
                val dy = rawY - touchStartY

                when (touchMode) {
                    TouchMode.DRAG -> {
                        winX = (startWinX + dx).coerceAtLeast(0f)
                        winY = (startWinY + dy).coerceAtLeast(0f)
                        x = winX
                        y = winY
                    }
                    TouchMode.RESIZE -> {
                        winWidth = max(minWidth, startWinW + dx.toInt())
                        winHeight = max(minHeight, startWinH + dy.toInt())
                        applyLayout()
                        requestLayout()
                    }
                    TouchMode.NONE -> {}
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isInTitlebar(y: Float) = y <= titlebarHeight && !isMinimized || isMinimized
    private fun isInResizeZone(x: Float, y: Float) =
        !isMinimized && x >= width - resizeHandle && y >= height - resizeHandle

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        contentArea.visibility = if (isMinimized) View.GONE else View.VISIBLE
        applyLayout()
        requestLayout()
        onMinimize?.invoke(this)
    }

    fun setTitle(title: String) {
        app.let { invalidateTitlebar() }
        // Оновлюємо через tag для перемальовки
        tag = title
        invalidateTitlebar()
    }

    private fun invalidateTitlebar() = invalidate(0, 0, width, titlebarHeight)

    /**
     * Встановлює View як вміст вікна
     */
    fun setContentView(view: View) {
        contentArea.removeAllViews()
        contentArea.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }
}

/** Маленька кругла кнопка тайтлбара */
private class WindowButton(context: Context, colorHex: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(colorHex)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    init { isClickable = true }

    override fun onDraw(canvas: Canvas) {
        val r = width / 2f
        canvas.drawCircle(r, r, r - 1f, paint)
        canvas.drawCircle(r, r, r - 1f, borderPaint)
    }
}
