package com.arturkowalczyk300.cryptocurrencyprices.View

import android.content.Context
import android.widget.TextView
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight

class ChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private lateinit var tvMarkerText: TextView

    init {
        tvMarkerText = findViewById(R.id.tvMarkerText)
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvMarkerText.text = e?.y.toString() ?: "unknown"

        super.refreshContent(e, highlight)
    }
}