package com.raiadnan.quranreader

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.raiadnan.quranreader.Utils.Network.ActivityLifecycleCallbacksImp
import com.raiadnan.quranreader.Utils.Network.Core.NetworkCallbackImp
import com.raiadnan.quranreader.Utils.Network.Core.NetworkEvent
import com.raiadnan.quranreader.Utils.Network.Core.NetworkStateImp
import com.raiadnan.quranreader.Utils.Network.Event
import com.raiadnan.quranreader.Utils.Network.NetworkEvents
import com.raiadnan.quranreader.Utils.Network.NetworkState

object ConnectivityStateHolder : ConnectivityState {

    private val mutableSet: MutableSet<NetworkState> = mutableSetOf()

    override val networkStats: Iterable<NetworkState>
        get() = mutableSet


    private fun networkEventHandler(state: NetworkState, event: NetworkEvent) {
        when (event) {
            is NetworkEvent.AvailabilityEvent -> {
                if (isConnected != event.oldNetworkAvailability) {
                    NetworkEvents.notify(Event.ConnectivityEvent(state.isAvailable))
                }
            }
            else -> {
              //  NetworkEvents.notify(Event.ConnectivityEvent(state.isAvailable))
            }
        }
    }

    /**
     * This starts the broadcast of network events to NetworkState and all Activity implementing NetworkConnectivityListener
     * @see NetworkState
     * @see NetworkConnectivityListener
     */
    @SuppressLint("MissingPermission")
    fun Application.registerConnectivityBroadcaster() {
        //register the Activity Broadcaster
        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImp())

        //get connectivity manager
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //register to network events
        listOf(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        ).forEach {
            val stateHolder = NetworkStateImp { a, b -> networkEventHandler(a, b) }
            mutableSet.add(stateHolder)
            connectivityManager.registerNetworkCallback(it, NetworkCallbackImp(stateHolder))
        }

    }

}