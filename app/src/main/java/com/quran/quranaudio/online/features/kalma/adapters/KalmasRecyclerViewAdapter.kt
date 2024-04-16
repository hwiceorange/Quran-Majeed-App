package com.quran.quranaudio.online.features.kalma.adapters

import com.quran.quranaudio.online.features.kalma.clicklisteners.KalmasRecyclerViewClickListeners
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.KalmaviewholderBinding
import com.quran.quranaudio.online.features.utils.extensions.setSafeOnClickListener

class KalmasRecyclerViewAdapter(
    var activity: Context,
    var kalmasData: Array<String>,
    var kalmasRecyclerViewClickListeners: KalmasRecyclerViewClickListeners
) : RecyclerView.Adapter<KalmasRecyclerViewAdapter.KalmaViewHolder>() {

    var isPlaying: Array<Char> = Array(6, {
        's'
    }) // p for playing, s for stopped

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KalmaViewHolder {
        val view = KalmaviewholderBinding.inflate(LayoutInflater.from(activity), parent, false)
        return KalmaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KalmaViewHolder, position: Int) {
        val kalmaData = kalmasData[position].split("@@@".toRegex()).toTypedArray()
        holder.view.kalmaTitle.text = kalmaData[0]
        holder.view.kalmaArabic.text = kalmaData[2]
        holder.view.kalmaTansilation.text = kalmaData[3]
        holder.view.kalmaEngtranslation.text = kalmaData[4]
        holder.view.kalmaUrdutranslation.text = kalmaData[5]

        if (isPlaying[position] == 'p') {
            holder.view.playImage.setImageResource(R.drawable.ic_pause)
        } else if (isPlaying[position] == 's') {
            holder.view.playImage.setImageResource(R.drawable.ic_play)
        }

        holder.view.playImage.setSafeOnClickListener {
            kalmasRecyclerViewClickListeners.onPlayClick(position)
        }
        holder.view.btnShare.setSafeOnClickListener {
            kalmasRecyclerViewClickListeners.onShareClick(position)
        }
        holder.view.btnStop.setSafeOnClickListener {
            kalmasRecyclerViewClickListeners.onStopClick(position)
        }

    }

    override fun getItemCount(): Int {
        return 6
    }

    fun setisPlaying(isPlaying: Char, position: Int) {
        this.isPlaying[position] = isPlaying
    }

    fun resetisPlaying(){
        isPlaying = Array(6) {
            's'
        }
    }

    inner class KalmaViewHolder(var view: KalmaviewholderBinding) :
        RecyclerView.ViewHolder(view.root)
}
