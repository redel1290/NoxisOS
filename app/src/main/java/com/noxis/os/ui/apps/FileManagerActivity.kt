package com.noxis.os.ui.apps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.noxis.os.R
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManagerActivity : AppCompatActivity() {

    private lateinit var pathView: TextView
    private lateinit var listView: LinearLayout
    private lateinit var scrollView: ScrollView

    private var currentDir: File = NoxisConstants.USER_FILES_DIR
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0F"))
        }

        // Хлібні крихти / поточний шлях
        pathView = TextView(this).apply {
            setTextColor(Color.parseColor("#C8AAFF"))
            textSize = 12f
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            setBackgroundColor(Color.parseColor("#0F0F12"))
        }
        root.addView(pathView)

        // Розділювач
        root.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#2A2A35"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        })

        // Список файлів
        scrollView = ScrollView(this)
        listView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listView)
        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        supportActionBar?.apply {
            title = getString(R.string.file_manager)
            setDisplayHomeAsUpEnabled(true)
        }

        // Створюємо директорії якщо не існують
        NoxisConstants.USER_FILES_DIR.mkdirs()

        navigateTo(currentDir)
    }

    private fun navigateTo(dir: File) {
        currentDir = dir
        pathView.text = dir.absolutePath.replace(
            NoxisConstants.BASE_USER_DIR.absolutePath, "~/NoxisOS"
        )

        listView.removeAllViews()

        // Кнопка "вгору"
        if (dir != NoxisConstants.BASE_USER_DIR && dir.parentFile != null) {
            listView.addView(fileRow(
                icon = "⬆",
                name = "..",
                info = "Вгору",
                onClick = { navigateTo(dir.parentFile!!) }
            ))
        }

        val files = dir.listFiles()?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: emptyList()

        if (files.isEmpty()) {
            listView.addView(TextView(this).apply {
                text = "Папка порожня"
                setTextColor(Color.parseColor("#8A8A9A"))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(32), 0, 0)
            })
            return
        }

        files.forEach { file ->
            listView.addView(fileRow(
                icon = if (file.isDirectory) "📁" else getFileIcon(file.name),
                name = file.name,
                info = if (file.isDirectory)
                    "${file.listFiles()?.size ?: 0} елементів"
                else
                    "${formatSize(file.length())} • ${dateFormat.format(Date(file.lastModified()))}",
                onClick = {
                    if (file.isDirectory) navigateTo(file)
                    else openFile(file)
                }
            ))
        }
    }

    private fun fileRow(icon: String, name: String, info: String, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            setBackgroundColor(Color.TRANSPARENT)
        }

        row.addView(TextView(this).apply {
            text = icon
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
        })

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dpToPx(12)
            }
        }
        textCol.addView(TextView(this).apply {
            text = name
            textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
        })
        textCol.addView(TextView(this).apply {
            text = info
            textSize = 11f
            setTextColor(Color.parseColor("#8A8A9A"))
        })

        row.addView(textCol)

        // Розділювач знизу
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        wrapper.addView(row)
        wrapper.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also {
                it.marginStart = dpToPx(60)
            }
        })
        return wrapper
    }

    private fun openFile(file: File) {
        Toast.makeText(this, "Відкрити: ${file.name}", Toast.LENGTH_SHORT).show()
        // TODO: текстовий редактор, переглядач зображень тощо
    }

    private fun getFileIcon(name: String) = when {
        name.endsWith(".lua") -> "📜"
        name.endsWith(".lki") -> "📦"
        name.endsWith(".txt") || name.endsWith(".md") -> "📄"
        name.endsWith(".png") || name.endsWith(".jpg") -> "🖼"
        name.endsWith(".json") -> "⚙"
        else -> "📄"
    }

    private fun formatSize(bytes: Long) = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    override fun onSupportNavigateUp(): Boolean {
        if (currentDir != NoxisConstants.BASE_USER_DIR && currentDir.parentFile != null) {
            navigateTo(currentDir.parentFile!!)
            return true
        }
        finish()
        return true
    }

    override fun onBackPressed() {
        if (currentDir != NoxisConstants.BASE_USER_DIR && currentDir.parentFile != null) {
            navigateTo(currentDir.parentFile!!)
        } else {
            super.onBackPressed()
        }
    }
}
