package com.rootgenie.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rootgenie.app.ui.ChatFragment
import com.rootgenie.app.ui.ModulesFragment
import com.rootgenie.app.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RootShell.init()
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        if (savedInstanceState == null) {
            show(ChatFragment())
        }
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> show(ChatFragment())
                R.id.nav_modules -> show(ModulesFragment())
                R.id.nav_settings -> show(SettingsFragment())
            }
            true
        }
    }

    private fun show(f: Fragment) =
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
}
