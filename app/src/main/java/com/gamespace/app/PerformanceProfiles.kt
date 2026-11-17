package com.gamespace.app

/**
 * These profiles only combine settings this app can ACTUALLY apply
 * through legitimate, already-implemented paths (renderer, composition
 * type, resolution downscale, MSAA, vsync). There is no overclocking, no
 * voltage/frequency table editing, and no disabling of thermal
 * protection anywhere in this app — those require kernel-level access
 * this app does not have and intentionally does not attempt to get.
 */
enum class PerformanceProfile(val label: String) {
    PERFORMANCE("Performance"),
    BALANCED("Balanced"),
    BATTERY_SAVER("Battery Saver"),
    CUSTOM("Custom")
}

data class ProfileSettings(
    val renderer: RenderingManager.Renderer,
    val compositionType: RenderingManager.CompositionType,
    val vsyncOff: Boolean,
    val forceMsaa: Boolean,
    val downscalePercent: Int
)

object PerformanceProfiles {

    fun settingsFor(profile: PerformanceProfile, customDownscale: Int = 100): ProfileSettings = when (profile) {
        PerformanceProfile.PERFORMANCE -> ProfileSettings(
            renderer = RenderingManager.Renderer.VULKAN,
            compositionType = RenderingManager.CompositionType.GPU,
            vsyncOff = true,
            forceMsaa = false,
            downscalePercent = 85
        )
        PerformanceProfile.BALANCED -> ProfileSettings(
            renderer = RenderingManager.Renderer.DEFAULT,
            compositionType = RenderingManager.CompositionType.DEFAULT,
            vsyncOff = false,
            forceMsaa = false,
            downscalePercent = 100
        )
        PerformanceProfile.BATTERY_SAVER -> ProfileSettings(
            renderer = RenderingManager.Renderer.DEFAULT,
            compositionType = RenderingManager.CompositionType.CPU,
            vsyncOff = false,
            forceMsaa = false,
            downscalePercent = 75
        )
        PerformanceProfile.CUSTOM -> ProfileSettings(
            renderer = RenderingManager.Renderer.DEFAULT,
            compositionType = RenderingManager.CompositionType.DEFAULT,
            vsyncOff = false,
            forceMsaa = false,
            downscalePercent = customDownscale
        )
    }

    /**
     * Applies via the real managers already in this app (renderer/
     * composition/vsync/MSAA). The resolution downscale is applied
     * separately by the caller via DisplayScaler, since that needs a
     * Context — kept out of this function to avoid an unused parameter.
     */
    fun apply(settings: ProfileSettings) {
        RenderingManager.applyRenderer(settings.renderer)
        RenderingManager.setCompositionType(settings.compositionType)
        RenderingManager.setVsyncOff(settings.vsyncOff)
        RenderingManager.setForceMsaa(settings.forceMsaa)
    }
}
