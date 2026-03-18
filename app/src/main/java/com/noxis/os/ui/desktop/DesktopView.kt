package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.ui.cursor.CursorView
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.hypot

class DesktopView(context: Context) : View(context) {

    private val grid = DesktopGrid(context)
    private val apps = mutableListOf<DesktopIcon>()

    // Пейнти
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 40, 40, 60)
        style = Paint.Style.FILL
    }
    private val iconBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7B5EA7")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = context.dpToPx(10).toFloat()
        textAlign = Paint.Align.CENTER
    }
    private val defaultIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9D7EC7")
        style = Paint.Style.FILL
    }

    // Drag стан
    private var draggingIcon: DesktopIcon? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var isDragging = false
    private val dragThreshold = context.dpToPx(8).toFloat()

    // Колбеки
    var onAppLaunch: ((AppInfo) -> Unit)? = null
    var cursorView: CursorView? = null

    fun loadApps(appList: List<AppInfo>) {
        apps.clear()
        grid.clearAll()

        appList.forEach { app ->
            val (x, y) = grid.cellToPixel(app.gridCol, app.gridRow)
            apps.add(DesktopIcon(app, x, y))
            grid.occupy(app.gridCol, app.gridRow)
        }
        invalidate()
    }

    fun addApp(app: AppInfo) {
        val (col, row) = grid.findFirstFreeCell()
        app.gridCol = col
        app.gridRow = row
        val (x, y) = grid.cellToPixel(col, row)
        apps.add(DesktopIcon(app, x, y))
        grid.occupy(col, row)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        grid.updateSize(w, h, context.dpToPx(56)) // taskbar height
    }

    override fun onDraw(canvas: Canvas) {
        // Малюємо іконки
        apps.forEach { icon ->
            val isBeingDragged = icon == draggingIcon && isDragging
            if (!isBeingDragged) {
                drawIcon(canvas, icon, icon.x, icon.y, alpha = 255)
            }
        }

        // Drag preview
        if (isDragging && draggingIcon != null) {
            // Показуємо тінь на цільовій комірці
            val (snapCol, snapRow) = grid.snapToNearest(
                dragCurrentX, dragCurrentY,
                excludeKey = "${draggingIcon!!.app.gridCol}_${draggingIcon!!.app.gridRow}"
            )
            val (snapX, snapY) = grid.cellToPixel(snapCol, snapRow)
            drawSnapPreview(canvas, snapX, snapY)

            // Сама іконка слідує за пальцем
            drawIcon(canvas, draggingIcon!!, dragCurrentX - dragOffsetX, dragCurrentY - dragOffsetY, alpha = 200)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: DesktopIcon, x: Float, y: Float, alpha: Int) {
        val size = grid.iconSize.toFloat()
        val rect = RectF(x, y, x + size, y + size)
        val cornerRadius = size * 0.2f

        // Фон іконки
        iconBgPaint.alpha = alpha
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, iconBgPaint)

        // Зображення або дефолтний ромб
        icon.app.icon?.let { bitmap ->
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha }
            val src = Rect(0, 0, bitmap.width, bitmap.height)
            val dst = RectF(x + 4, y + 4, x + size - 4, y + size - 16)
            canvas.drawBitmap(bitmap, src, dst, bitmapPaint)
        } ?: run {
            // Дефолтна іконка — квадрат з літерою
            defaultIconPaint.alpha = alpha
            val innerRect = RectF(x + 8, y + 8, x + size - 8, y + size - 20)
            canvas.drawRoundRect(innerRect, cornerRadius * 0.5f, cornerRadius * 0.5f, defaultIconPaint)
            labelPaint.apply {
                this.alpha = alpha
                textSize = size * 0.35f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                icon.app.name.take(1).uppercase(),
                x + size / 2,
                y + size / 2 + labelPaint.textSize * 0.35f,
                labelPaint
            )
        }

        // Обводка
        iconBorderPaint.alpha = (alpha * 0.6f).toInt()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, iconBorderPaint)

        // Назва застосунку
        labelPaint.apply {
            this.alpha = alpha
            textSize = context.dpToPx(10).toFloat()
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            icon.app.name,
            x + size / 2,
            y + size + context.dpToPx(12),
            labelPaint
        )
    }

    private fun drawSnapPreview(canvas: Canvas, x: Float, y: Float) {
        val size = grid.iconSize.toFloat()
        val rect = RectF(x, y, x + size, y + size)
        val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#407B5EA7")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC7B5EA7")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, size * 0.2f, size * 0.2f, previewPaint)
        canvas.drawRoundRect(rect, size * 0.2f, size * 0.2f, borderPaint)
    }

    // Touch обробка — проксі через курсор
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Передаємо у курсор для позиціонування
        cursorView?.handleTouch(event)
        return handleDesktopTouch(event)
    }

    private fun handleDesktopTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val (ix, iy) = event.x to event.y
                draggingIcon = findIconAt(ix, iy)
                if (draggingIcon != null) {
                    dragOffsetX = ix - draggingIcon!!.x
                    dragOffsetY = iy - draggingIcon!!.y
                    dragCurrentX = ix
                    dragCurrentY = iy
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingIcon != null) {
                    val dx = abs(event.x - dragCurrentX)
                    val dy = abs(event.y - dragCurrentY)
                    if (!isDragging && hypot(dx, dy) > dragThreshold) {
                        isDragging = true
                    }
                    if (isDragging) {
                        dragCurrentX = event.x
                        dragCurrentY = event.y
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && draggingIcon != null) {
                    // Snap до найближчої вільної комірки
                    val icon = draggingIcon!!
                    grid.release(icon.app.gridCol, icon.app.gridRow)

                    val (newCol, newRow) = grid.snapToNearest(
                        dragCurrentX - dragOffsetX + grid.iconSize / 2,
                        dragCurrentY - dragOffsetY + grid.iconSize / 2
                    )
                    val (newX, newY) = grid.cellToPixel(newCol, newRow)

                    icon.x = newX
                    icon.y = newY
                    icon.app.gridCol = newCol
                    icon.app.gridRow = newRow
                    grid.occupy(newCol, newRow)

                } else if (!isDragging && draggingIcon != null) {
                    // Тап — запуск застосунку
                    onAppLaunch?.invoke(draggingIcon!!.app)
                }

                draggingIcon = null
                isDragging = false
                invalidate()
                return true
            }
        }
        return false
    }

    /**
     * Клік від курсора (CursorView)
     */
    fun handleCursorClick(x: Float, y: Float, isDouble: Boolean) {
        val icon = findIconAt(x, y) ?: return
        if (isDouble) {
            onAppLaunch?.invoke(icon.app)
        }
    }

    private fun findIconAt(x: Float, y: Float): DesktopIcon? {
        val size = grid.iconSize.toFloat()
        return apps.firstOrNull { icon ->
            x >= icon.x && x <= icon.x + size && y >= icon.y && y <= icon.y + size
        }
    }
}

// Внутрішня модель іконки на робочому столі
data class DesktopIcon(
    val app: AppInfo,
    var x: Float,
    var y: Float
)
