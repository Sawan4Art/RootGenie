package com.rootgenie.app

import com.topjohnwu.superuser.Shell

/**
 * Root command execution engine built on libsu (by topjohnwu, Magisk's author).
 * All root operations in RootGenie flow through this single gate.
 */
object RootShell {

    data class Result(val code: Int, val out: List<String>, val err: List<String>) {
        val ok: Boolean get() = code == 0
        fun outputText(maxLines: Int = 18): String {
            val all = (out + err).take(maxLines)
            return if (all.isEmpty()) "(no output)" else all.joinToString("\n")
        }
    }

    fun init() {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(20)
        )
    }

    fun isRoot(): Boolean = try {
        Shell.isAppGrantedRoot() == true
    } catch (e: Exception) {
        false
    }

    fun exec(cmd: String): Result = try {
        val r = Shell.cmd(cmd).exec()
        Result(r.code, ArrayList(r.out), ArrayList(r.err))
    } catch (e: Exception) {
        Result(-1, emptyList(), listOf(e.message ?: "shell error"))
    }

    fun magiskVersion(): String =
        exec("magisk -v").out.firstOrNull()?.trim().takeIf { !it.isNullOrBlank() } ?: "N/A"

    fun installModule(zipPath: String): Result =
        exec("magisk --install-module \"$zipPath\"")
}
