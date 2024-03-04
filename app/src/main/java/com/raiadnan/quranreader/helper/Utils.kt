package com.raiadnan.quranreader.helper

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.core.content.edit

import com.raiadnan.quranreader.R
import com.google.firebase.database.*
import com.raiadnan.quranreader.ui.Config


object Utils {
    fun databaseRef(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    fun isRateApp(context: Context):Boolean{
        val myPref = context.getSharedPreferences(Config.pref, Context.MODE_PRIVATE)
        return myPref.getBoolean(Config.rated, false)
    }
    fun rateApp(context: Context){
       context.getSharedPreferences(Config.pref, Context.MODE_PRIVATE).edit {
            putBoolean(Config.rated,true)
            apply()
        }
    }

    fun btnClick(view: View, activity: Activity) {
        val myAnim = AnimationUtils.loadAnimation(activity, R.anim.bounce)
        val interpolator = MyBounceInterpolator(0.2, 20.0)
        myAnim.interpolator = interpolator
        view.startAnimation(myAnim)
    }
    internal class MyBounceInterpolator(amplitude: Double, frequency: Double) : Interpolator {
        private var mAmplitude = 1.0
        private var mFrequency = 10.0
        override fun getInterpolation(time: Float): Float {
            return (-1 * Math.pow(Math.E, -time / mAmplitude) *
                    Math.cos(mFrequency * time) + 1).toFloat()
        }

        init {
            mAmplitude = amplitude
            mFrequency = frequency
        }
    }
}