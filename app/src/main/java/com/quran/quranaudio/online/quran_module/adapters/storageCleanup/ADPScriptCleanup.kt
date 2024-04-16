package com.quran.quranaudio.online.quran_module.adapters.storageCleanup

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.components.storageCleanup.ScriptCleanupItemModel
import com.quran.quranaudio.online.databinding.LytStorageCleanupItemBinding
import com.quran.quranaudio.online.quran_module.utils.extensions.layoutInflater
import com.quran.quranaudio.online.quran_module.utils.reader.QuranScriptUtils
import com.quran.quranaudio.online.quran_module.utils.reader.getQuranScriptName

class ADPScriptCleanup(
    private val items: List<ScriptCleanupItemModel>,
    private val fileUtils: com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
) :
    RecyclerView.Adapter<ADPScriptCleanup.VHScriptCleanupItem>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHScriptCleanupItem {
        return VHScriptCleanupItem(
            LytStorageCleanupItemBinding.inflate(
                parent.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHScriptCleanupItem, position: Int) {
        holder.bind(items[position])
    }

    inner class VHScriptCleanupItem(
        private val binding: LytStorageCleanupItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ScriptCleanupItemModel) {
            binding.let {
                it.title.text = model.scriptKey.getQuranScriptName()
                it.subtitle.text = it.root.context.getString(
                    R.string.nScriptsAndFonts,
                    1,
                    model.fontDownloadsCount
                )

                it.iconDelete.setImageResource(
                    if (!model.isCleared) R.drawable.dr_icon_delete else R.drawable.dr_icon_check
                )

                if (!model.isCleared) {
                    it.iconDelete.setOnClickListener {
                        deleteItem(model)
                    }
                } else {
                    it.iconDelete.setOnClickListener(null)
                }

                it.iconDelete.isClickable = !model.isCleared
                it.iconDelete.isFocusable = !model.isCleared
            }
        }

        private fun deleteItem(model: ScriptCleanupItemModel) {
            PeaceDialog.newBuilder(itemView.context).apply {
                setTitle(R.string.titleScriptCleanup)
                setMessage(
                    itemView.context.getString(
                        R.string.msgScriptCleanup,
                        model.scriptKey.getQuranScriptName()
                    )
                )
                setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setNeutralButton(R.string.strLabelCancel, null)
                setDialogGravity(PeaceDialog.GRAVITY_TOP)
                setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER) { _, _ ->
                    if (com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedScript(itemView.context) == model.scriptKey) {
                        com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedScript(itemView.context, QuranScriptUtils.SCRIPT_DEFAULT)
                    }

                    fileUtils.getScriptFile(model.scriptKey).delete()
                    fileUtils.getKFQPCScriptFontDir(model.scriptKey).deleteRecursively()

                    model.isCleared = true
                    notifyItemChanged(adapterPosition)
                }
                setFocusOnNegative(true)
            }.show()
        }
    }
}
