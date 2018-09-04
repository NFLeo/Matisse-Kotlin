package com.matisse.entity

class IncapableCause {

    var mForm = FORM.TOAST
    var mTitle: String? = null
    var mMessage: String? = null

    enum class FORM {
        TOAST, DIALOG, NONE
    }

    constructor(message: String) : this(FORM.TOAST, "", message)

    constructor(title: String, message: String) : this(FORM.TOAST, title, message)

    constructor(form: FORM, message: String) : this(form, "", message)

    constructor(form: FORM, title: String, message: String) {
        mForm = form
        mTitle = title
        mMessage = message
    }

    /**
     * handleCause(Context context, IncapableCause cause)
     *
     * 为静态方法 见UIUtils.handleCause(Context, IncapableCause)
     **/
}