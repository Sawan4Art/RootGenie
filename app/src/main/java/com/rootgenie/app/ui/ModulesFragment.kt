package com.rootgenie.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rootgenie.app.AgentEngine
import com.rootgenie.app.AgentResult
import com.rootgenie.app.ModuleStore
import com.rootgenie.app.R
import com.rootgenie.app.RootShell
import com.rootgenie.app.modules.ModuleFactory
import com.rootgenie.app.modules.ModuleTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModulesFragment : Fragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var listAdapter: ModuleListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_modules, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val desc = view.findViewById<EditText>(R.id.module_desc)
        val recycler = view.findViewById<RecyclerView>(R.id.module_list)
        listAdapter = ModuleListAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = listAdapter

        // Quick template buttons
        val row = view.findViewById<LinearLayout>(R.id.templates_row)
        for ((label, builder) in ModuleTemplates.all) {
            val b = Button(requireContext())
            b.text = label
            b.setOnClickListener {
                scope.launch {
                    val f = withContext(Dispatchers.IO) {
                        ModuleFactory.build(requireContext().applicationContext, builder())
                    }
                    ModuleStore.add(f)
                    listAdapter.notifyDataSetChanged()
                }
            }
            row.addView(b)
        }

        view.findViewById<Button>(R.id.btn_generate).setOnClickListener {
            val t = desc.text.toString().trim()
            if (t.isEmpty()) return@setOnClickListener
            desc.setText("")
            generate(t)
        }
    }

    private fun generate(description: String) {
        val appCtx = requireContext().applicationContext
        scope.launch {
            val result = AgentEngine(appCtx).process(description, forceModule = true)
            when (result) {
                is AgentResult.ModulePlan -> {
                    val f = withContext(Dispatchers.IO) {
                        ModuleFactory.build(appCtx, result.spec)
                    }
                    ModuleStore.add(f)
                    listAdapter.notifyDataSetChanged()
                }
                is AgentResult.Chat -> toast(result.text)
                is AgentResult.ShellPlan -> toast(result.reply)
            }
        }
    }

    private fun toast(t: String) {
        if (isAdded()) android.widget.Toast.makeText(requireContext(), t, android.widget.Toast.LENGTH_LONG).show()
    }

    // ---- list of generated modules ----
    inner class ModuleListAdapter : RecyclerView.Adapter<ModuleListAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.module_name)
            val install: Button = v.findViewById(R.id.btn_install)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_module, parent, false))

        override fun getItemCount(): Int = ModuleStore.files.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val f = ModuleStore.files[position]
            holder.name.text = f.name
            holder.install.setOnClickListener {
                if (!isAdded()) return@setOnClickListener
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.install_title)
                    .setMessage(getString(R.string.install_q, f.name))
                    .setPositiveButton(R.string.run) { _, _ ->
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                RootShell.installModule(f.absolutePath)
                            }
                            toast(r.outputText())
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
    }
}
