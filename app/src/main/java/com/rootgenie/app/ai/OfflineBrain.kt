package com.rootgenie.app.ai

import com.rootgenie.app.AgentEngine
import com.rootgenie.app.AgentResult
import com.rootgenie.app.Command

/**
 * Offline brain — instant local command recognition without internet.
 * Covers the most common modder actions in Arabic & English.
 * Returns null when no confident match → falls back to the cloud brain.
 */
object OfflineBrain {

    private data class Rule(val regex: Regex, val build: (MatchResult) -> AgentResult)

    private val knownApps = mapOf(
        "واتس" to "com.whatsapp", "whats" to "com.whatsapp",
        "يوتيوب" to "com.google.android.youtube", "youtube" to "com.google.android.youtube",
        "انستا" to "com.instagram.android", "insta" to "com.instagram.android",
        "تليجرام" to "org.telegram.messenger", "telegram" to "org.telegram.messenger",
        "تيك توك" to "com.zhiliaoapp.musically", "tiktok" to "com.zhiliaoapp.musically",
        "فيسبوك" to "com.facebook.katana", "facebook" to "com.facebook.katana",
        "كاميرا" to "com.android.camera", "camera" to "com.android.camera"
    )

    private val rules = listOf(
        Rule(Regex("""(?:كثافة|دقة|density|dpi)\D*(\d{3,4})""")) { m ->
            AgentResult.ShellPlan("تغيير كثافة الشاشة إلى ${m.groupValues[1]}",
                listOf(Command("wm density ${m.groupValues[1]}", "confirm")))
        },
        Rule(Regex("""(?:اقفل|اقفلوا|سكي|سكه|اقفه|force.?stop)\s+(?:تطبيق\s+)?(\S+)""")) { m ->
            val kw = m.groupValues[1].lowercase()
            val pkg = knownApps[kw] ?: (if (kw.contains(".")) kw else null)
            if (pkg != null) AgentResult.ShellPlan("إغلاق $pkg من الخلفية",
                listOf(Command("am force-stop $pkg", "confirm")))
            else AgentResult.Chat("مش عارف الباكدج بتاع «${m.groupValues[1]}» — اكتب اسم الباكدج الكامل أو اقل مني أبحث.")
        },
        Rule(Regex("""(?:البطارية|battery|شحن)""")) { _ ->
            AgentResult.ShellPlan("حالة البطارية:", listOf(Command("dumpsys battery", "safe")))
        },
        Rule(Regex("""(?:الوضع الليلي|داكن|dark.?mode|night)""")) { _ ->
            AgentResult.ShellPlan("تبديل الوضع الليلي:",
                listOf(Command("cmd uimode night auto", "confirm")))
        },
        Rule(Regex("""(?:اعمل|استخرج|هات).*(?:قائمة|list).*(?:تطبيقات|apps)""")) { _ ->
            AgentResult.ShellPlan("تطبيقات المستخدم المثبتة:",
                listOf(Command("pm list packages -3", "safe")))
        },
        Rule(Regex("""(?:روت|root|صلاحيات)""", RegexOption.IGNORE_CASE)) { _ ->
            AgentResult.ShellPlan("فحص صلاحيات الروت:",
                listOf(Command("id", "safe"), Command("magisk -v", "safe")))
        },
        Rule(Regex("""(?:نسخة|إصدار|version).*(?:اندرويد|أندرويد|android)""")) { _ ->
            AgentResult.ShellPlan("نسخة أندرويد:",
                listOf(Command("getprop ro.build.version.release", "safe")))
        },
        Rule(Regex("""(?:ريستارت|اعادة تشغيل|إعادة تشغيل|reboot)""")) { _ ->
            AgentResult.ShellPlan("إعادة تشغيل الجهاز — محتاج تأكيدك:",
                listOf(Command("reboot", "confirm")))
        }
    )

    fun parse(input: String): AgentResult? {
        val t = input.trim()
        for (r in rules) {
            r.regex.find(t)?.let { m -> return r.build(m) }
        }
        return null
    }
}
