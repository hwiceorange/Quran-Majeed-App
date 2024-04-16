package com.bible.tools.extension

import android.content.Context
import com.bible.tools.utils.LoadingDialog

fun showLoadingDialog(context: Context):LoadingDialog {
    return LoadingDialog(context).apply { show() }
}

fun hideLoadingDialog(dialog: LoadingDialog?){
    dialog?.dismiss()
}