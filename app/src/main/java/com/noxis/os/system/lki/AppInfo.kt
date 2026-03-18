package com.noxis.os.system.lki

import android.graphics.Bitmap

/**
 * Модель встановленого .lki застосунку
 */
data class AppInfo(
    val id: String,           // унікальний id з маніфесту
    val name: String,         // відображувана назва
    val version: String,      // версія
    val author: String,       // автор
    val description: String,  // опис
    val icon: Bitmap?,        // іконка (null = дефолтна)
    val installDir: String,   // шлях до папки застосунку
    val mainScript: String,   // відносний шлях до main.lua
    // Позиція на робочому столі (колонка, рядок)
    var gridCol: Int = 0,
    var gridRow: Int = 0
)
