package com.noxis.os.system.lua

import android.content.Context
import android.util.Log
import com.noxis.os.system.lki.AppInfo
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

/**
 * Виконує Lua скрипти застосунків.
 * Надає API для взаємодії з системою (файли, UI події).
 */
class LuaRuntime(private val context: Context) {

    private val tag = "LuaRuntime"

    /**
     * Запустити застосунок
     * @param app інформація про застосунок
     * @param windowApi об'єкт для взаємодії із вікном (передається в Lua)
     */
    fun launch(app: AppInfo, windowApi: LuaWindowApi): LuaSession? {
        val scriptFile = File(app.installDir, app.mainScript)
        if (!scriptFile.exists()) {
            Log.e(tag, "Скрипт не знайдено: ${scriptFile.absolutePath}")
            return null
        }

        return try {
            val globals = JsePlatform.standardGlobals()

            // Inject Noxis API
            globals.set("noxis", buildNoxisApi(globals, app, windowApi))

            // Встановлюємо кореневу директорію для require
            globals.set("APP_DIR", LuaValue.valueOf(app.installDir))

            val session = LuaSession(globals, scriptFile, app)
            session.start()
            session
        } catch (e: Exception) {
            Log.e(tag, "Помилка запуску ${app.id}: ${e.message}")
            null
        }
    }

    /**
     * Будує таблицю noxis.* доступну з Lua скрипту
     *
     * Lua API:
     *   noxis.print(msg)          -- вивід у лог
     *   noxis.setTitle(title)     -- змінити заголовок вікна
     *   noxis.setContent(html)    -- встановити HTML контент вікна (простий рендер)
     *   noxis.readFile(path)      -- прочитати файл із директорії застосунку
     *   noxis.writeFile(path, data) -- записати файл
     *   noxis.close()             -- закрити вікно
     */
    private fun buildNoxisApi(globals: Globals, app: AppInfo, windowApi: LuaWindowApi): LuaTable {
        val api = LuaTable()

        // noxis.print(msg)
        api.set("print", object : OneArgFunction() {
            override fun call(msg: LuaValue): LuaValue {
                Log.i("Lua[${app.id}]", msg.tojstring())
                return NONE
            }
        })

        // noxis.setTitle(title)
        api.set("setTitle", object : OneArgFunction() {
            override fun call(title: LuaValue): LuaValue {
                windowApi.onSetTitle(title.tojstring())
                return NONE
            }
        })

        // noxis.setContent(htmlOrText)
        api.set("setContent", object : OneArgFunction() {
            override fun call(content: LuaValue): LuaValue {
                windowApi.onSetContent(content.tojstring())
                return NONE
            }
        })

        // noxis.readFile(relativePath) -> string | nil
        api.set("readFile", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                return try {
                    val file = File(app.installDir, path.tojstring())
                    if (file.exists()) valueOf(file.readText())
                    else NIL
                } catch (e: Exception) { NIL }
            }
        })

        // noxis.writeFile(relativePath, data)
        api.set("writeFile", object : TwoArgFunction() {
            override fun call(path: LuaValue, data: LuaValue): LuaValue {
                return try {
                    val file = File(app.installDir, path.tojstring())
                    file.parentFile?.mkdirs()
                    file.writeText(data.tojstring())
                    TRUE
                } catch (e: Exception) { FALSE }
            }
        })

        // noxis.close()
        api.set("close", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                windowApi.onClose()
                return NONE
            }
        })

        // noxis.appId
        api.set("appId", valueOf(app.id))

        // noxis.appDir
        api.set("appDir", valueOf(app.installDir))

        return api
    }
}

/**
 * Callback інтерфейс для Lua → Window взаємодії
 */
interface LuaWindowApi {
    fun onSetTitle(title: String)
    fun onSetContent(content: String)
    fun onClose()
}

/**
 * Активна Lua сесія застосунку
 */
class LuaSession(
    private val globals: Globals,
    private val scriptFile: File,
    val app: AppInfo
) {
    private val tag = "LuaSession"
    private var running = false

    fun start() {
        running = true
        Thread {
            try {
                val chunk = globals.loadfile(scriptFile.absolutePath)
                chunk.call()
            } catch (e: LuaError) {
                Log.e(tag, "Lua помилка [${app.id}]: ${e.message}")
            } catch (e: Exception) {
                Log.e(tag, "Помилка сесії [${app.id}]: ${e.message}")
            } finally {
                running = false
            }
        }.apply {
            isDaemon = true
            name = "lua-${app.id}"
            start()
        }
    }

    fun stop() {
        running = false
        // LuaJ не має прямого interrupt, але оскільки thread є daemon —
        // завершиться разом із застосунком
    }

    fun isRunning() = running
}
