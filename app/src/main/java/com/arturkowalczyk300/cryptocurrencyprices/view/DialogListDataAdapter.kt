package com.arturkowalczyk300.cryptocurrencyprices.view

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.arturkowalczyk300.cryptocurrencyprices.R

class DialogListDataAdapter(
    context: Context,
    resource: Int,
    private var data: List<Pair<String, String>>,
) :
    ArrayAdapter<Pair<String, String>>(context, resource, data) {
    private val originalData: List<Pair<String, String>> = data.toList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.my_spinner_item, parent, false)

        val currentItem = getItem(position) as Pair<String, String>

        val textViewName: TextView = view.findViewById(R.id.item_cryptocurrency_name)
        val textViewSymbol: TextView = view.findViewById(R.id.item_cryptocurrency_symbol)

        textViewName.text = currentItem.first
        textViewSymbol.text = currentItem.second

        return view
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                var filterResults = FilterResults()
                constraint?.let {

                    val filtered = originalData.filter {
                        it.first.contains(constraint) || it.second.contains(constraint)
                    }

                    filterResults.values = filtered
                    filterResults.count = filtered.count()
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val filteredData = results?.values as List<Pair<String, String>>
                data = filteredData
                clear()
                addAll(data)
                notifyDataSetChanged()
            }
        }
    }
}