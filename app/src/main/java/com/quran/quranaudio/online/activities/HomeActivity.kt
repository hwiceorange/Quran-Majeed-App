@file:Suppress("DEPRECATION")
@file:JvmName("DemoUtils")
package com.quran.quranaudio.online.activities

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.github.kayvannj.permission_utils.PermissionUtil.PermissionRequestObject
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.quran.quranaudio.online.*
import com.quran.quranaudio.online.Allah.names99.fragments.Name99Fragment
import com.quran.quranaudio.online.BaseActivity
import com.quran.quranaudio.online.prayertimes.ui.home.HomeFragment
import com.quran.quranaudio.online.prayertimes.ui.home.PrayersFragment
import com.quran.quranaudio.quiz.fragments.QuestionFragment
import java.util.*


@Suppress("DEPRECATION")
class HomeActivity : BaseActivity() {

    @JvmField
    var mRequestObject: PermissionRequestObject? = null


    private var isPermissionGranted = false


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        isStoragePermissionGranted()

        HomeFragment()
        Name99Fragment()

        replaceFragment(HomeFragment())

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        QuestionFragment.isSelected=false
        when (item.itemId) {
            R.id.nav_home -> {
                val mFragment = HomeFragment.newInstance()
                replaceFragment(mFragment)

                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_namaz -> {
                val mFragment = PrayersFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.nav_name_99 -> {
               // startActivity(Intent(this, AllahNameActivity::class.java))
              val mFragment = QuestionFragment()  //com.quran.quranaudio.online.Allah.names99.fragments.Name99Fragment.newInstance()
                QuestionFragment.isSelected=true
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.nav_tasbih -> {
              val mFragment = com.quran.quranaudio.online.tasbih.fragments.TasbihFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                val mFragment = com.quran.quranaudio.online.tasbih.fragments.TasbihFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        true
    }

private fun replaceFragment(fragment: Fragment) {
    if (fragment!=null){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.home_host_fragment,fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}


    //HeaderEnd

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            // Log.v(FragmentActivity.TAG, "Permission is granted")
            isPermissionGranted = true
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionRequestObject = mRequestObject
        permissionRequestObject?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.quran.quranaudio.online.helper.LocaleHelper.onAttach(base))
    }


    override fun onBackPressed() {

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog_ad_exit)
        val yesBtn = dialog.findViewById(R.id.btn_exitdialog_yes) as Button
        val noBtn = dialog.findViewById(R.id.btn_exitdialog_no) as Button

        yesBtn.setOnClickListener {
          //  System.exit(0)
            finish()
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()

    }

    override fun onResume() {
        super.onResume()
        this.onCreate(null)
    }
}