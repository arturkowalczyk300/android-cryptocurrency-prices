package com.arturkowalczyk300.cryptocurrencyprices.view

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceAlertEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity

class PriceAlertRecyclerAdapter(
    private val context: Context,
    private val data: List<PriceAlertEntity>,
    private val itemDeleteCallback: (entity: PriceAlertEntity) -> Unit
) :
    RecyclerView.Adapter<PriceAlertRecyclerAdapter.PriceAlertViewholder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceAlertViewholder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alerts_recycler_item, parent, false)

        return PriceAlertViewholder(view, context)
    }

    override fun onBindViewHolder(holder: PriceAlertViewholder, position: Int) {
        val item = data[position]
        holder.bind(item)
        holder.alertDelete.setOnClickListener {
            itemDeleteCallback.invoke(item)
        }
    }

    override fun getItemCount() = data.size

    class PriceAlertViewholder(view: View, private val context: Context) :
        RecyclerView.ViewHolder(view) {
        val cryptocurrencySymbol: TextView =
            view.findViewById(R.id.recyclerItemCryptocurrencySymbol)
        val alertType: TextView = view.findViewById(R.id.recyclerItemAlertType)
        val alertDelete: ImageView = view.findViewById(R.id.recyclerItemAlertDelete)

        fun bind(alert: PriceAlertEntity) {
            cryptocurrencySymbol.text = alert.cryptocurrencySymbol

            val arr = context.resources.getStringArray(R.array.alertTypes)
            alertType.text = arr[alert.alertType.ordinal]
        }
    }
}