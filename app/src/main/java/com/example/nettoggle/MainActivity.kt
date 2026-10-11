package com.example.nettoggle

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(48, 96, 48, 48)

        val info = TextView(this)
        info.textSize = 15f
        info.text = "1. Turn on NetToggle under Accessibility (button below).\n" +
            "2. Add the \"5G / 4G\" tile to your quick panel.\n" +
            "3. Tap the tile.\n\nLog (newest first):"

        val openA11y = Button(this)
        openA11y.text = "Open Accessibility settings"
        openA11y.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        val clear = Button(this)
        clear.text = "Clear log"
        clear.setOnClickListener { Dbg.clear(this); showLog() }

        logView = TextView(this)
        logView.textSize = 12f
        logView.setTextIsSelectable(true)

        val scroll = ScrollView(this)
        scroll.addView(logView)

        page.addView(info)
        page.addView(openA11y)
        page.addView(clear)
        page.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(page)
    }

    override fun onResume() {
        super.onResume()
        showLog()
    }

    private fun showLog() {
        val text = Dbg.read(this)
        logView.text = if (text.isBlank()) "(empty)" else text
    }
}
