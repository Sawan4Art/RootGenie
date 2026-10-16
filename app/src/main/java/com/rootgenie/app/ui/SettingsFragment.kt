package com.rootgenie.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.rootgenie.app.PrefStore
import com.rootgenie.app.R
import com.rootgenie.app.ai.AIClient
import com.rootgenie.app.BuildConfig
import kotlinx.coroutines.runBlocking

class SettingsFragment : Fragment() {

    private data class Preset(val base: String, val model: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val base = view.findViewById<EditText>(R.id.ed_api_base)
        val key = view.findViewById<EditText>(R.id.ed_api_key)
        val model = view.findViewById<EditText>(R.id.ed_model)
        val strict = view.findViewById<Switch>(R.id.sw_strict)
        val status = view.findViewById<TextView>(R.id.tv_test_status)
        val testBtn = view.findViewById<Button>(R.id.btn_test)

        base.setText(PrefStore.getApiBase(requireContext()))
        key.setText(PrefStore.getApiKey(requireContext()))
        model.setText(PrefStore.getModel(requireContext()))
        strict.isChecked = PrefStore.isStrict(requireContext())

        val builtinStatus = view.findViewById<TextView>(R.id.tv_builtin_status)

        fun refreshBuiltinUi() {
            val ctx = requireContext()
            builtinStatus.text = when {
                PrefStore.usesBuiltin(ctx)  -> getString(R.string.builtin_active)
                PrefStore.hasBuiltinKey()   -> getString(R.string.builtin_key_saved)
                else                        -> getString(R.string.builtin_missing)
            }
            // Show the built-in values as hints so the user sees what is active without it being saved
            base.hint  = if (PrefStore.hasBuiltinKey()) BuildConfig.BUILTIN_AI_BASE else ""
            model.hint = if (PrefStore.hasBuiltinKey()) BuildConfig.BUILTIN_AI_MODEL else ""
            key.hint   = if (PrefStore.usesBuiltin(ctx)) getString(R.string.builtin_key_hint) else ""
        }
        refreshBuiltinUi()

        view.findViewById<Button>(R.id.btn_save).setOnClickListener {
            PrefStore.setApiBase(requireContext(), base.text.toString())
            PrefStore.setApiKey(requireContext(), key.text.toString())
            PrefStore.setModel(requireContext(), model.text.toString())
            PrefStore.setStrict(requireContext(), strict.isChecked)
            refreshBuiltinUi()
            Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show()
        }

        // ---- Free provider presets: tap to auto-fill endpoint + model ----
        val presets = mapOf(
            R.id.btn_preset_gemini to Preset(
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                "gemini-2.0-flash"
            ),
            R.id.btn_preset_openrouter to Preset(
                "https://openrouter.ai/api/v1/chat/completions",
                "meta-llama/llama-3.3-70b-instruct:free"
            ),
            R.id.btn_preset_groq to Preset(
                "https://api.groq.com/openai/v1/chat/completions",
                "meta-llama/llama-4-scout-17b-16e-instruct"
            ),
            R.id.btn_preset_openai to Preset(
                "https://api.openai.com/v1/chat/completions",
                "gpt-4o-mini"
            ),
            R.id.btn_preset_mistral to Preset(
                "https://api.mistral.ai/v1/chat/completions",
                "mistral-small-latest"
            ),
            R.id.btn_preset_local to Preset(
                "http://192.168.1.100:1234/v1/chat/completions",
                "local-model"
            )
        )
        presets.forEach { (id, p) ->
            view.findViewById<Button>(id).setOnClickListener {
                base.setText(p.base)
                model.setText(p.model)
                val msg = if (id == R.id.btn_preset_local) R.string.preset_filled_local else R.string.preset_filled
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }

        // ---- Fetch available models from any provider ----
        view.findViewById<Button>(R.id.btn_models).setOnClickListener {
            val ctx = requireContext()
            PrefStore.setApiBase(ctx, base.text.toString())
            PrefStore.setApiKey(ctx, key.text.toString())
            status.text = getString(R.string.fetching_models)
            Thread {
                var error: String? = null
                val list: List<String> = try {
                    runBlocking { AIClient.models(ctx) }
                } catch (e: Exception) {
                    error = e.message ?: e.javaClass.simpleName
                    emptyList()
                }
                if (!isAdded) return@Thread
                requireActivity().runOnUiThread {
                    if (list.isEmpty()) {
                        status.text = getString(R.string.no_models) + (error?.let { "\n$it" } ?: "")
                    } else {
                        status.text = getString(R.string.found_models, list.size)
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.models_dialog_title)
                            .setItems(list.toTypedArray()) { _, which -> model.setText(list[which]) }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
            }.start()
        }

        // ---- Test connection button ----
        testBtn.setOnClickListener {
            val ctx = requireContext()
            // save first so the test uses exactly what is on screen
            PrefStore.setApiBase(ctx, base.text.toString())
            PrefStore.setApiKey(ctx, key.text.toString())
            PrefStore.setModel(ctx, model.text.toString())

            testBtn.isEnabled = false
            status.text = getString(R.string.testing)
            Thread {
                val result: String = try {
                    if (PrefStore.getApiKeyEff(ctx).isBlank()) {
                        getString(R.string.test_fail) + " " + getString(R.string.test_no_key)
                    } else {
                        runBlocking { AIClient.chat(ctx, "ping") }
                        getString(R.string.test_ok)
                    }
                } catch (e: Exception) {
                    getString(R.string.test_fail) + "\n" + (e.message ?: e.javaClass.simpleName)
                }
                if (!isAdded) return@Thread
                requireActivity().runOnUiThread {
                    testBtn.isEnabled = true
                    status.text = result
                }
            }.start()
        }
    }
}
