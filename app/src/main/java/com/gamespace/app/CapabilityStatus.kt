package com.gamespace.app

/**
 * Every feature in GS Renji reports one of these instead of silently
 * assuming it works. UI surfaces must read this before claiming a feature
 * is active — a feature that can't be verified reports UNSUPPORTED or
 * PERMISSION_REQUIRED, never a fabricated "working" state.
 */
enum class CapabilityStatus(val label: String) {
    SUPPORTED("Supported"),
    PARTIALLY_SUPPORTED("Partially Supported"),
    REQUIRES_ROOT("Requires Root"),
    REQUIRES_SHIZUKU("Requires Shizuku"),
    PERMISSION_REQUIRED("Permission Required"),
    UNSUPPORTED("Unsupported")
}

/** A status plus the human-readable reason, shown together in the UI. */
data class CapabilityResult(
    val status: CapabilityStatus,
    val reason: String
)
