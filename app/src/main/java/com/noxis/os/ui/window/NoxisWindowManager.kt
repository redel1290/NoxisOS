package com.noxis.os.ui.window

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.system.lua.LuaRuntime
import com.noxis.os.system.lua.LuaSession
import com.noxis.os.system.lua.LuaWindowApi

/**
 * Керує всіма відкритими вікнами:
 * - відкриття/закриття
 * - z-order (активне вікно зверху)
 * - прив'язка Lua сесій до вікон
 */
class NoxisWindowManager(
    private val context: Context,
    private val container: FrameLayout  // WindowLayer поверх DesktopView
) {

    private val windows = mutableListOf<WindowEntry>()
    private val luaRuntime = LuaRuntime(context)

    // Колбек для оновлення тасбару
    var onWindowListChanged: ((List<WindowEntry>) -> Unit)? = null

    /**
     * Відкрити застосунок у новому вікні
     */
    fun openApp(app: AppInfo) {
        // Якщо вже відкрито — фокусуємо
        val existing = windows.find { it.app.id == app.id }
        if (existing != null) {
            focus(existing.window)
            if (existing.window.isMinimized) {
                existing.window.performClick() // toggle minimize
            }
            return
        }

        val window = NoxisWindowView(context, app)

        // Каскадне розміщення нових вікон
        val offset = windows.size * context.resources.displayMetrics.density.toInt() * 20
        window.winX = 40f + offset
        window.winY = 80f + offset
        window.x = window.winX
        window.y = window.winY

        // Прив'язуємо Lua
        val luaApi = object : LuaWindowApi {
            override fun onSetTitle(title: String) {
                container.post { window.setTitle(title) }
            }
            override fun onSetContent(content: String) {
                container.post {
                    val tv = android.widget.TextView(context).apply {
                        text = content
                        setTextColor(android.graphics.Color.parseColor("#F0F0F5"))
                        setPadding(16, 16, 16, 16)
                    }
                    window.setContentView(tv)
                }
            }
            override fun onClose() {
                container.post { closeWindow(window) }
            }
        }

        val session = luaRuntime.launch(app, luaApi)

        // Якщо немає Lua (системний застосунок) — відкриваємо порожнє вікно
        val entry = WindowEntry(app, window, session)
        windows.add(entry)

        window.onFocus = { focus(it) }
        window.onClose = { closeWindow(it) }
        window.onMinimize = { onWindowListChanged?.invoke(windows.toList()) }

        container.addView(window)
        focus(window)
        onWindowListChanged?.invoke(windows.toList())
    }

    /**
     * Закрити вікно
     */
    fun closeWindow(window: NoxisWindowView) {
        val entry = windows.find { it.window == window } ?: return
        entry.luaSession?.stop()
        container.removeView(window)
        windows.remove(entry)
        // Фокус на попереднє
        windows.lastOrNull()?.let { focus(it.window) }
        onWindowListChanged?.invoke(windows.toList())
    }

    /**
     * Перевести вікно у фокус (підняти на верх z-order)
     */
    fun focus(window: NoxisWindowView) {
        windows.forEach { it.window.isActive = (it.window == window) }
        window.bringToFront()
        window.elevation = 12f
        windows.filter { it.window != window }.forEach { it.window.elevation = 8f }
    }

    fun getOpenWindows(): List<WindowEntry> = windows.toList()

    fun closeAll() {
        windows.toList().forEach { closeWindow(it.window) }
    }
}

data class WindowEntry(
    val app: AppInfo,
    val window: NoxisWindowView,
    val luaSession: LuaSession?
)
