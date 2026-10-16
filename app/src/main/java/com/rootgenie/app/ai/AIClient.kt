package com.rootgenie.app.ai

import android.content.Context
import com.rootgenie.app.PrefStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cloud brain — talks to any OpenAI-compatible /chat/completions endpoint
 * (OpenAI, Gemini OpenAI-compat, OpenRouter, z.ai, LM Studio, ...).
 */
object AIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """You are RootGenie, an AI assistant living inside a ROOTED Android phone (Magisk).
You help the user: (1) control the device via shell commands, (2) generate Magisk modules.

Respond with ONLY one JSON object, no markdown fences, matching this schema:
{
  "type": "chat" | "shell" | "module",
  "reply": "short friendly user-facing text (reply in Arabic if the user wrote Arabic)",
  "commands": [{"cmd": "exact shell command", "risk": "safe"|"confirm"}],
  "module": {
    "id": "lowercase_underscores_only",
    "name": "Module Name",
    "version": "v1.0",
    "author": "RootGenie",
    "description": "one-line description",
    "props": "system.prop content lines key=value (empty string if none)",
    "script": "customize.sh shell script content (may be empty string)",
    "files": [{"path": "system/relative/path", "content": "text file content"}]
  }
}

HARD RULES:
- NEVER output destructive commands: no dd to block devices, no mkfs, no rm -rf on / or /system or /data, no flashing, no bootloader unlock.
- Use type=shell for device questions/actions with concrete commands in "commands". Read-only commands risk=safe; anything that changes settings/apps risk=confirm.
- Use type=module when the user asks to create/generate a module. Fill "module" fully. customize.sh may use ui_print, set_perm, and the MODPATH variable. Keep files small (text only).
- Use type=chat for greetings, explanations, or anything needing no action.
- Unknown package names: use "pm list packages | grep keyword" first (risk=safe)."""

    suspend fun chat(ctx: Context, userText: String, forceModule: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            val base = PrefStore.getApiBase(ctx).trimEnd('/')
            val url = when {
                base.endsWith("/chat/completions") -> base
                Regex("/v\\d+$").containsMatchIn(base) -> "$base/chat/completions"
                else -> "$base/v1/chat/completions"
            }

            val sys = if (forceModule) SYSTEM_PROMPT +
                "\n- The user request is a MODULE REQUEST: respond with type=module." else SYSTEM_PROMPT

            val body = JSONObject().apply {
                put("model", PrefStore.getModel(ctx))
                put("temperature", 0.3)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", sys))
                    put(JSONObject().put("role", "user").put("content", userText))
                })
            }

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${PrefStore.getApiKey(ctx)}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            resp.use { r ->
                val text = r.body?.string() ?: "{}"
                if (!r.isSuccessful) {
                    throw RuntimeException("API ${r.code}: ${text.take(300)}")
                }
                val root = JSONObject(text)
                val content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                // Lenient parse: weak free models sometimes wrap JSON in chatter
                val clean = content.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()
                val start = clean.indexOf('{')
                val end = clean.lastIndexOf('}')
                if (start < 0 || end <= start) throw RuntimeException("الرد مش JSON: ${clean.take(120)}")
                JSONObject(clean.substring(start, end + 1))
            }
        }

    /**
     * Fetch available model IDs from any OpenAI-compatible provider
     * (Groq, OpenRouter, Gemini-compat, Mistral, LM Studio, Ollama, ...).
     */
    suspend fun models(ctx: Context): List<String> = withContext(Dispatchers.IO) {
        val base = PrefStore.getApiBase(ctx).trimEnd('/')
        val url = when {
            base.endsWith("/chat/completions") -> base.removeSuffix("/chat/completions") + "/models"
            Regex("/v\\d+$").containsMatchIn(base) -> "$base/models"
            else -> "$base/v1/models"
        }
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${PrefStore.getApiKey(ctx)}")
            .get()
            .build()
        val resp = client.newCall(req).execute()
        resp.use { r ->
            val text = r.body?.string() ?: "{}"
            if (!r.isSuccessful) throw RuntimeException("API ${r.code}: ${text.take(200)}")
            val data = JSONObject(text).optJSONArray("data") ?: JSONArray()
            val ids = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id") ?: continue
                if (id.isNotBlank()) ids.add(id)
            }
            ids.sorted()
        }
    }
}
