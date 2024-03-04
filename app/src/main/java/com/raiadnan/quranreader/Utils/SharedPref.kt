package com.raiadnan.quranreader.Utils

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.raiadnan.quranreader.R

const val defaultLat = 33.97159194946289F
const val defaultLng = -6.849812984466553F
class SharePref(context: Context) {
    private val mySharePref: SharedPreferences
    val mContext = context




    fun setIsFirstTime(firstTime:Boolean){
        val editor = mySharePref.edit()
        editor.putBoolean("first_time",firstTime)
        editor.apply()
    }

    fun setFirstTimeAyah(firstTime:Boolean){
        val editor = mySharePref.edit()
        editor.putBoolean("first_time_ayah",firstTime)
        editor.apply()
    }


    fun saveSwitchState(state:Boolean){
        val editor = mySharePref.edit()
        editor.putBoolean(mContext.getString(R.string.pref_notification_key),state)
        editor.apply()
    }

    fun loadSwitchState():Boolean{
        val state = mySharePref.getBoolean(mContext.getString(R.string.pref_notification_key),true)
        return state
    }




    fun loadIsFirstTimePref(): Boolean {
        return mySharePref.getBoolean("first_time", true)
    }


    fun loadFirstTimeAyah():Boolean{
        return mySharePref.getBoolean("first_time_ayah", true)
    }


    init {
        mySharePref = context.getSharedPreferences("filename", Context.MODE_PRIVATE)
    }
}
