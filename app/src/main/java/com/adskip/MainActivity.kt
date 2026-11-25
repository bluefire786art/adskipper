package com.adskip

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var dumpView: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        val swOn = Switch(this).apply {
            text = "Auto skip ON"
            isChecked = prefs.getBoolean("on", true)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("on", c).apply() }
        }
        root.addView(swOn)

        val swDebug = Switch(this).apply {
            text = "Debug mode (screen ke texts record karo)"
            isChecked = prefs.getBoolean("debug", false)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("debug", c).apply() }
        }
        root.addView(swDebug)

        root.addView(Button(this).apply {
            text = "Accessibility settings kholo"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "Debug list refresh"
            setOnClickListener { refresh() }
        })
        root.addView(Button(this).apply {
            text = "Debug list copy"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("dump", dumpView.text))
                Toast.makeText(this@MainActivity, "Copy ho gaya", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "Debug list saaf karo"
            setOnClickListener {
                synchronized(SkipService.seen) { SkipService.seen.clear() }
                refresh()
            }
        })

        dumpView = TextView(this).apply { textSize = 11f; setTextIsSelectable(true) }
        val scroll = ScrollView(this)
        scroll.addView(dumpView)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val active = enabledServices.contains(packageName)
        status.text = (if (active) "Service chal rahi hai ✅" else "Service band hai ❌ (Accessibility mein 'Ad Skipper' ON karein)") +
                "\nAb tak swipes: ${SkipService.swipes}"
        synchronized(SkipService.seen) {
            dumpView.text = SkipService.seen.joinToString("\n")
        }
    }
}
