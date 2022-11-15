package com.arturkowalczyk300.cryptocurrencyprices.View

import android.content.Context
import android.widget.TextView
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

const val X_OFFSET = -100.0f
const val Y_OFFSET = 0

class ChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private var tvMarkerText: TextView = findViewById(R.id.tvMarkerText)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvMarkerText.text = e?.y.toString() ?: "unknown"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF? {
        return MPPointF((-(width / 2)).toFloat() + X_OFFSET, (-height).toFloat() + Y_OFFSET)
    }
}