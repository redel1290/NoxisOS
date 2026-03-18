package com.noxis.os.ui.apps

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.noxis.os.R
import com.noxis.os.util.NoxisConstants
import com.noxis.os.util.dpToPx

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREF_NAME = "noxis_settings"
        const val KEY_GRID_SNAP = "grid_snap"
        const val KEY_CURSOR_ENABLED = "cursor_enabled"
        const val KEY_SHOW_GRID_DEBUG = "grid_debug"
        const val KEY_SERVER_ENABLED = "server_enabled"
        const val KEY_SERVER_PORT = "server_port"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_LANGUAGE = "language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#0D0D0F"))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(32))
        }

        layout.addView(sectionTitle("Інтерфейс"))

        layout.addView(switchRow(
            "Прив'язка до сітки",
            "Іконки прилипають до сітки при перетягуванні",
            KEY_GRID_SNAP, true
        ))
        layout.addView(switchRow(
            "Курсор",
            "Показувати курсор у стилі XFCE4",
            KEY_CURSOR_ENABLED, true
        ))
        layout.addView(switchRow(
            "Показати сітку (debug)",
            "Відображати лінії сітки на робочому столі",
            KEY_SHOW_GRID_DEBUG, false
        ))

        layout.addView(divider())
        layout.addView(sectionTitle("Мережа"))

        layout.addView(switchRow(
            "Серверне підключення",
            "Дозволити підключення до Noxis сервера",
            KEY_SERVER_ENABLED, false
        ))

        layout.addView(inputRow(
            "Порт сервера",
            KEY_SERVER_PORT,
            NoxisConstants.DEFAULT_SERVER_PORT.toString()
        ))

        layout.addView(divider())
        layout.addView(sectionTitle("Мова"))

        layout.addView(spinnerRow(
            "Мова інтерфейсу",
            KEY_LANGUAGE,
            listOf("Українська", "English"),
            listOf("uk", "en")
        ))

        layout.addView(divider())
        layout.addView(sectionTitle("Про систему"))

        layout.addView(infoRow("Версія", "Noxis OS 1.0.0"))
        layout.addView(infoRow("Сітка", "${NoxisConstants.GRID_COLUMNS}×${NoxisConstants.GRID_ROWS}"))
        layout.addView(infoRow("Шлях даних", NoxisConstants.BASE_USER_DIR.absolutePath))

        scroll.addView(layout)
        setContentView(scroll)

        // Тайтлбар
        supportActionBar?.apply {
            title = getString(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // --- UI будівники ---

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#C8AAFF"))
        setPadding(0, dpToPx(8), 0, dpToPx(4))
    }

    private fun switchRow(title: String, subtitle: String, key: String, default: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
        })
        texts.addView(TextView(this).apply {
            text = subtitle
            textSize = 11f
            setTextColor(Color.parseColor("#8A8A9A"))
        })

        val sw = Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
            thumbTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#7B5EA7")
            )
        }

        row.addView(texts)
        row.addView(sw)
        return row
    }

    private fun inputRow(title: String, key: String, default: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        row.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        row.addView(EditText(this).apply {
            setText(prefs.getString(key, default))
            textSize = 13f
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#8A8A9A"))
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) prefs.edit().putString(key, text.toString()).apply()
            }
        })

        return row
    }

    private fun spinnerRow(title: String, key: String, labels: List<String>, values: List<String>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        row.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val saved = prefs.getString(key, values[0])
        val currentIndex = values.indexOf(saved).coerceAtLeast(0)

        row.addView(Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item, labels
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(currentIndex)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    prefs.edit().putString(key, values[pos]).apply()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })

        return row
    }

    private fun infoRow(label: String, value: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        row.addView(TextView(this).apply {
            text = "$label: "
            textSize = 13f
            setTextColor(Color.parseColor("#8A8A9A"))
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.parseColor("#F0F0F5"))
        })
        return row
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#2A2A35"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        ).also { it.setMargins(0, dpToPx(12), 0, dpToPx(12)) }
    }
}
