package com.rootgenie.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rootgenie.app.PrefStore
import com.rootgenie.app.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val base = view.findViewById<EditText>(R.id.ed_api_base)
        val key = view.findViewById<EditText>(R.id.ed_api_key)
        val model = view.findViewById<EditText>(R.id.ed_model)
        val strict = view.findViewById<Switch>(R.id.sw_strict)

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
    }
}
