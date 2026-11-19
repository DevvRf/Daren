package com.gamespace.app

import org.json.JSONObject

enum class CrosshairShape { DOT, CROSS, CIRCLE }

data class CrosshairStyle(
    val shape: CrosshairShape = CrosshairShape.CROSS,
    val colorHex: String = "#FF1B3D",
    val sizeDp: Int = 24,
    val thicknessDp: Int = 2,
    val opacityPercent: Int = 100,
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shape", shape.name)
        put("colorHex", colorHex)
        put("sizeDp", sizeDp)
        put("thicknessDp", thicknessDp)
        put("opacityPercent", opacityPercent)
        put("offsetXDp", offsetXDp)
        put("offsetYDp", offsetYDp)
    }

    companion object {
        fun fromJson(obj: JSONObject?): CrosshairStyle {
            if (obj == null) return CrosshairStyle()
            return CrosshairStyle(
                shape = try { CrosshairShape.valueOf(obj.optString("shape", "CROSS")) } catch (e: Exception) { CrosshairShape.CROSS },
                colorHex = obj.optString("colorHex", "#FF1B3D"),
                sizeDp = obj.optInt("sizeDp", 24),
                thicknessDp = obj.optInt("thicknessDp", 2),
                opacityPercent = obj.optInt("opacityPercent", 100),
                offsetXDp = obj.optInt("offsetXDp", 0),
                offsetYDp = obj.optInt("offsetYDp", 0)
            )
        }
    }
}
