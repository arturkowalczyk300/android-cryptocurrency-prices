package com.arturkowalczyk300.cryptocurrencyprices.View

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.R

class DialogListWithSearchTool {
    private var dialog: Dialog? = null

    private lateinit var listenerOnClickItem: (itemText: String) -> Unit

    fun showDialog(context: Context, data: ArrayList<String>) {


        //display dialog
        dialog = Dialog(context)

        dialog!!.setContentView(R.layout.dialog_searchable_list)
        dialog!!.show()
        val listView = dialog!!.findViewById(R.id.dialogListView) as ListView

        //set adapter to list with cryptocurrencies
        val adapter = ArrayAdapter(context, R.layout.my_spinner_item, data)
        adapter.setDropDownViewResource(R.layout.my_spinner_item)
        listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val itemText = listView.adapter.getItem(position).toString()
            listenerOnClickItem.invoke(itemText)
            dialog!!.dismiss()
        }

        //handle search filter
        val editTextFilter = dialog!!.findViewById(R.id.dialogEtFilter) as EditText
        editTextFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                (listView.adapter as ArrayAdapter<*>).filter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    fun setListenerOnClickItem(listener: (itemText: String) -> Unit) {
        listenerOnClickItem = listener
    }
}