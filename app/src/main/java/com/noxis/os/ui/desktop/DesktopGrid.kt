package com.noxis.os.ui.desktop

import android.content.Context
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx

/**
 * Розраховує сітку робочого столу.
 * Іконки прив'язуються до комірок сітки.
 */
class DesktopGrid(context: Context) {

    val iconSize = context.dpToPx(NoxisConstants.GRID_ICON_SIZE_DP)
    val iconPadding = context.dpToPx(NoxisConstants.GRID_PADDING_DP)
    val cellSize = iconSize + iconPadding * 2

    private var screenWidth = 0
    private var screenHeight = 0
    private var taskbarHeight = 0

    // Кількість реальних колонок/рядків з урахуванням розміру екрану
    var columns = NoxisConstants.GRID_COLUMNS
        private set
    var rows = NoxisConstants.GRID_ROWS
        private set

    // Зайняті комірки: key = "col_row"
    private val occupied = mutableSetOf<String>()

    fun updateSize(width: Int, height: Int, taskbar: Int) {
        screenWidth = width
        screenHeight = height - taskbar
        taskbarHeight = taskbar

        // Авто розрахунок колонок і рядків
        columns = (screenWidth / cellSize).coerceAtLeast(4)
        rows = (screenHeight / cellSize).coerceAtLeast(6)
    }

    /**
     * Конвертує pixel координати у (col, row) сітки
     */
    fun pixelToCell(x: Float, y: Float): Pair<Int, Int> {
        val col = ((x - iconPadding) / cellSize).toInt().coerceIn(0, columns - 1)
        val row = ((y - iconPadding) / cellSize).toInt().coerceIn(0, rows - 1)
        return col to row
    }

    /**
     * Конвертує (col, row) у pixel координати лівого верхнього кута іконки
     */
    fun cellToPixel(col: Int, row: Int): Pair<Float, Float> {
        val x = col * cellSize.toFloat() + iconPadding
        val y = row * cellSize.toFloat() + iconPadding
        return x to y
    }

    /**
     * Знайти найближчу вільну комірку до вказаної позиції
     */
    fun snapToNearest(x: Float, y: Float, excludeKey: String? = null): Pair<Int, Int> {
        val (targetCol, targetRow) = pixelToCell(x, y)

        // Спочатку пробуємо цільову комірку
        if (!isCellOccupied(targetCol, targetRow, excludeKey)) {
            return targetCol to targetRow
        }

        // Ищемо найближчу вільну spiral search
        for (radius in 1..maxOf(columns, rows)) {
            for (dc in -radius..radius) {
                for (dr in -radius..radius) {
                    if (maxOf(Math.abs(dc), Math.abs(dr)) != radius) continue
                    val col = (targetCol + dc).coerceIn(0, columns - 1)
                    val row = (targetRow + dr).coerceIn(0, rows - 1)
                    if (!isCellOccupied(col, row, excludeKey)) {
                        return col to row
                    }
                }
            }
        }

        return targetCol to targetRow
    }

    fun occupy(col: Int, row: Int) {
        occupied.add(cellKey(col, row))
    }

    fun release(col: Int, row: Int) {
        occupied.remove(cellKey(col, row))
    }

    fun setOccupied(col: Int, row: Int, isOccupied: Boolean) {
        if (isOccupied) occupy(col, row) else release(col, row)
    }

    private fun isCellOccupied(col: Int, row: Int, excludeKey: String?): Boolean {
        val key = cellKey(col, row)
        return key in occupied && key != excludeKey
    }

    private fun cellKey(col: Int, row: Int) = "${col}_${row}"

    /**
     * Знайти першу вільну комірку (для нових іконок)
     * Зліва направо, зверху вниз
     */
    fun findFirstFreeCell(): Pair<Int, Int> {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (cellKey(col, row) !in occupied) {
                    return col to row
                }
            }
        }
        return 0 to 0
    }

    fun clearAll() = occupied.clear()
}
