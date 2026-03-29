package io.github.pandier.kpresence.util

import java.lang.System.getenv

internal val isUnix: Boolean by lazy {
    !System.getProperty("os.name").lowercase().startsWith("windows")
}

internal val defaultUnixPaths: List<String>
    get() {
        val tmp = getenv("XDG_RUNTIME_DIR")
            ?: getenv("TMPDIR")
            ?: getenv("TMP")
            ?: getenv("TEMP")
            ?: "/tmp"
        return listOf(tmp, "$tmp/app/com.discordapp.Discord", "$tmp/snap.discord")
    }
