package com.primesoft.reelcounter

import android.content.Context
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Supported apps. `hints` = view-id ke tukde jo us app ki Reels screen par milte hain.
 * Agar kisi app ka update ho jaye aur count rukk jaye to yahan hints badal sakte hain
 * (ya app mein "Loose mode" ON kar den).
 */
enum class ReelApp(
    val label: String,
    val packages: List<String>,
    val hints: List<String>,
    val checkDescription: Boolean,
    val color: Int
) {
    TIKTOK(
        "TikTok",
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"),
        emptyList(), false, 0xFF111111.toInt()
    ),
    INSTAGRAM(
        "Instagram",
        listOf("com.instagram.android"),
        listOf("clips_viewer", "clips_swipe", "reel_viewer", "clips_video", "clips_pager"),
        false, 0xFFE1306C.toInt()
    ),
    YOUTUBE(
        "YouTube Shorts",
        listOf("com.google.android.youtube"),
        listOf("reel_player", "reel_recycler", "reel_watch"),
        false, 0xFFFF0000.toInt()
    ),
    FACEBOOK(
        "Facebook",
        listOf("com.facebook.katana", "com.facebook.lite"),
        listOf("reel"),
        true, 0xFF1877F2.toInt()
    )
}

object Store {
    private const val COUNTS = "reel_counts"
    private const val SETTINGS = "reel_settings"

    private fun counts(c: Context) = c.getSharedPreferences(COUNTS, Context.MODE_PRIVATE)
    private fun settings(c: Context) = c.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)

    fun appFor(pkg: String): ReelApp? = ReelApp.values().firstOrNull { pkg in it.packages }

    fun dayKey(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    // ---------- counting ----------

    fun increment(c: Context, app: ReelApp) {
        val p = counts(c)
        val k = "d:${dayKey(Calendar.getInstance())}:${app.name}"
        p.edit().putInt(k, p.getInt(k, 0) + 1).apply()
    }

    fun todayTotal(c: Context): Int {
        val day = dayKey(Calendar.getInstance())
        val p = counts(c)
        var sum = 0
        for (a in ReelApp.values()) sum += p.getInt("d:$day:${a.name}", 0)
        return sum
    }

    fun resetAll(c: Context) {
        counts(c).edit().clear().apply()
    }

    /** date -> counts per app (index = ReelApp.ordinal) */
    fun loadAll(c: Context): Map<String, IntArray> {
        val out = HashMap<String, IntArray>()
        for ((k, v) in counts(c).all) {
            if (!k.startsWith("d:") || v !is Int) continue
            val parts = k.split(":")
            if (parts.size != 3) continue
            val app = ReelApp.values().firstOrNull { it.name == parts[2] } ?: continue
            val arr = out.getOrPut(parts[1]) { IntArray(ReelApp.values().size) }
            arr[app.ordinal] += v
        }
        return out
    }

    // ---------- stats ----------

    private fun dayTotal(all: Map<String, IntArray>, cal: Calendar): Int =
        all[dayKey(cal)]?.sum() ?: 0

    fun today(all: Map<String, IntArray>): IntArray =
        all[dayKey(Calendar.getInstance())] ?: IntArray(ReelApp.values().size)

    fun rangeSum(all: Map<String, IntArray>, daysBack: Int): Int {
        var sum = 0
        for (i in 0 until daysBack) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            sum += dayTotal(all, cal)
        }
        return sum
    }

    fun monthToDate(all: Map<String, IntArray>): Int {
        val prefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        var sum = 0
        for ((k, v) in all) if (k.startsWith(prefix)) sum += v.sum()
        return sum
    }

    fun allTime(all: Map<String, IntArray>): Int {
        var sum = 0
        for (v in all.values) sum += v.sum()
        return sum
    }

    fun bestDay(all: Map<String, IntArray>): Int {
        var best = 0
        for (v in all.values) if (v.sum() > best) best = v.sum()
        return best
    }

    fun dailySeries(all: Map<String, IntArray>): List<Pair<String, Int>> {
        val f = SimpleDateFormat("EEE", Locale.getDefault())
        val out = ArrayList<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            out.add(Pair(f.format(cal.time), dayTotal(all, cal)))
        }
        return out
    }

    fun weeklySeries(all: Map<String, IntArray>): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        for (w in 7 downTo 0) {
            var sum = 0
            var label = ""
            for (d in 0 until 7) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -(w * 7 + d))
                sum += dayTotal(all, cal)
                if (d == 6) label = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
            }
            out.add(Pair(label, sum))
        }
        return out
    }

    fun monthlySeries(all: Map<String, IntArray>): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val nameFmt = SimpleDateFormat("MMM", Locale.getDefault())
        for (m in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -m)
            val prefix = keyFmt.format(cal.time)
            var sum = 0
            for ((k, v) in all) if (k.startsWith(prefix)) sum += v.sum()
            out.add(Pair(nameFmt.format(cal.time), sum))
        }
        return out
    }

    // ---------- settings ----------

    fun alwaysShow(c: Context): Boolean = settings(c).getBoolean("always_show", false)
    fun setAlwaysShow(c: Context, v: Boolean) = settings(c).edit().putBoolean("always_show", v).apply()

    fun loose(c: Context): Boolean = settings(c).getBoolean("loose", false)
    fun setLoose(c: Context, v: Boolean) = settings(c).edit().putBoolean("loose", v).apply()

    fun bubblePos(c: Context): Pair<Int, Int>? {
        val s = settings(c)
        return if (s.contains("bx") && s.contains("by")) Pair(s.getInt("bx", 0), s.getInt("by", 0)) else null
    }

    fun setBubblePos(c: Context, x: Int, y: Int) {
        settings(c).edit().putInt("bx", x).putInt("by", y).apply()
    }

    fun isServiceEnabled(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return s.split(':').any { it.startsWith(c.packageName + "/") }
    }
}
