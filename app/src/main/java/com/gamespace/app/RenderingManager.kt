package com.gamespace.app

/**
 * Switches the actual Android HWUI/GPU rendering backend using the same
 * `debug.*` system properties Android's own renderer reads at process
 * start (these are the real, documented AOSP debug properties — not a
 * private trick). Requires a privileged shell (Shizuku or root), since a
 * normal app can't call `setprop` for namespaces outside its own process.
 *
 * IMPORTANT, honestly: `debug.hwui.renderer` and the composition-type /
 * MSAA / swap-interval properties are read by an app's graphics pipeline
 * when its process starts. Changing them does NOT repaint an already
 * running app's current frames with a new backend — the target game must
 * be restarted (or not yet running) for the new renderer to actually take
 * effect. This mirrors how every such tool actually behaves; there is no
 * way to hot-swap a running process's GPU backend without restarting it.
 * `GameExitMonitorService` restarts the game's renderer/resolution state
 * back to default automatically once you leave it, so the next launch is
 * clean either way.
 */
object RenderingManager {

    enum class Renderer(val propValue: String, val label: String) {
        DEFAULT("", "Default"),
        SKIA_GL("skiagl", "SkiaGL"),
        SKIA_VULKAN("skiavk", "SkiaVK (Vulkan)"),
        VULKAN("vulkan", "Vulkan"),
        OPENGLES("opengles", "OpenGL ES")
    }

    fun rendererByKey(key: String): Renderer =
        Renderer.values().firstOrNull { it.propValue == key } ?: Renderer.DEFAULT

    /** Applies the HWUI renderer backend. Empty/DEFAULT clears the override. */
    fun applyRenderer(renderer: Renderer): Boolean {
        val cmd = if (renderer == Renderer.DEFAULT) {
            "setprop debug.hwui.renderer \"\""
        } else {
            "setprop debug.hwui.renderer ${renderer.propValue}"
        }
        return PrivilegedExec.run(cmd) != null
    }

    /** VSync off trades tearing risk for lower input latency / higher measured FPS. */
    fun setVsyncOff(disabled: Boolean): Boolean {
        val value = if (disabled) 0 else 1
        return PrivilegedExec.run("setprop debug.egl.swapinterval $value") != null
    }

    fun setForceMsaa(enabled: Boolean): Boolean {
        return PrivilegedExec.run("setprop debug.egl.force_msaa $enabled") != null
    }

    enum class CompositionType(val propValue: String, val label: String) {
        DEFAULT("dyn", "Default (dynamic)"),
        GPU("gpu", "Force GPU"),
        CPU("cpu", "Force CPU")
    }

    fun setCompositionType(type: CompositionType): Boolean {
        return PrivilegedExec.run("setprop debug.composition.type ${type.propValue}") != null
    }

    /** Resets every property this manager can set back to Android's defaults. */
    fun resetAll(): Boolean {
        val ok1 = PrivilegedExec.run("setprop debug.hwui.renderer \"\"") != null
        val ok2 = PrivilegedExec.run("setprop debug.egl.swapinterval 1") != null
        val ok3 = PrivilegedExec.run("setprop debug.egl.force_msaa false") != null
        val ok4 = PrivilegedExec.run("setprop debug.composition.type dyn") != null
        return ok1 && ok2 && ok3 && ok4
    }
}
