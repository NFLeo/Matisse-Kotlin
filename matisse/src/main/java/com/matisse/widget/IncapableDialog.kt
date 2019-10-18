package com.matisse.widget

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import com.matisse.R

class IncapableDialog : DialogFragment() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_MESSAGE = "extra_message"

        fun newInstance(title: String?, message: String?): IncapableDialog {
            val dialog = IncapableDialog()
            val bundle = Bundle()
            bundle.putString(EXTRA_TITLE, title)
            bundle.putString(EXTRA_MESSAGE, message)
            dialog.arguments = bundle
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val title = arguments?.getString(EXTRA_TITLE) ?: ""
        val message = arguments?.getString(EXTRA_MESSAGE) ?: ""

        val builder = activity?.let { AlertDialog.Builder(it) }
        if (title.isNotEmpty()) builder?.setTitle(title)

        if (message.isNotEmpty()) {
            builder?.setMessage(message)
        }

        builder?.setPositiveButton(R.string.button_ok) { dialog, _ -> dialog?.dismiss() }

        return builder?.create()!!
    }
}