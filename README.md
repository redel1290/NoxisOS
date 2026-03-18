# Noxis OS

Нативна Android ОС — повністю написана на Kotlin без WebView.  
Еволюція Lunyx OS: стабільніша, швидша, без залежності від сервера.

## Архітектура

```
NoxisOS/
├── app/src/main/java/com/noxis/os/
│   ├── ui/
│   │   ├── desktop/
│   │   │   ├── DesktopActivity.kt   ← головна активність
│   │   │   ├── DesktopView.kt       ← Canvas рендер іконок + drag
│   │   │   ├── DesktopGrid.kt       ← snap-to-grid логіка
│   │   │   └── TaskbarView.kt       ← нижня панель
│   │   ├── window/
│   │   │   ├── NoxisWindowView.kt   ← draggable/resizable вікно
│   │   │   └── NoxisWindowManager.kt← менеджер вікон
│   │   ├── cursor/
│   │   │   └── CursorView.kt        ← XFCE4-стиль курсор
│   │   └── apps/
│   │       ├── SettingsActivity.kt  ← налаштування
│   │       └── FileManagerActivity.kt← файловий менеджер
│   ├── system/
│   │   ├── lki/
│   │   │   ├── LkiManager.kt        ← встановлення пакетів
│   │   │   ├── LkiManifest.kt       ← модель маніфесту
│   │   │   ├── AppInfo.kt           ← модель застосунку
│   │   │   └── LkiInstallerActivity.kt
│   │   └── lua/
│   │       └── LuaRuntime.kt        ← LuaJ виконання
│   └── util/
│       ├── NoxisConstants.kt
│       └── DpExtensions.kt
```

## .lki пакети

`.lki` — це ZIP архів перейменований на `.lki`.

**Структура пакету:**
```
myapp.lki  (насправді ZIP)
├── manifest.json   ← обов'язково
├── main.lua        ← точка входу
└── icon.png        ← іконка (опціонально)
```

**manifest.json:**
```json
{
  "id": "com.example.myapp",
  "name": "My App",
  "version": "1.0.0",
  "author": "Author Name",
  "description": "Опис застосунку",
  "main": "main.lua",
  "icon": "icon.png",
  "permissions": ["files"]
}
```

**main.lua — Lua API:**
```lua
-- Змінити заголовок вікна
noxis.setTitle("Hello World")

-- Встановити вміст вікна
noxis.setContent("Привіт від Lua!")

-- Читати/писати файли
local data = noxis.readFile("data.txt")
noxis.writeFile("output.txt", "Hello")

-- Закрити вікно
noxis.close()

-- Інфо про застосунок
noxis.print("App ID: " .. noxis.appId)
```

## Сховище

```
/storage/emulated/0/NoxisOS/
├── apps/           ← встановлені .lki застосунки
│   └── com.example.myapp/
│       ├── meta.json
│       ├── main.lua
│       └── icon.png
└── files/          ← файли користувача
```

## Збірка

```bash
git clone https://github.com/redel1290/noxis-os
cd NoxisOS
chmod +x gradlew
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Курсор

- Тач будь-де → курсор переміщується
- Тап близько до курсора → клік
- Подвійний тап близько до курсора → подвійний клік
- Фізична миша → вбудований курсор ховається, фізична керує

## GitHub Actions

Кожен push на `main` автоматично збирає debug APK.  
Завантажити: `Actions` → останній run → `noxis-os-debug`
