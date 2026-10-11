package com.example.nettoggle

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

// The "robot finger". Once armed by the tile, it looks at the screen every 0.4s,
// finds the right rows by their text, taps them, then presses Back until Settings closes.
class ToggleAccessibilityService : AccessibilityService() {

    companion object {
        // The tile uses this to reach the running service (null = service not enabled).
        @Volatile
        var instance: ToggleAccessibilityService? = null
        private const val TIMEOUT_MS = 15_000L
        private const val TICK_MS = 400L
    }

    // Where we are in the job.
    private enum class Step { IDLE, OPEN_MODE, PICK, CLOSING }

    private var step = Step.IDLE
    private var armedAt = 0L
    private var workPkg: String? = null          // the app we're driving (Settings)
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}   // we poll instead
    override fun onInterrupt() {}

    // ---- Called by the tile: "start the job" ----
    fun arm() {
        handler.removeCallbacksAndMessages(null)
        step = Step.OPEN_MODE
        armedAt = System.currentTimeMillis()
        workPkg = null
        tick()
    }

    // ---- The loop: look at the screen, do the current step, repeat ----
    private fun tick() {
        handler.postDelayed({
            if (step != Step.OPEN_MODE && step != Step.PICK) return@postDelayed

            if (System.currentTimeMillis() - armedAt > TIMEOUT_MS) {
                Dbg.log(this, "TIMED OUT in step $step. Screen texts: ${dump(rootInActiveWindow)}")
                Toast.makeText(this, "NetToggle: gave up (see log in the app)", Toast.LENGTH_LONG).show()
                step = Step.IDLE
                return@postDelayed
            }

            val root = rootInActiveWindow
            if (root != null) {
                if (step == Step.OPEN_MODE) openMode(root) else pick(root)
            }
            tick()
        }, TICK_MS)
    }

    // Step 1: find the "Network mode" row and tap it.
    private fun openMode(root: AccessibilityNodeInfo) {
        val node = textNodes(root).firstOrNull {
            it.text.toString().contains("network mode", ignoreCase = true)
        } ?: return
        if (click(node)) {
            workPkg = root.packageName?.toString()
            Dbg.log(this, "Tapped 'Network mode' in app: $workPkg")
            step = Step.PICK
        }
    }

    // Step 2: find the 5G option and the 4G option, see which is ticked, tap the other.
    private fun pick(root: AccessibilityNodeInfo) {
        val nodes = textNodes(root)

        // A "row" = the tappable line containing text that matches, and that has a radio/check box.
        fun rowFor(match: (String) -> Boolean): AccessibilityNodeInfo? {
            for (n in nodes) {
                if (!match(n.text.toString())) continue
                val row = rowOf(n) ?: continue
                if (anyInTree(row) { it.isCheckable }) return row
            }
            return null
        }

        val fiveRow = rowFor { it.contains("5G", ignoreCase = true) }
        val fourRow = rowFor {
            !it.contains("5G", ignoreCase = true) &&
                (it.contains("LTE", ignoreCase = true) || it.contains("4G", ignoreCase = true))
        }
        if (fiveRow == null || fourRow == null) return   // screen not ready yet, try again next tick

        val fiveOn = anyInTree(fiveRow) { it.isChecked }
        val fourOn = anyInTree(fourRow) { it.isChecked }

        val target: AccessibilityNodeInfo
        val goingTo: String
        if (fiveOn && !fourOn) {
            target = fourRow; goingTo = "4G"
        } else if (fourOn && !fiveOn) {
            target = fiveRow; goingTo = "5G"
        } else {
            Dbg.log(this, "Could not tell which option is selected. Screen texts: ${dump(root)}")
            step = Step.IDLE
            return
        }

        target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Dbg.log(this, "Switched to $goingTo")
        Toast.makeText(this, "Switching to $goingTo", Toast.LENGTH_SHORT).show()
        step = Step.CLOSING
        closeSettings(0)
    }

    // Step 3: press Back until we've left Settings (max 6 times).
    private fun closeSettings(attempt: Int) {
        handler.postDelayed({
            val stillThere = workPkg != null && rootInActiveWindow?.packageName?.toString() == workPkg
            if (stillThere && attempt < 6) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                closeSettings(attempt + 1)
            } else {
                step = Step.IDLE
            }
        }, 800)
    }

    // ---- Helpers ----

    // Tap a node, or the nearest tappable parent if the node itself isn't tappable.
    private fun click(node: AccessibilityNodeInfo): Boolean {
        val row = rowOf(node) ?: return false
        return row.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // Walk up (max 5 levels) to the first tappable parent.
    private fun rowOf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        var depth = 0
        while (n != null && depth < 5) {
            if (n.isClickable) return n
            n = n.parent
            depth++
        }
        return null
    }

    // All nodes on screen that carry some text.
    private fun textNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            if (!n.text.isNullOrBlank()) out.add(n)
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return out
    }

    // Does this node or anything under it satisfy the test?
    private fun anyInTree(n: AccessibilityNodeInfo?, test: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (n == null) return false
        if (test(n)) return true
        for (i in 0 until n.childCount) {
            if (anyInTree(n.getChild(i), test)) return true
        }
        return false
    }

    // Debug: every text on screen, with [x] / [ ] for checkboxes.
    private fun dump(root: AccessibilityNodeInfo?): String {
        if (root == null) return "(no screen)"
        val parts = mutableListOf<String>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val t = n.text?.toString()
            if (!t.isNullOrBlank()) {
                val box = if (n.isCheckable) (if (n.isChecked) "[x]" else "[ ]") else ""
                parts.add(t + box)
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return "pkg=${root.packageName} :: " + parts.joinToString(" | ").take(700)
    }
}
