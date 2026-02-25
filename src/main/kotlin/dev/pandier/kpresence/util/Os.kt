package dev.pandier.kpresence.util

import java.lang.System.getenv

internal val isUnix: Boolean by lazy {
    !System.getProperty("os.name").lowercase().startsWith("windows")
}

internal val defaultUnixPaths
    get() = listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
        .mapNotNull { getenv(it) }
        .plus("/tmp")
        .flatMap { base -> listOf(base, "$base/app/com.discordapp.Discord", "$base/snap.discord") }
