package com.matisse.listener

import android.content.Context
import com.matisse.entity.IncapableCause

interface NoticeConsumer {
    /**
     * 提示方式实现类
     * @param activity 宿主activity
     * @param noticeType 提示类型 TOAST、DIALOG
     * @param title 提示标题
     * @param message 提示内容
     */
    fun accept(
        context: Context, @IncapableCause.Form noticeType: Int,
        title: String, message: String
    )
}