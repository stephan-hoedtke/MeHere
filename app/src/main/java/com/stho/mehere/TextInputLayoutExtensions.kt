package com.stho.mehere

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout


fun <T> MaterialAutoCompleteTextView.setAdapter(context: Context, values: Array<T>) {
    val adapter = ArrayAdapter(context, R.layout.drop_down_list_item, values)
    setAdapter(adapter)
}

fun <T> MaterialAutoCompleteTextView.setValue(value: T) {
    setText(value.toString(), false)
}

fun TextInputLayout.setText(value: String) =
    editText?.also {
        it.setText(value)
    }

fun TextInputLayout.getText(): String =
    editText?.text.toString() ?: ""
