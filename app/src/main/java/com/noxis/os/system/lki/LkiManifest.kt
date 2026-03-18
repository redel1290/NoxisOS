package com.noxis.os.system.lki

import com.google.gson.annotations.SerializedName

/**
 * manifest.json всередині .lki пакету
 *
 * Приклад:
 * {
 *   "id": "com.example.myapp",
 *   "name": "My App",
 *   "version": "1.0.0",
 *   "author": "Someone",
 *   "description": "Опис застосунку",
 *   "main": "main.lua",
 *   "icon": "icon.png",
 *   "permissions": ["files", "network"]
 * }
 */
data class LkiManifest(
    @SerializedName("id")          val id: String = "",
    @SerializedName("name")        val name: String = "",
    @SerializedName("version")     val version: String = "1.0.0",
    @SerializedName("author")      val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("main")        val main: String = "main.lua",
    @SerializedName("icon")        val icon: String = "icon.png",
    @SerializedName("permissions") val permissions: List<String> = emptyList()
) {
    fun isValid(): Boolean = id.isNotBlank() && name.isNotBlank()
}
