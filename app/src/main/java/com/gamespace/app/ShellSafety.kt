package com.gamespace.app

/**
 * Every PrivilegedExec/RootUtils/ShizukuManager command in this app is a
 * fixed string template — but some templates interpolate a value that
 * ultimately traces back to something the user typed (a custom game's
 * package name in GameProfileStore, for instance). Without validation,
 * a package name like `com.foo; rm -rf /` would turn into a second shell
 * command. Every such value must pass through here before interpolation.
 */
object ShellSafety {

    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val PROP_VALUE_REGEX = Regex("^[a-zA-Z0-9_.\\-]*$")

    /** True only for a syntactically valid Android package name — no shell metacharacters possible. */
    fun isValidPackageName(name: String): Boolean =
        name.length in 1..255 && PACKAGE_NAME_REGEX.matches(name)

    /** True for simple alphanumeric prop/preset values (e.g. "skiavk", "vulkan", "90"). */
    fun isSafePropValue(value: String): Boolean =
        value.length in 0..64 && PROP_VALUE_REGEX.matches(value)

    /**
     * Returns [packageName] if valid, or null. Callers must treat null as
     * "do not run the command" rather than falling back to the raw string.
     */
    fun sanitizedPackageNameOrNull(packageName: String?): String? {
        if (packageName == null) return null
        return if (isValidPackageName(packageName)) packageName else null
    }
}
