package com.rootgenie.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Tiny SharedPreferences wrapper for app configuration.
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
        prefs(ctx).getString(KEY_API_BASE, "https://api.openai.com/v1") ?: "https://api.openai.com/v1"

    fun setApiBase(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_API_BASE, v.trim()).apply()

    fun getApiKey(ctx: Context): String =
        prefs(ctx).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_API_KEY, v.trim()).apply()

    fun getModel(ctx: Context): String =
        prefs(ctx).getString(KEY_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"

    fun setModel(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_MODEL, v.trim()).apply()

    fun isStrict(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_STRICT, true)

    fun setStrict(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_STRICT, v).apply()
}
