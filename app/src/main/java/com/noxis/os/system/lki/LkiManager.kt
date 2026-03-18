package com.noxis.os.system.lki

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.noxis.os.util.NoxisConstants
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Відповідає за:
 * - встановлення .lki пакетів (розпакування ZIP)
 * - видалення застосунків
 * - завантаження списку встановлених застосунків
 */
class LkiManager(private val context: Context) {

    private val gson = Gson()
    private val tag = "LkiManager"

    /**
     * Встановити .lki пакет із вказаного файлу.
     * Повертає [AppInfo] при успіху або null при помилці.
     */
    fun install(lkiFile: File): InstallResult {
        if (!lkiFile.exists()) return InstallResult.Error("Файл не знайдено")

        // Читаємо маніфест не розпаковуючи весь архів
        val manifest = readManifest(lkiFile)
            ?: return InstallResult.Error("Невалідний маніфест")

        if (!manifest.isValid())
            return InstallResult.Error("Маніфест не містить обов'язкових полів")

        val installDir = File(NoxisConstants.APPS_DIR, manifest.id)

        // Якщо вже встановлено — видаляємо стару версію
        if (installDir.exists()) installDir.deleteRecursively()
        installDir.mkdirs()

        // Розпаковуємо весь ZIP
        return try {
            ZipInputStream(lkiFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(installDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val iconFile = File(installDir, manifest.icon)
            val icon = if (iconFile.exists())
                BitmapFactory.decodeFile(iconFile.absolutePath)
            else null

            val appInfo = AppInfo(
                id = manifest.id,
                name = manifest.name,
                version = manifest.version,
                author = manifest.author,
                description = manifest.description,
                icon = icon,
                installDir = installDir.absolutePath,
                mainScript = manifest.main
            )

            saveAppMeta(appInfo)
            Log.i(tag, "Встановлено: ${manifest.id} v${manifest.version}")
            InstallResult.Success(appInfo)

        } catch (e: Exception) {
            installDir.deleteRecursively()
            Log.e(tag, "Помилка встановлення: ${e.message}")
            InstallResult.Error("Помилка розпакування: ${e.message}")
        }
    }

    /**
     * Читає маніфест із .lki файлу без повного розпакування
     */
    fun readManifest(lkiFile: File): LkiManifest? {
        return try {
            ZipInputStream(lkiFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == NoxisConstants.LKI_MANIFEST) {
                        val text = zip.bufferedReader().readText()
                        return gson.fromJson(text, LkiManifest::class.java)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "readManifest error: ${e.message}")
            null
        }
    }

    /**
     * Видалити застосунок
     */
    fun uninstall(appId: String): Boolean {
        val installDir = File(NoxisConstants.APPS_DIR, appId)
        return if (installDir.exists()) {
            installDir.deleteRecursively()
            true
        } else false
    }

    /**
     * Завантажити всі встановлені застосунки
     */
    fun loadInstalledApps(): List<AppInfo> {
        val appsDir = NoxisConstants.APPS_DIR
        if (!appsDir.exists()) return emptyList()

        return appsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                try {
                    val metaFile = File(dir, "meta.json")
                    if (!metaFile.exists()) return@mapNotNull null

                    val meta = gson.fromJson(metaFile.readText(), AppMeta::class.java)
                    val iconFile = File(dir, "icon.png")
                    val icon = if (iconFile.exists())
                        BitmapFactory.decodeFile(iconFile.absolutePath)
                    else null

                    AppInfo(
                        id = meta.id,
                        name = meta.name,
                        version = meta.version,
                        author = meta.author,
                        description = meta.description,
                        icon = icon,
                        installDir = dir.absolutePath,
                        mainScript = meta.mainScript,
                        gridCol = meta.gridCol,
                        gridRow = meta.gridRow
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Помилка завантаження ${dir.name}: ${e.message}")
                    null
                }
            } ?: emptyList()
    }

    /**
     * Зберегти/оновити позицію іконки на сітці
     */
    fun saveGridPosition(appId: String, col: Int, row: Int) {
        val metaFile = File(NoxisConstants.APPS_DIR, "$appId/meta.json")
        if (!metaFile.exists()) return
        val meta = gson.fromJson(metaFile.readText(), AppMeta::class.java)
        metaFile.writeText(gson.toJson(meta.copy(gridCol = col, gridRow = row)))
    }

    private fun saveAppMeta(app: AppInfo) {
        val metaFile = File(app.installDir, "meta.json")
        metaFile.writeText(
            gson.toJson(
                AppMeta(
                    id = app.id,
                    name = app.name,
                    version = app.version,
                    author = app.author,
                    description = app.description,
                    mainScript = app.mainScript
                )
            )
        )
    }

    // Внутрішня модель для збереження
    private data class AppMeta(
        val id: String,
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val mainScript: String,
        val gridCol: Int = 0,
        val gridRow: Int = 0
    )
}

sealed class InstallResult {
    data class Success(val app: AppInfo) : InstallResult()
    data class Error(val message: String) : InstallResult()
}
