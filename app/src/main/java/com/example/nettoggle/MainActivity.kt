package com.example.nettoggle

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

// An Activity = one screen of an app. This one just shows instructions.
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.text = "NetToggle installed.\n\nSwipe down your notification panel, tap the pencil/edit icon, and drag the \"5G / 4G\" tile into your active tiles."
        text.textSize = 18f
        text.setPadding(48, 96, 48, 48)
        setContentView(text)
    }
}
