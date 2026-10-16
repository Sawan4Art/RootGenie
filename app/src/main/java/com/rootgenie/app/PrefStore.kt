package com.rootgenie.app

import android.content.Context
import android.content.SharedPreferences
import com.rootgenie.app.BuildConfig

/**
 * Tiny SharedPreferences wrapper for app configuration.
 *
 * Two layers:
 *  - Raw getters: exactly what the user saved (used by Settings UI fields).
 *  - Effective getters (*Eff): user value if set, otherwise the BUILT-IN provider
 *    injected at build time — so the app works out of the box with zero setup.
 */
object PrefStore {

    private const val FILE = "rootgenie_prefs"
    private const val KEY_API_BASE = "api_base"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model_name"
    private const val KEY_STRICT = "strict_mode"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getApiBase(ctx: Context): String =
        prefs(ctx).getString(KEY_API_BASE, "") ?: ""

    fun setApiBase(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_API_BASE, v.trim()).apply()

    fun getApiKey(ctx: Context): String =
        prefs(ctx).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_API_KEY, v.trim()).apply()

    fun getModel(ctx: Context): String =
        prefs(ctx).getString(KEY_MODEL, "") ?: ""

    fun setModel(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_MODEL, v.trim()).apply()

    fun isStrict(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_STRICT, true)

    fun setStrict(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_STRICT, v).apply()

    // ---- Built-in provider (build-time injected) ----

    fun hasBuiltinKey(): Boolean = BuildConfig.BUILTIN_AI_KEY.isNotBlank()

    /** True when the app is running on the built-in key (user has no personal key saved). */
    fun usesBuiltin(ctx: Context): Boolean =
        hasBuiltinKey() && getApiKey(ctx).isBlank()

    fun getApiBaseEff(ctx: Context): String =
        getApiBase(ctx).ifBlank { BuildConfig.BUILTIN_AI_BASE }

    fun getApiKeyEff(ctx: Context): String =
        getApiKey(ctx).ifBlank { BuildConfig.BUILTIN_AI_KEY }

    fun getModelEff(ctx: Context): String =
        getModel(ctx).ifBlank { BuildConfig.BUILTIN_AI_MODEL }
}
