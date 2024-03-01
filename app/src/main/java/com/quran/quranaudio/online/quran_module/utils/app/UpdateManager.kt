
package com.quran.quranaudio.online.quran_module.utils.app

import android.content.Context
import android.view.ViewGroup
import com.quran.quranaudio.online.quran_module.components.AppUpdateInfo
import com.quran.quranaudio.online.quran_module.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateManager(private val ctx: Context, private val parent: ViewGroup?) {

    fun refreshAppUpdatesJson() {
        CoroutineScope(Dispatchers.IO).launch {

        }
    }

    fun check4Update(): Boolean {
        val priority = AppUpdateInfo(ctx).getMostImportantUpdate().priority
        Logger.print("Update priority = $priority")

        when (priority) {
            AppUpdateInfo.NONE -> {
                return false
            }

        }

        return priority == AppUpdateInfo.CRITICAL
    }

}
