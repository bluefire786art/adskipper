package com.adskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipService : AccessibilityService() {

    companion object {
        // TikTok ke package names (global + Asia version)
        val PACKAGES = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

        // Ad pehchanne ke liye texts (lowercase, poora match). Zaroorat ho to yahan add karein.
        val AD_MARKERS = setOf("ad", "sponsored", "advertisement", "promoted")

        // Debug mode mein screen ke texts yahan jama hote hain
        val seen = LinkedHashSet<String>()
        var swipes = 0
    }

    private var lastScan = 0L
    private var lastSwipe = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg !in PACKAGES) return

        val now = SystemClock.uptimeMillis()
        if (now - lastScan < 700) return
        lastScan = now

        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        val enabled = prefs.getBoolean("on", true)
        val debug = prefs.getBoolean("debug", false)
        if (!enabled && !debug) return

        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collect(root, texts, debug)

        if (enabled && now - lastSwipe > 1500 && isAd(texts)) {
            lastSwipe = now
            swipes++
            // Agar timer chal raha hai to swipe app ignore kar degi; timer khatam hote hi
            // agli koshish (1.5 sec baad) kamyab ho jayegi.
            swipeUp()
        }
    }

    private fun isAd(texts: List<String>): Boolean =
        texts.any { it.trim().lowercase() in AD_MARKERS }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>, debug: Boolean) {
        if (node == null) return
        val t = node.text?.toString()
        val d = node.contentDescription?.toString()
        if (!t.isNullOrBlank()) out.add(t)
        if (!d.isNullOrBlank()) out.add(d)
        if (debug && (!t.isNullOrBlank() || !d.isNullOrBlank())) {
            val line = "id=${node.viewIdResourceName} | text=$t | desc=$d | ${node.className}"
            synchronized(seen) {
                if (seen.size < 500) seen.add(line)
            }
        }
        for (i in 0 until node.childCount) {
            collect(node.getChild(i), out, debug)
        }
    }

    private fun swipeUp() {
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        val path = Path().apply {
            moveTo(x, m.heightPixels * 0.75f)
            lineTo(x, m.heightPixels * 0.25f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}
}
