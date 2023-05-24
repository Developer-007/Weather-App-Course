package com.myapps.weatherappcourse

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

object DialogManager {
    fun slocationSettingsDialog(context: Context, listener: Listener){
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Enable location?")
        dialog.setMessage("Loaction disabled. Do you want enable it?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK"){
            _,_ -> listener.onClick(null)
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel"){
                _,_ -> dialog.dismiss()
        }
        dialog.show()
    }

    fun searchByNameDialog(context: Context, listener: Listener){
        val builder = AlertDialog.Builder(context)
        val edName = EditText(context)
        builder.setView(edName)
        val dialog = builder.create()
        dialog.setTitle("City Name:")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK"){
                _,_ -> listener.onClick(edName.text.toString())
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel"){
                _,_ -> dialog.dismiss()
        }
        dialog.show()
    }

    interface Listener{
        fun onClick(name: String?)
    }
}