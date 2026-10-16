package com.rootgenie.app

/**
 * Three-tier safety gate for every command before it touches the shell.
 * SAFE    → runs instantly (read-only)
 * CONFIRM → requires explicit user confirmation (changes settings/apps)
 * BLOCKED → refused always (destructive / bricking potential)
 */
object SafetyChecker {

    enum class Risk { SAFE, CONFIRM, BLOCKED }

    data class Verdict(val risk: Risk, val reason: String)

    private val BLOCKED = listOf(
        Regex("""rm\s+(-[a-zA-Z]*\s+)*-?[a-zA-Z]*[rf][a-zA-Z]*\s+/(system|data|vendor|product|odm|storage|sdcard)"""),
        Regex("""rm\s+-[a-zA-Z]*r[a-zA-Z]*f[a-zA-Z]*\s+/\s*$"""),
        Regex("""\bdd\b.*\bof=/dev/(block|mmcblk|sd[a-z]|by-name)"""),
        Regex("""\bmkfs\b|\bmkfs\.\w+"""),
        Regex("""\bflash_(image|all)\b|\bfastboot\b"""),
        Regex("""mount\s+.*-o\s+.*rw.*\s+/(system|vendor)\b"""),
        Regex("""\bwm\s+size\s+reset?"""),
        Regex("""\bchmod\s+(-R\s+)?777\s+/\s*$"""),
        Regex("""\bdd\s+if=/dev/(zero|random)\b"""),
        Regex("""\bexport\s+PATH=""")
    )

    private val CONFIRM = listOf(
        Regex("""^settings\s+(put|delete)"""),
        Regex("""^pm\s+(uninstall|disable|suspend|clear|hide|reinstall|install-existing)"""),
        Regex("""^am\s+(force-stop|kill|kill-all)"""),
        Regex("""^wm\s+(density|size)"""),
        Regex("""^svc\s+(wifi|data|bluetooth|usb|nfc)\s+(enable|disable)"""),
        Regex("""^cmd\s+(uimode|package|activity)"""),
        Regex("""^reboot\b|^shutdown\b"""),
        Regex("""^setprop\b"""),
        Regex("""^setenforce\b"""),
        Regex("""^magisk\s+--install-module"""),
        Regex("""^magisk\s+--remove-modules"""),
        Regex("""^rm\s"""),
        Regex("""\bcp\b.*\s+/(system|vendor|product)/"""),
        Regex("""\bmount\b"""),
        Regex("""\bchmod\b"""),
        Regex("""\bchown\b"""),
        Regex("""\bkill\b""")
    )

    private val SAFE = listOf(
        Regex("""^getprop\b"""),
        Regex("""^settings\s+get\b"""),
        Regex("""^settings\s+list\b"""),
        Regex("""^pm\s+(list|path|resolve-activity|dump)\b"""),
        Regex("""^dumpsys\b"""),
        Regex("""^am\s+start\b"""),
        Regex("""^ls\b|^cat\b(?!.*\s/(system|data|vendor)/.*>)"""), // cat on readable paths ok
        Regex("""^id\b|^whoami\b|^uname\b"""),
        Regex("""^magisk\s+-[vc]"""),
        Regex("""^getenforce\b"""),
        Regex("""^df\b|^free\b|^top\s+-n\s+1"""),
        Regex("""^logcat\s+-d\b"""),
        Regex("""^uptime\b"""),
        Regex("""^cmd\s+(uimode\s+night|wifi|notification)\b.*(get|no\b)?""")
    )

    fun check(cmd: String): Verdict {
        val c = cmd.trim()
        if (c.contains("\n")) return Verdict(Risk.BLOCKED, "multi-line command rejected")
        for (r in BLOCKED) if (r.containsMatchIn(c))
            return Verdict(Risk.BLOCKED, "destructive command pattern")
        for (r in SAFE) if (r.containsMatchIn(c)) return Verdict(Risk.SAFE, "read-only")
        for (r in CONFIRM) if (r.containsMatchIn(c)) return Verdict(Risk.CONFIRM, "modifies system state")
        // Unknown commands default to CONFIRM (never auto-run unknown root commands)
        return Verdict(Risk.CONFIRM, "unknown command — manual confirmation required")
    }
}
