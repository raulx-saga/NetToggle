package com.example.nettoggle

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tiny notebook the service writes into, so the app can show what happened.
object Dbg {
    private const val FILE = "dbg"
    private const val KEY = "log"

    fun log(ctx: Context, msg: String) {
        val prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val old = prefs.getString(KEY, "") ?: ""
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val updated = ("$time  $msg\n\n" + old).take(4000)   // newest on top, capped size
        prefs.edit().putString(KEY, updated).apply()
    }

    fun read(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
