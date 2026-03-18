package com.noxis.os.ui.desktop

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.system.lki.LkiManager
import com.noxis.os.ui.apps.FileManagerActivity
import com.noxis.os.ui.apps.SettingsActivity
import com.noxis.os.ui.cursor.CursorView
import com.noxis.os.ui.window.NoxisWindowManager

class DesktopActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var desktopView: DesktopView
    private lateinit var windowLayer: FrameLayout
    private lateinit var taskbar: TaskbarView
    private lateinit var cursorView: CursorView
    private lateinit var windowManager: NoxisWindowManager
    private lateinit var lkiManager: LkiManager

    companion object {
        private const val REQ_PERMISSIONS = 100
        private const val REQ_MANAGE_STORAGE = 101
        private const val REQ_INSTALL_APP = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Повноекранний режим без статус-бару
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        lkiManager = LkiManager(this)
        buildUI()
        requestPermissions()
    }

    private fun buildUI() {
        // Root container
        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#0D0D0F"))

        // 1. Робочий стіл
        desktopView = DesktopView(this)

        // 2. Шар вікон (поверх столу)
        windowLayer = FrameLayout(this)

        // 3. Тасбар (закріплений знизу)
        taskbar = TaskbarView(this)

        // 4. Курсор (самий верхній шар)
        cursorView = CursorView(this).apply {
            isClickable = false
            isFocusable = false
        }

        // Менеджер вікон
        windowManager = NoxisWindowManager(this, windowLayer)
        windowManager.onWindowListChanged = { windows ->
            runOnUiThread { taskbar.updateWindows(windows) }
        }

        // Прив'язуємо курсор до десктопа
        desktopView.cursorView = cursorView
        cursorView.onCursorClick = { x, y, isDouble ->
            desktopView.handleCursorClick(x, y, isDouble)
        }

        // Запуск застосунку
        desktopView.onAppLaunch = { app -> openApp(app) }

        // Тасбар
        taskbar.onStartClick = { showStartMenu() }
        taskbar.onWindowClick = { entry ->
            windowManager.focus(entry.window)
        }

        // Компонування
        val taskbarHeight = resources.displayMetrics.density.toInt() * 48

        rootLayout.addView(desktopView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        rootLayout.addView(windowLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also { it.bottomMargin = taskbarHeight })

        rootLayout.addView(taskbar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            taskbarHeight,
            android.view.Gravity.BOTTOM
        ))

        rootLayout.addView(cursorView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(rootLayout)
    }

    private fun openApp(app: AppInfo) {
        when (app.id) {
            "com.noxis.system.settings" -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            "com.noxis.system.files" -> {
                startActivity(Intent(this, FileManagerActivity::class.java))
            }
            else -> {
                windowManager.openApp(app)
            }
        }
    }

    private fun showStartMenu() {
        // TODO: StartMenuView — в наступних версіях
        Toast.makeText(this, "Start Menu — буде у v2", Toast.LENGTH_SHORT).show()
    }

    private fun loadApps() {
        val installed = lkiManager.loadInstalledApps().toMutableList()

        // Системні застосунки завжди присутні
        installed.add(0, AppInfo(
            id = "com.noxis.system.files",
            name = "Файли",
            version = "1.0",
            author = "Noxis",
            description = "Файловий менеджер",
            icon = null,
            installDir = "",
            mainScript = "",
            gridCol = 0,
            gridRow = 0
        ))
        installed.add(0, AppInfo(
            id = "com.noxis.system.settings",
            name = "Налаштування",
            version = "1.0",
            author = "Noxis",
            description = "Системні налаштування",
            icon = null,
            installDir = "",
            mainScript = "",
            gridCol = 1,
            gridRow = 0
        ))

        desktopView.loadApps(installed)
    }

    // --- Дозволи ---

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, REQ_MANAGE_STORAGE)
            } else {
                onPermissionsGranted()
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val denied = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (denied.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, denied.toTypedArray(), REQ_PERMISSIONS)
            } else {
                onPermissionsGranted()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            onPermissionsGranted()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_MANAGE_STORAGE -> onPermissionsGranted()
            REQ_INSTALL_APP -> {
                if (resultCode == Activity.RESULT_OK) {
                    val appId = data?.getStringExtra("app_id")
                    if (appId != null) {
                        // Перезавантажуємо застосунки після встановлення
                        loadApps()
                    }
                }
            }
        }
    }

    private fun onPermissionsGranted() {
        // Створюємо директорії якщо не існують
        com.noxis.os.util.NoxisConstants.BASE_USER_DIR.mkdirs()
        com.noxis.os.util.NoxisConstants.APPS_DIR.mkdirs()
        com.noxis.os.util.NoxisConstants.USER_FILES_DIR.mkdirs()

        loadApps()
    }

    override fun onBackPressed() {
        // Блокуємо Back — це ОС, не звичайний додаток
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.closeAll()
    }
}
