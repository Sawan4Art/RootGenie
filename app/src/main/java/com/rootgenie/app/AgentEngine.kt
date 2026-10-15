package com.rootgenie.app

import android.content.Context
import com.rootgenie.app.ai.AIClient
import com.rootgenie.app.ai.OfflineBrain

/** A single shell command planned by the brain. */
data class Command(val cmd: String, val risk: String)

/** One file inside a generated Magisk module. */
data class FileSpec(val path: String, val content: String)

/** Full specification of a generated Magisk module. */
data class ModuleSpec(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val props: String,
    val script: String,
    val files: List<FileSpec>
)

/** What the brain decided to do with the user input. */
sealed class AgentResult {
    data class Chat(val text: String) : AgentResult()
    data class ShellPlan(val reply: String, val commands: List<Command>) : AgentResult()
    data class ModulePlan(val reply: String, val spec: ModuleSpec) : AgentResult()
}

/**
 * The orchestrator: user input → offline brain (instant) → cloud brain (LLM).
 * All results come back as typed plans the UI executes behind the safety gate.
 */
class AgentEngine(private val ctx: Context) {

    suspend fun process(input: String, forceModule: Boolean = false): AgentResult {
        if (!forceModule) {
            OfflineBrain.parse(input)?.let { return it }
        }
        return try {
            val json = AIClient.chat(ctx, input, forceModule)
            when (json.optString("type", "chat")) {
                "shell" -> {
                    val cmds = mutableListOf<Command>()
                    val arr = json.optJSONArray("commands") ?: JSONArray0()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        cmds.add(Command(o.optString("cmd"), o.optString("risk", "confirm")))
                    }
                    AgentResult.ShellPlan(json.optString("reply", ""), cmds)
                }
                "module" -> {
                    val m = json.optJSONObject("module")
                    if (m == null) AgentResult.Chat(json.optString("reply", "الرد ناقص بيانات الموديول."))
                    else {
                        val files = mutableListOf<FileSpec>()
                        val fa = m.optJSONArray("files") ?: JSONArray0()
                        for (i in 0 until fa.length()) {
                            val o = fa.getJSONObject(i)
                            files.add(FileSpec(o.optString("path"), o.optString("content")))
                        }
                        val spec = ModuleSpec(
                            id = m.optString("id", "gen_module").replace(Regex("[^a-z0-9_]"), "_"),
                            name = m.optString("name", "Generated Module"),
                            version = m.optString("version", "v1.0"),
                            author = m.optString("author", "RootGenie"),
                            description = m.optString("description", ""),
                            props = m.optString("props", ""),
                            script = m.optString("script", ""),
                            files = files
                        )
                        AgentResult.ModulePlan(json.optString("reply", "جهزت الموديول!"), spec)
                    }
                }
                else -> AgentResult.Chat(json.optString("reply", ""))
            }
        } catch (e: Exception) {
            AgentResult.Chat("حصلت مشكلة في الاتصال بالعقل السحابي:\n${e.message}\n\nجرّب تاني أو اتأكد من إعدادات الـ API.")
        }
    }

    private fun JSONArray0() = org.json.JSONArray()
}
