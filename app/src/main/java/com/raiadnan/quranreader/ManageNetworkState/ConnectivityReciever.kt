@file:Suppress("DEPRECATION")

package com.raiadnan.quranreader.ManageNetworkState


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class ConnectivityReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, arg1: Intent) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
        if (connectivityReceiverListener != null) {
            connectivityReceiverListener!!.onNetworkConnectionChanged(
                isConnected
            )
        }
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    companion object {
        var connectivityReceiverListener: ConnectivityReceiverListener? = null
        val isConnected: Boolean
            @SuppressLint("MissingPermission")
            get() {
                val cm =  MyApplication.instance!!.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetworkInfo
                return (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
            }
    }
}