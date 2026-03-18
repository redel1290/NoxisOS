package com.noxis.os.util

import android.os.Environment
import java.io.File

object NoxisConstants {

    // --- Сітка робочого столу ---
    const val GRID_COLUMNS = 5
    const val GRID_ROWS = 8
    const val GRID_ICON_SIZE_DP = 60
    const val GRID_PADDING_DP = 8

    // --- Курсор ---
    const val CURSOR_SIZE_DP = 24
    const val CURSOR_TAP_RADIUS_DP = 48f   // зона для визначення "тап близько до курсора"
    const val CURSOR_DOUBLE_TAP_MS = 300L  // максимальний проміжок між двома тапами

    // --- Вікна ---
    const val WINDOW_MIN_WIDTH_DP = 240
    const val WINDOW_MIN_HEIGHT_DP = 160
    const val WINDOW_TITLEBAR_HEIGHT_DP = 36
    const val WINDOW_RESIZE_HANDLE_DP = 16

    // --- Шляхи зберігання ---
    val BASE_USER_DIR: File
        get() = File(
            Environment.getExternalStorageDirectory(),
            "NoxisOS"
        )

    val APPS_DIR: File
        get() = File(BASE_USER_DIR, "apps")

    val USER_FILES_DIR: File
        get() = File(BASE_USER_DIR, "files")

    val BACKUP_DIR: File
        get() = File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.noxis.os/backup"
        )

    // --- .lki пакети ---
    const val LKI_MANIFEST = "manifest.json"
    const val LKI_MAIN_SCRIPT = "main.lua"
    const val LKI_ICON = "icon.png"
    const val LKI_EXTENSION = ".lki"

    // --- Мережа (опціонально) ---
    const val DEFAULT_SERVER_PORT = 7291
    const val SERVER_TIMEOUT_MS = 5000L
}
