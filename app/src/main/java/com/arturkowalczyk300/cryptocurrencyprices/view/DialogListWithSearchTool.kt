package com.arturkowalczyk300.cryptocurrencyprices.view

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import com.arturkowalczyk300.cryptocurrencyprices.R

class DialogListWithSearchTool {
    private lateinit var dialog: Dialog
    private lateinit var onItemClickListener: (itemText: String) -> Unit
    var isListenerSet = false

    fun open(context: Context, data: List<Pair<String, String>>) {
        val listView = showDialog(context)
        setListAdapter(context, data, listView)
        handleSearchFilter(listView)
    }

    private fun showDialog(context: Context): ListView {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_searchable_list)
        dialog.show()
        val listView = dialog.findViewById(R.id.dialogListView) as ListView
        return listView
    }

    private fun setListAdapter(
        context: Context,
        data: List<Pair<String,String>>,
        listView: ListView,
    ) {
        val adapter = DialogListDataAdapter(context, R.layout.my_spinner_item, data)
        adapter.setDropDownViewResource(R.layout.my_spinner_item)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val itemText = listView.adapter.getItem(position) as Pair<String, String>
            onItemClickListener.invoke(itemText.first)
            dialog.dismiss()
        }
    }

    private fun handleSearchFilter(listView: ListView) {
        val editTextFilter = dialog.findViewById(R.id.dialogEtFilter) as EditText
        editTextFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                (listView.adapter as ArrayAdapter<*>).filter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    fun setOnItemClickListener(listener: (itemText: String) -> Unit) {
        onItemClickListener = listener
        isListenerSet = true
    }
}