package com.noxis.os.ui.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Overlay-курсор поверх всього екрану.
 *
 * Логіка:
 * 1. Будь-який тач у будь-якому місці → курсор переміщується
 * 2. Тап близько до курсора (<CURSOR_TAP_RADIUS_DP) → клік
 * 3. Подвійний тап близько до курсора → подвійний клік
 * 4. Фізична миша → перехоплює курсор (вбудована ховається якщо фізична підключена)
 */
class CursorView(context: Context) : View(context) {

    // Позиція курсора
    var cursorX = 100f
    var cursorY = 100f
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPath = Path()

    private val cursorSize = context.dpToPx(NoxisConstants.CURSOR_SIZE_DP).toFloat()
    private val tapRadius = context.dpToPx(NoxisConstants.CURSOR_TAP_RADIUS_DP)

    // Стан подвійного тапу
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private val handler = Handler(Looper.getMainLooper())

    // Колбеки для передачі кліків у систему
    var onCursorClick: ((x: Float, y: Float, isDouble: Boolean) -> Unit)? = null

    // Чи є фізична миша підключена
    private var physicalMouseConnected = false
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

    init {
        // Курсор не перехоплює дотики сам по собі
        isClickable = false
        isFocusable = false

        buildCursorPath()
        setupMouseDetection()
    }

    private fun buildCursorPath() {
        // Класична стрілка курсору
        cursorPath.apply {
            moveTo(0f, 0f)
            lineTo(0f, cursorSize * 0.85f)
            lineTo(cursorSize * 0.25f, cursorSize * 0.65f)
            lineTo(cursorSize * 0.45f, cursorSize)
            lineTo(cursorSize * 0.55f, cursorSize * 0.96f)
            lineTo(cursorSize * 0.35f, cursorSize * 0.62f)
            lineTo(cursorSize * 0.62f, cursorSize * 0.62f)
            close()
        }
    }

    private fun setupMouseDetection() {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                checkPhysicalMouse()
            }
            override fun onInputDeviceRemoved(deviceId: Int) {
                checkPhysicalMouse()
            }
            override fun onInputDeviceChanged(deviceId: Int) {
                checkPhysicalMouse()
            }
        }
        inputManager.registerInputDeviceListener(listener, handler)
        checkPhysicalMouse()
    }

    private fun checkPhysicalMouse() {
        physicalMouseConnected = InputDevice.getDeviceIds().any { id ->
            val device = InputDevice.getDevice(id) ?: return@any false
            device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
                    && !device.isVirtual
        }
        // Ховаємо/показуємо вбудований курсор
        visibility = if (physicalMouseConnected) GONE else VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (physicalMouseConnected) return

        canvas.save()
        canvas.translate(cursorX, cursorY)

        // Тінь
        shadowPaint.apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.translate(2f, 2f)
        canvas.drawPath(cursorPath, shadowPaint)
        canvas.restore()

        // Основний курсор — білий
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(cursorPath, paint)

        // Контур — чорний
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawPath(cursorPath, paint)

        canvas.restore()
    }

    /**
     * Обробляє тач-події від батьківського DesktopView.
     * DesktopView має викликати цей метод для всіх MotionEvent.
     */
    fun handleTouch(event: MotionEvent): Boolean {
        if (physicalMouseConnected) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val touchX = event.getX(event.actionIndex)
                val touchY = event.getY(event.actionIndex)

                moveCursor(touchX, touchY)

                // Перевіряємо чи тап близько до курсора
                if (isTapNearCursor(touchX, touchY)) {
                    handleTap(touchX, touchY)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Переміщуємо курсор за першим пальцем
                moveCursor(event.x, event.y)
                return true
            }
        }
        return false
    }

    /**
     * Обробка подій від фізичної миші (SOURCE_MOUSE)
     */
    fun handleMouseEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_MOUSE != InputDevice.SOURCE_MOUSE) return false

        cursorX = event.rawX.coerceIn(0f, width.toFloat())
        cursorY = event.rawY.coerceIn(0f, height.toFloat())
        invalidate()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTap(cursorX, cursorY)
            }
        }
        return true
    }

    private fun moveCursor(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, (width - cursorSize).coerceAtLeast(0f))
        cursorY = y.coerceIn(0f, (height - cursorSize).coerceAtLeast(0f))
        invalidate()
    }

    private fun isTapNearCursor(x: Float, y: Float): Boolean {
        val dist = hypot(abs(x - cursorX), abs(y - cursorY))
        return dist <= tapRadius
    }

    private fun handleTap(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val isDouble = (now - lastTapTime) < NoxisConstants.CURSOR_DOUBLE_TAP_MS
                && hypot(abs(x - lastTapX), abs(y - lastTapY)) < tapRadius

        lastTapTime = now
        lastTapX = x
        lastTapY = y

        // Клік відправляємо за позицією курсора, а не за місцем тапу
        onCursorClick?.invoke(cursorX, cursorY, isDouble)
    }
}
