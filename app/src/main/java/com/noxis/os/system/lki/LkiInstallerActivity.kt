package com.noxis.os.system.lki

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.noxis.os.R
import java.io.File

class LkiInstallerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }

        handleInstall(uri)
    }

    private fun handleInstall(uri: Uri) {
        val manager = LkiManager(this)

        // Копіюємо файл у кеш для читання
        val tmpFile = copyToCache(uri) ?: run {
            Toast.makeText(this, getString(R.string.install_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Читаємо маніфест для показу інфо
        val manifest = manager.readManifest(tmpFile)
        if (manifest == null || !manifest.isValid()) {
            tmpFile.delete()
            Toast.makeText(this, getString(R.string.invalid_package), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Показуємо діалог підтвердження
        AlertDialog.Builder(this, R.style.Theme_NoxisOS_Dialog)
            .setTitle(getString(R.string.install_app))
            .setMessage(
                buildString {
                    append(getString(R.string.install_confirm, manifest.name))
                    append("\n\n")
                    append(getString(R.string.version, manifest.version))
                    append("\n")
                    append(getString(R.string.author, manifest.author))
                    if (manifest.description.isNotBlank()) {
                        append("\n\n")
                        append(manifest.description)
                    }
                }
            )
            .setPositiveButton(getString(R.string.install)) { _, _ ->
                performInstall(manager, tmpFile)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                tmpFile.delete()
                finish()
            }
            .setOnCancelListener {
                tmpFile.delete()
                finish()
            }
            .show()
    }

    private fun performInstall(manager: LkiManager, tmpFile: File) {
        when (val result = manager.install(tmpFile)) {
            is InstallResult.Success -> {
                Toast.makeText(
                    this,
                    getString(R.string.install_success),
                    Toast.LENGTH_SHORT
                ).show()
                // Повідомляємо робочий стіл про новий застосунок
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("app_id", result.app.id)
                })
            }
            is InstallResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
        tmpFile.delete()
        finish()
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri) ?: "package.lki"
            val cacheFile = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment
    }
}
