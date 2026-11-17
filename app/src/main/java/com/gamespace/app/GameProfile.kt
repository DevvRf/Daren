package com.gamespace.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * One game's saved customization: crosshair (full style, not just a
 * preset key), color preset, downscale, renderer backend, and whether
 * GameMonitorService should apply/revert this automatically.
 */
data class GameProfile(
    val id: String,
    val displayName: String,
    val packageName: String?, // null for a custom/manual entry with no known package
    val crosshairEnabled: Boolean = false,
    val crosshairStyle: CrosshairStyle = CrosshairStyle(),
    val colorPreset: String = "off",    // matches ColorFilterManager.PRESETS keys
    val downscalePercent: Int = 100,    // 50-100, 100 = native resolution
    val rendererKey: String = "",       // matches RenderingManager.Renderer.propValue ("" = default)
    val monitorMetrics: MonitorMetrics = MonitorMetrics(),
    val autoApply: Boolean = false      // if true, GameMonitorService applies/reverts this automatically
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("displayName", displayName)
        put("packageName", packageName ?: JSONObject.NULL)
        put("crosshairEnabled", crosshairEnabled)
        put("crosshairStyle", crosshairStyle.toJson())
        put("colorPreset", colorPreset)
        put("downscalePercent", downscalePercent)
        put("rendererKey", rendererKey)
        put("monitorMetrics", JSONObject().apply {
            put("fps", monitorMetrics.fps)
            put("cpu", monitorMetrics.cpu)
            put("ram", monitorMetrics.ram)
            put("gpu", monitorMetrics.gpu)
            put("temperature", monitorMetrics.temperature)
        })
        put("autoApply", autoApply)
    }

    companion object {
        fun fromJson(obj: JSONObject): GameProfile {
            val metricsObj = obj.optJSONObject("monitorMetrics")
            val metrics = if (metricsObj != null) {
                MonitorMetrics(
                    fps = metricsObj.optBoolean("fps", true),
                    cpu = metricsObj.optBoolean("cpu", true),
                    ram = metricsObj.optBoolean("ram", true),
                    gpu = metricsObj.optBoolean("gpu", true),
                    temperature = metricsObj.optBoolean("temperature", true)
                )
            } else MonitorMetrics()

            return GameProfile(
                id = obj.getString("id"),
                displayName = obj.getString("displayName"),
                packageName = if (obj.isNull("packageName")) null else obj.getString("packageName"),
                crosshairEnabled = obj.optBoolean("crosshairEnabled", false),
                crosshairStyle = CrosshairStyle.fromJson(obj.optJSONObject("crosshairStyle")),
                colorPreset = obj.optString("colorPreset", "off"),
                downscalePercent = obj.optInt("downscalePercent", 100),
                rendererKey = obj.optString("rendererKey", ""),
                monitorMetrics = metrics,
                autoApply = obj.optBoolean("autoApply", false)
            )
        }

        /** A couple of popular games pre-filled so the home screen isn't empty on first run. */
        fun defaultProfiles(): List<GameProfile> = listOf(
            GameProfile(
                id = "ml",
                displayName = "Mobile Legends: Bang Bang",
                packageName = "com.mobile.legends"
            ),
            GameProfile(
                id = "ff",
                displayName = "Free Fire",
                packageName = "com.dts.freefireth"
            )
        )
    }
}

fun List<GameProfile>.toJsonArray(): String {
    val arr = JSONArray()
    forEach { arr.put(it.toJson()) }
    return arr.toString()
}

fun parseProfileList(json: String): List<GameProfile> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { GameProfile.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }
}
