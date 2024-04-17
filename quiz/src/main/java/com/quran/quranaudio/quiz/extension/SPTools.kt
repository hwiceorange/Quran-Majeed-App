package com.quran.quranaudio.quiz.extension

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

object SPTools {

   var sp:SharedPreferences?=null
    fun init(context:Application){
        sp =context.getSharedPreferences("quran_quiz", Context.MODE_PRIVATE)
    }

    fun put(key:String, value:Any){
        var mmkv= sp?.edit()
        when (value){
            is Boolean -> mmkv?.putBoolean(key, value)
            is String -> mmkv?.putString(key, value)
            is Long -> mmkv?.putLong(key, value)
            is Float -> mmkv?.putFloat(key, value)
            is Int -> mmkv?.putInt(key, value)
            is ByteArray ->mmkv?.putString(key,Base64.encodeToString(value,Base64.DEFAULT))
            is Nothing -> return
        }
        mmkv?.commit()//.apply()
    }

    fun putSet(key:String,  value:Set<String>){
        var mmkv= sp?.edit()
        mmkv?.putStringSet(key, value)
        mmkv?.apply()
    }

    fun getInt(key:String, def:Int): Int {
        return sp?.getInt(key, def) ?: def
    }

    fun getString(key:String, def:String): String {
        return sp?.getString(key, def) ?: def
    }

    fun getLong(key:String, def:Long): Long {
        return sp?.getLong(key, def) ?: def
    }

    fun getFloat(key:String, def:Float): Float {
        return sp?.getFloat(key, def) ?: def
    }

    fun getBoolean(key:String, def:Boolean): Boolean {
        return sp?.getBoolean(key, def) ?: def
    }

    fun getSet(key:String, def:Set<String>): Set<String> {
        return sp?.getStringSet(key, def) ?: def
    }
    fun getBytes(key:String,def:ByteArray):ByteArray {
        try {
            var temp = sp?.getString(key, null)
            return Base64.decode(temp, Base64.DEFAULT) ?: def
        }catch (e:Exception){
            e.printStackTrace()
        }
        return def
    }

}