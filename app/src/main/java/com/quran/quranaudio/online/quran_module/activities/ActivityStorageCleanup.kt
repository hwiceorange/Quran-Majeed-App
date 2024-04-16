package com.quran.quranaudio.online.quran_module.activities

import android.os.Bundle
import android.view.View
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.ActivityStorageCleanupBinding
import com.quran.quranaudio.online.quran_module.frags.storageCleapup.FragStorageCleanupBase
import com.quran.quranaudio.online.quran_module.frags.storageCleapup.FragStorageCleanupMain

class ActivityStorageCleanup : com.quran.quranaudio.online.quran_module.activities.base.BaseActivity() {

    override fun getLayoutResource() = R.layout.activity_storage_cleanup

    override fun shouldInflateAsynchronously() = true

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityStorageCleanupBinding.bind(activityView)

        if (savedInstanceState != null) {
            onFragChanged(binding)
        } else {
            initFrag(binding)
        }

        initHeader(binding.header)
        supportFragmentManager.addOnBackStackChangedListener { onFragChanged(binding) }
    }

    private fun initHeader(header: com.quran.quranaudio.online.quran_module.views.BoldHeader) {
        header.setBGColor(R.color.colorBGPage)
    }

    private fun initFrag(binding: ActivityStorageCleanupBinding) {
        val fm = supportFragmentManager
        val t = fm.beginTransaction()
        t.add(
            R.id.frags_container,
            FragStorageCleanupMain::class.java,
            null,
            FragStorageCleanupMain::class.simpleName
        )
        t.setReorderingAllowed(true)
        t.runOnCommit { onFragChanged(binding) }
        t.commit()
    }

    private fun onFragChanged(binding: ActivityStorageCleanupBinding) {
        val frag = supportFragmentManager.findFragmentById(R.id.frags_container)
        if (frag is FragStorageCleanupBase) {
            frag.setupHeader(this, binding.header)
        }
    }
}
