package com.primesoft.reelcounter

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import kotlin.math.abs

/**
 * Accessibility service: Facebook / TikTok / Instagram / YouTube mein vertical scroll
 * (yaani agli reel) dekh kar ginti barhata hai, aur screen par chota "▶ 23" bubble dikhata hai.
 * Sirf scroll ka event dekhta hai — kuch record, save ya upload nahi karta.
 */
class ReelService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var bubble: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var shown = false
    private var inReelApp = false

    private var lastCountAt = 0L
    private var lastCheckAt = 0L
    private var lastCheckPkg = ""
    private var lastCheckOk = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createBubble()
        updateBubble()
        if (Store.alwaysShow(this)) show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleForeground(pkg)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(pkg, e)
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        hide()
        instance = null
        return super.onUnbind(intent)
    }

    // ---------- logic ----------

    private fun handleForeground(pkg: String) {
        // Notification shade / keyboard foreground nahi maane jate
        if (pkg == "com.android.systemui" || pkg == "android" || pkg == packageName ||
            pkg.contains("inputmethod") || pkg.contains("keyboard") || pkg.contains("honeyboard")
        ) return
        inReelApp = Store.appFor(pkg) != null
        updateBubble()
        if (inReelApp || Store.alwaysShow(this)) show() else hide()
    }

    /** Comments panel ke andar ka scroll reel nahi hota — usay ignore karo. */
    private fun inComments(node: AccessibilityNodeInfo?): Boolean {
        var cur = node
        var depth = 0
        while (cur != null && depth < 10) {
            val id = cur.viewIdResourceName
            if (id != null && id.contains("comment", ignoreCase = true)) return true
            cur = cur.parent
            depth++
        }
        return false
    }

    private fun handleScroll(pkg: String, e: AccessibilityEvent) {
        val app = Store.appFor(pkg) ?: return
        // Sirf upar/neeche (vertical) swipe: story tray / carousel jaisa side scroll na gino
        if (Build.VERSION.SDK_INT >= 28 && e.scrollDeltaY == 0 && e.scrollDeltaX != 0) return
        if (inComments(e.source)) return
        val now = SystemClock.elapsedRealtime()
        // Ek swipe se kai scroll events aate hain — 0.8 sec mein ek hi reel gino
        if (now - lastCountAt < MIN_GAP_MS) return

        val ok = if (pkg == lastCheckPkg && now - lastCheckAt < 1200) {
            lastCheckOk
        } else {
            val r = isReelScreen(app)
            lastCheckPkg = pkg
            lastCheckAt = now
            lastCheckOk = r
            r
        }
        if (!ok) return

        lastCountAt = now
        inReelApp = true
        Store.increment(this, app)
        show()
        updateBubble()
    }

    private fun isReelScreen(app: ReelApp): Boolean {
        if (Store.loose(this) || app.hints.isEmpty()) return true
        val root = rootInActiveWindow ?: return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < 700) {
            val n = queue.removeFirst()
            seen++
            val id = n.viewIdResourceName
            if (id != null && app.hints.any { id.contains(it, ignoreCase = true) }) return true
            if (app.checkDescription) {
                val cd = n.contentDescription?.toString()
                if (cd != null && app.hints.any { cd.contains(it, ignoreCase = true) }) return true
            }
            for (i in 0 until n.childCount) {
                val c = n.getChild(i)
                if (c != null) queue.add(c)
            }
        }
        return false
    }

    // ---------- bubble ----------

    private fun createBubble() {
        val d = resources.displayMetrics.density
        val tv = TextView(this)
        tv.text = "▶ 0"
        tv.setTextColor(Color.WHITE)
        tv.textSize = 15f
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.cornerRadius = 100f * d
        bg.setColor(0xE66C5CE7.toInt())
        tv.background = bg
        tv.setPadding((12 * d).toInt(), (6 * d).toInt(), (12 * d).toInt(), (6 * d).toInt())

        val saved = Store.bubblePos(this)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = saved?.first ?: (resources.displayMetrics.widthPixels - (110 * d).toInt())
        lp.y = saved?.second ?: (160 * d).toInt()
        params = lp

        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        tv.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    downX = ev.rawX
                    downY = ev.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (abs(dx) > 10f || abs(dy) > 10f) moved = true
                    if (moved) {
                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()
                        if (shown) {
                            try {
                                wm?.updateViewLayout(tv, lp)
                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) Store.setBubblePos(this, lp.x, lp.y) else openApp()
                }
            }
            true
        }
        bubble = tv
    }

    private fun openApp() {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun show() {
        val v = bubble ?: return
        val p = params ?: return
        if (shown) return
        try {
            wm?.addView(v, p)
            shown = true
        } catch (ex: Exception) {
        }
    }

    private fun hide() {
        val v = bubble ?: return
        if (!shown) return
        try {
            wm?.removeView(v)
        } catch (ex: Exception) {
        }
        shown = false
    }

    fun updateBubble() {
        bubble?.text = "▶ " + Store.todayTotal(this)
    }

    fun applyMode() {
        if (Store.alwaysShow(this)) show() else if (!inReelApp) hide()
    }

    companion object {
        private const val MIN_GAP_MS = 800L
        var instance: ReelService? = null

        fun refresh() {
            instance?.applyMode()
            instance?.updateBubble()
        }
    }
}
