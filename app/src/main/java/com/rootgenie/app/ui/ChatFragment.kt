package com.rootgenie.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rootgenie.app.AgentEngine
import com.rootgenie.app.AgentResult
import com.rootgenie.app.Command
import com.rootgenie.app.ModuleSpec
import com.rootgenie.app.ModuleStore
import com.rootgenie.app.PrefStore
import com.rootgenie.app.R
import com.rootgenie.app.RootShell
import com.rootgenie.app.SafetyChecker
import com.rootgenie.app.modules.ModuleFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatFragment : Fragment() {

    data class Msg(val text: String, val isUser: Boolean, val isCode: Boolean = false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val msgs = mutableListOf<Msg>()
    private lateinit var adapter: ChatAdapter
    private lateinit var list: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list = view.findViewById(R.id.chat_list)
        adapter = ChatAdapter(msgs)
        list.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        list.adapter = adapter

        val status = view.findViewById<TextView>(R.id.root_status)
        scope.launch {
            val root = withContext(Dispatchers.IO) { RootShell.isRoot() }
            val mv = withContext(Dispatchers.IO) { RootShell.magiskVersion() }
            status.text = if (root)
                getString(R.string.root_status_ok) + "  •  " + mv
            else
                getString(R.string.root_status_no)
        }

        if (msgs.isEmpty()) add(Msg(getString(R.string.welcome_msg), false))

        val input = view.findViewById<EditText>(R.id.input)
        view.findViewById<Button>(R.id.btn_send).setOnClickListener {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                input.setText("")
                onUser(t)
            }
        }
    }

    private fun add(m: Msg) {
        msgs.add(m)
        adapter.notifyItemInserted(msgs.size - 1)
        list.scrollToPosition(msgs.size - 1)
    }

    private fun removeThinking() {
        val idx = msgs.indexOfLast { !it.isUser && !it.isCode && it.text == getString(R.string.thinking) }
        if (idx >= 0) {
            msgs.removeAt(idx)
            adapter.notifyItemRemoved(idx)
        }
    }

    private fun onUser(text: String) {
        add(Msg(text, true))
        add(Msg(getString(R.string.thinking), false))
        val appCtx = requireContext().applicationContext
        scope.launch {
            val result = AgentEngine(appCtx).process(text)
            removeThinking()
            handle(result)
        }
    }

    private suspend fun handle(result: AgentResult) {
        when (result) {
            is AgentResult.Chat -> add(Msg(result.text.ifBlank { "…" }, false))
            is AgentResult.ShellPlan -> {
                if (result.reply.isNotBlank()) add(Msg(result.reply, false))
                for (c in result.commands) runCommand(c)
            }
            is AgentResult.ModulePlan -> {
                if (result.reply.isNotBlank()) add(Msg(result.reply, false))
                buildAndOfferModule(result.spec)
            }
        }
    }

    private fun runCommand(c: Command) {
        if (!isAdded()) return
        val verdict = SafetyChecker.check(c.cmd)
        val strict = PrefStore.isStrict(requireContext())
        val autoOk = !strict && c.risk.equals("safe", true)
        when (verdict.risk) {
            SafetyChecker.Risk.SAFE -> doRun(c.cmd)
            SafetyChecker.Risk.CONFIRM ->
                if (autoOk) doRun(c.cmd)
                else confirmDialog(
                    getString(R.string.confirm_title),
                    c.cmd + "\n\n(" + verdict.reason + ")",
                    onYes = { doRun(c.cmd) }
                )
            SafetyChecker.Risk.BLOCKED ->
                add(Msg(getString(R.string.blocked_cmd) + "\n" + c.cmd, false, true))
        }
    }

    private fun doRun(cmd: String) {
        val appCtx = requireContext().applicationContext
        scope.launch {
            val r = withContext(Dispatchers.IO) { RootShell.exec(cmd) }
            add(Msg("$ $cmd\n${r.outputText()}", false, true))
        }
    }

    private fun buildAndOfferModule(spec: ModuleSpec) {
        if (!isAdded()) return
        val appCtx = requireContext().applicationContext
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { ModuleFactory.build(appCtx, spec) }
                ModuleStore.add(file)
                add(Msg("📦 ${spec.name} ${spec.version}\n${spec.description}\n✔ ${file.name}", false))
                confirmDialog(
                    getString(R.string.install_title),
                    getString(R.string.install_q, spec.name),
                    onYes = {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                RootShell.installModule(file.absolutePath)
                            }
                            add(Msg(r.outputText(), false, true))
                            add(Msg(getString(R.string.reboot_hint), false))
                        }
                    }
                )
            } catch (e: Exception) {
                add(Msg(getString(R.string.build_fail) + "\n" + e.message, false, true))
            }
        }
    }

    private fun confirmDialog(title: String, message: String, onYes: () -> Unit) {
        if (!isAdded()) return
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.run) { _, _ -> onYes() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
    }
}
