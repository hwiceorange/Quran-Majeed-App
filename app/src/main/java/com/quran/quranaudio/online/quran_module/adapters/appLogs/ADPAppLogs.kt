package com.quran.quranaudio.online.quran_module.adapters.appLogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.AppBridge
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.api.ApiConfig
import com.quran.quranaudio.online.quran_module.components.appLogs.AppLogModel
import com.quran.quranaudio.online.databinding.LytLogItemBinding
import com.quran.quranaudio.online.quran_module.utils.extensions.copyToClipboard

class ADPAppLogs(
    private val logs: ArrayList<AppLogModel>,
    private val logColor: Int
) : RecyclerView.Adapter<ADPAppLogs.VHAppLog>() {
    inner class VHAppLog(private val binding: LytLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.clipToOutline = true

            binding.root.context.let {
                ViewCompat.setTooltipText(binding.btnDelete, it.getString(R.string.strLabelDelete))
                ViewCompat.setTooltipText(binding.btnCopy, it.getString(R.string.strLabelCopy))
                ViewCompat.setTooltipText(binding.btnGitHub, it.getString(R.string.createIssue))
            }
        }

        fun bind(logModel: AppLogModel) {
            binding.datetime.text = logModel.datetime
            binding.logText.text = logModel.logShort
            binding.place.text = logModel.place
            binding.logText.setTextColor(logColor)

            binding.btnDelete.setOnClickListener {
                if (logModel.file.delete()) {
                    com.quran.quranaudio.online.quran_module.utils.univ.MessageUtils.showRemovableToast(it.context, R.string.logRemoved, Toast.LENGTH_SHORT)
                    logs.removeAt(bindingAdapterPosition)
                    notifyItemRemoved(bindingAdapterPosition)
                }
            }
            binding.btnCopy.setOnClickListener {
                it.context.copyToClipboard(logModel.log)
                com.quran.quranaudio.online.quran_module.utils.univ.MessageUtils.showRemovableToast(it.context, R.string.copiedToClipboard, Toast.LENGTH_SHORT)
            }

            binding.btnGitHub.setOnClickListener {
                it.context.copyToClipboard(logModel.log)
                com.quran.quranaudio.online.quran_module.utils.univ.MessageUtils.showRemovableToast(it.context, R.string.pasteCrashLogGithubIssue, Toast.LENGTH_LONG)
                AppBridge.newOpener(it.context).browseLink(ApiConfig.WHATSAPP_REPORT_URL)
            }
        }
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHAppLog {
        return VHAppLog(LytLogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    override fun onBindViewHolder(holder: VHAppLog, position: Int) {
        holder.bind(logs[position])
    }
}