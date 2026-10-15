package com.rootgenie.app.modules

import com.rootgenie.app.FileSpec
import com.rootgenie.app.ModuleSpec

/**
 * Built-in offline module templates — work with zero internet & zero AI.
 */
object ModuleTemplates {

    fun performanceProps(): ModuleSpec = ModuleSpec(
        id = "rg_perf_tweaks",
        name = "RootGenie Performance Tweaks",
        version = "v1.0",
        author = "RootGenie",
        description = "Smoothness & responsiveness system.prop tweaks",
        props = listOf(
            "debug.hwui.renderer=skiavk",
            "ro.sys.fw.bg_apps_limit=32",
            "ro.min.fling_velocity=8000",
            "ro.max.fling_velocity=12000",
            "persist.sys.ui.hw_accel=1",
            "pm.sleep_mode=1",
            "ro.ril.disable.power.collapse=0"
        ).joinToString("\n"),
        script = "",
        files = emptyList()
    )

    fun batteryProps(): ModuleSpec = ModuleSpec(
        id = "rg_battery_tweaks",
        name = "RootGenie Battery Tweaks",
        version = "v1.0",
        author = "RootGenie",
        description = "Battery-friendly background & radio tweaks",
        props = listOf(
            "ro.sys.fw.bg_apps_limit=24",
            "wifi.supplicant_scan_interval=180",
            "pm.sleep_mode=1",
            "ro.mot.eri.losalert.delay=1000"
        ).joinToString("\n"),
        script = "",
        files = emptyList()
    )

    fun arabicFontNote(): ModuleSpec = ModuleSpec(
        id = "rg_font_note",
        name = "RootGenie Font Module (Starter)",
        version = "v1.0",
        author = "RootGenie",
        description = "Starter font module — drop your .ttf files via AI chat, then rebuild",
        props = "",
        script = """
            ui_print "- RootGenie font module starter"
            ui_print "- Ask the AI chat: 'ابني لي موديول خط عربي' and attach font info"
            ui_print "- For now this module installs nothing extra."
        """.trimIndent() + "\n",
        files = listOf(
            FileSpec("system/fonts/README_RootGenie.txt",
                "Put Arabic .ttf font files here via a generated module.\n")
        )
    )

    val all: List<Pair<String, () -> ModuleSpec>> = listOf(
        "Performance" to { performanceProps() },
        "Battery" to { batteryProps() },
        "Font starter" to { arabicFontNote() }
    )
}
