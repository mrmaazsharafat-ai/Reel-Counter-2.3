package com.primesoft.reelcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1500)
        }
    }

    private val purple = 0xFF6C5CE7.toInt()
    private val gray = 0xFF777777.toInt()

    private lateinit var statusText: TextView
    private lateinit var todayBig: TextView
    private val appValues = ArrayList<TextView>()
    private lateinit var weekVal: TextView
    private lateinit var monthVal: TextView
    private lateinit var totalVal: TextView
    private lateinit var chart: BarChartView
    private lateinit var chartTitle: TextView
    private lateinit var extraText: TextView
    private val tabButtons = ArrayList<Button>()
    private var chartMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ---------- UI helpers ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(text: String, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun makeCard(parent: LinearLayout): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(Color.WHITE)
        bg.cornerRadius = dp(16).toFloat()
        c.background = bg
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(12)
        parent.addView(c, lp)
        return c
    }

    private fun makeButton(text: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        return b
    }

    private fun statBox(parent: LinearLayout, title: String): TextView {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.setColor(Color.WHITE)
        bg.cornerRadius = dp(14).toFloat()
        box.background = bg
        box.setPadding(dp(6), dp(12), dp(6), dp(12))
        val v = label("0", 22f, purple, true)
        v.gravity = Gravity.CENTER
        val t = label(title, 11.5f, gray)
        t.gravity = Gravity.CENTER
        box.addView(v)
        box.addView(t)
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(dp(4), 0, dp(4), 0)
        parent.addView(box, lp)
        return v
    }

    // ---------- build ----------

    private fun buildUi() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(0xFFF3F4F8.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(28), dp(16), dp(24))
        scroll.addView(content)
        setContentView(scroll)

        content.addView(label("Reel Counter", 26f, purple, true))
        val sub = label("Facebook • TikTok • Instagram • YouTube Shorts", 13f, gray)
        content.addView(sub)
        val gap = View(this)
        content.addView(gap, LinearLayout.LayoutParams(1, dp(14)))

        // --- setup ---
        val setup = makeCard(content)
        statusText = label("", 15f, Color.BLACK, true)
        setup.addView(statusText)
        setup.addView(
            label(
                "Pehli dafa: neeche button dabayen, list mein 'Reel Counter' dhoondein aur ON karein. " +
                    "Agar ON na ho (Android 13+): 'App info kholein' → ⋮ (teen nuqte) → 'Restricted settings allow karein', phir dobara try karein.",
                12.5f, 0xFF666666.toInt()
            )
        )
        setup.addView(makeButton("Accessibility kholein") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setup.addView(makeButton("App info kholein") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
            )
        })

        // --- today ---
        val today = makeCard(content)
        today.addView(label("Aaj ki Reels", 14f, gray))
        todayBig = label("0", 52f, purple, true)
        today.addView(todayBig)
        for (app in ReelApp.values()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(4), 0, dp(4))
            val name = label("●  " + app.label, 15f, app.color)
            val v = label("0", 15f, Color.BLACK, true)
            row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            today.addView(row)
            appValues.add(v)
        }

        // --- stat boxes ---
        val statRow = LinearLayout(this)
        statRow.orientation = LinearLayout.HORIZONTAL
        weekVal = statBox(statRow, "Hafta (7 din)")
        monthVal = statBox(statRow, "Is mahine")
        totalVal = statBox(statRow, "Total")
        val srLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        srLp.bottomMargin = dp(12)
        content.addView(statRow, srLp)

        // --- chart ---
        val cc = makeCard(content)
        chartTitle = label("", 14f, Color.BLACK, true)
        cc.addView(chartTitle)
        val tabs = LinearLayout(this)
        tabs.orientation = LinearLayout.HORIZONTAL
        val names = listOf("Din wise", "Hafta wise", "Mahina wise")
        for ((i, n) in names.withIndex()) {
            val b = makeButton(n) {
                chartMode = i
                refresh()
            }
            b.textSize = 12f
            tabs.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            tabButtons.add(b)
        }
        cc.addView(tabs)
        chart = BarChartView(this)
        cc.addView(chart, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)))
        extraText = label("", 12.5f, 0xFF666666.toInt())
        cc.addView(extraText)

        // --- settings ---
        val sc = makeCard(content)
        val sw1 = Switch(this)
        sw1.text = "Bubble hamesha dikhao"
        sw1.textSize = 14f
        sw1.isChecked = Store.alwaysShow(this)
        sw1.setOnCheckedChangeListener { _, on ->
            Store.setAlwaysShow(this, on)
            ReelService.refresh()
        }
        sc.addView(sw1)
        sc.addView(label("OFF ho to bubble sirf in 4 apps ke andar nazar aata hai. Bubble ko ungli se kahin bhi khench kar rakh sakte hain; us par tap karein to ye app khul jati hai.", 11.5f, gray))
        val sw2 = Switch(this)
        sw2.text = "Loose mode"
        sw2.textSize = 14f
        sw2.isChecked = Store.loose(this)
        sw2.setOnCheckedChangeListener { _, on -> Store.setLoose(this, on) }
        val sw2Lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sw2Lp.topMargin = dp(14)
        sc.addView(sw2, sw2Lp)
        sc.addView(label("Agar kisi app mein reels count nahi ho rahin to ise ON karein: phir us app ka har vertical scroll count hoga (feed scroll bhi gin liya ja sakta hai).", 11.5f, gray))

        // --- reset ---
        content.addView(makeButton("Saara record reset karein") {
            AlertDialog.Builder(this)
                .setTitle("Reset?")
                .setMessage("Saari ginti hamesha ke liye mit jaye gi.")
                .setPositiveButton("Haan, mita do") { _, _ ->
                    Store.resetAll(this)
                    ReelService.refresh()
                    refresh()
                }
                .setNegativeButton("Nahi", null)
                .show()
        })

        val footer = label("Powered by Maaz Sharafat  •  PrimeSoft Solutions", 12f, gray)
        footer.gravity = Gravity.CENTER
        footer.setPadding(0, dp(14), 0, 0)
        content.addView(
            footer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    // ---------- refresh ----------

    private fun refresh() {
        val all = Store.loadAll(this)

        if (Store.isServiceEnabled(this)) {
            statusText.text = "✅ Service chalu hai — reels gin rahi hai"
            statusText.setTextColor(0xFF1B8A3A.toInt())
        } else {
            statusText.text = "⚠️ Service band hai — Accessibility mein ON karein"
            statusText.setTextColor(0xFFD35400.toInt())
        }

        val t = Store.today(all)
        todayBig.text = t.sum().toString()
        for ((i, v) in appValues.withIndex()) v.text = t[i].toString()

        val month = Store.monthToDate(all)
        weekVal.text = Store.rangeSum(all, 7).toString()
        monthVal.text = month.toString()
        totalVal.text = Store.allTime(all).toString()

        when (chartMode) {
            0 -> {
                chart.data = Store.dailySeries(all)
                chartTitle.text = "Pichhle 7 din"
            }
            1 -> {
                chart.data = Store.weeklySeries(all)
                chartTitle.text = "Pichhle 8 hafte (har bar = 7 din, label = shuruaat ki tareekh)"
            }
            else -> {
                chart.data = Store.monthlySeries(all)
                chartTitle.text = "Pichhle 6 mahine"
            }
        }
        for ((i, b) in tabButtons.withIndex()) b.alpha = if (i == chartMode) 1f else 0.5f

        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        extraText.text = "Sab se zyada ek din mein: " + Store.bestDay(all) +
            "   •   Is mahine roz ka average: " + (month / dayOfMonth)
    }
}
