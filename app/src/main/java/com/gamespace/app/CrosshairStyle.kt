package com.gamespace.app

import org.json.JSONObject

enum class CrosshairShape { DOT, CROSS, CIRCLE }

data class CrosshairStyle(
    val shape: CrosshairShape = CrosshairShape.CROSS,
    val colorHex: String = "#39FF6A",
    val sizeDp: Int = 24,
    val thicknessDp: Int = 2,
    val opacityPercent: Int = 100
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shape", shape.name)
        put("colorHex", colorHex)
        put("sizeDp", sizeDp)
        put("thicknessDp", thicknessDp)
        put("opacityPercent", opacityPercent)
    }

    companion object {
        fun fromJson(obj: JSONObject?): CrosshairStyle {
            if (obj == null) return CrosshairStyle()
            return CrosshairStyle(
                shape = try { CrosshairShape.valueOf(obj.optString("shape", "CROSS")) } catch (e: Exception) { CrosshairShape.CROSS },
                colorHex = obj.optString("colorHex", "#39FF6A"),
                sizeDp = obj.optInt("sizeDp", 24),
                thicknessDp = obj.optInt("thicknessDp", 2),
                opacityPercent = obj.optInt("opacityPercent", 100)
            )
        }
    }
}
