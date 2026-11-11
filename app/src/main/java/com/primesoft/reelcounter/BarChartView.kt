package com.primesoft.reelcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Chhota sa bar chart: har bar ke upar value, neeche label. */
class BarChartView(context: Context) : View(context) {

    var data: List<Pair<String, Int>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6C5CE7.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF555555.toInt()
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return
        val d = resources.displayMetrics.density
        textPaint.textSize = 11f * d
        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = 18f * d
        val topPad = 18f * d
        var max = 1
        for (p in data) if (p.second > max) max = p.second
        val slot = w / data.size
        val barW = slot * 0.55f
        for ((i, p) in data.withIndex()) {
            val cx = slot * i + slot / 2f
            var bh = (h - labelH - topPad) * p.second / max
            if (bh < 2f * d) bh = 2f * d
            val top = h - labelH - bh
            canvas.drawRoundRect(cx - barW / 2f, top, cx + barW / 2f, h - labelH, 6f * d, 6f * d, barPaint)
            canvas.drawText(p.second.toString(), cx, top - 4f * d, textPaint)
            canvas.drawText(p.first, cx, h - 4f * d, textPaint)
        }
    }
}
