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
import androidx.fragment.app.Fragment
import com.rootgenie.app.PrefStore
import com.rootgenie.app.R
import com.rootgenie.app.ai.AIClient
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

        view.findViewById<Button>(R.id.btn_save).setOnClickListener {
            PrefStore.setApiBase(requireContext(), base.text.toString())
            PrefStore.setApiKey(requireContext(), key.text.toString())
            PrefStore.setModel(requireContext(), model.text.toString())
            PrefStore.setStrict(requireContext(), strict.isChecked)
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
                "llama-3.3-70b-versatile"
            ),
            R.id.btn_preset_openai to Preset(
                "https://api.openai.com/v1/chat/completions",
                "gpt-4o-mini"
            )
        )
        presets.forEach { (id, p) ->
            view.findViewById<Button>(id).setOnClickListener {
                base.setText(p.base)
                model.setText(p.model)
                Toast.makeText(requireContext(), R.string.preset_filled, Toast.LENGTH_LONG).show()
            }
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
                    if (PrefStore.getApiKey(ctx).isBlank()) {
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
