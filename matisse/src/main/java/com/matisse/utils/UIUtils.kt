package com.matisse.utils

import android.content.Context
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.matisse.entity.IncapableCause
import com.matisse.widget.IncapableDialog

object UIUtils {

    fun handleCause(context: Context, cause: IncapableCause?) {
        if (cause == null)
            return

        when (cause.mForm) {
            IncapableCause.FORM.NONE -> {
                // do nothing.
            }
            IncapableCause.FORM.DIALOG -> {
                // Show description with dialog
                val incapableDialog = IncapableDialog.newInstance(cause.mTitle, cause.mMessage)
                incapableDialog.show((context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name)
            }
        // default is TOAST
            IncapableCause.FORM.TOAST -> Toast.makeText(context, cause.mMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun spanCount(context: Context, gridExpectedSize: Int): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val expected = screenWidth / gridExpectedSize
        var spanCount = Math.round(expected.toFloat())
        if (spanCount == 0) {
            spanCount = 1
        }

        return spanCount
    }
}