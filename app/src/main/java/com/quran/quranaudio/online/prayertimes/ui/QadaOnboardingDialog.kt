package com.quran.quranaudio.online.prayertimes.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.prayertimes.repository.PrayerLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Qada' 追溯引导对话框
 * 
 * 用于新用户首次配置 Qada' 起始日期
 */
class QadaOnboardingDialog(
    private val context: Context,
    private val onConfigured: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "QadaOnboardingDialog"
    }
    
    private val repository = PrayerLogRepository()
    private var dialog: Dialog? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private var isCustomDateSelected = false
    
    fun show() {
        val dialog = Dialog(context)
        this.dialog = dialog
        
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_qada_onboarding, null, false)
        dialog.setContentView(view)
        
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set dialog width to 90% of screen width
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        
        setupViews(view)
        
        dialog.show()
    }
    
    private fun setupViews(view: View) {
        val cardOptionToday: CardView = view.findViewById(R.id.card_option_today)
        val cardOptionCustom: CardView = view.findViewById(R.id.card_option_custom)
        val radioToday: RadioButton = view.findViewById(R.id.radio_option_today)
        val radioCustom: RadioButton = view.findViewById(R.id.radio_option_custom)
        val tvSelectedDate: TextView = view.findViewById(R.id.tv_selected_date)
        val btnConfirm: MaterialButton = view.findViewById(R.id.btn_confirm)
        
        // Option A: Start from Today (默认选中)
        cardOptionToday.setOnClickListener {
            selectOptionToday(cardOptionToday, cardOptionCustom, radioToday, radioCustom)
        }
        
        radioToday.setOnClickListener {
            selectOptionToday(cardOptionToday, cardOptionCustom, radioToday, radioCustom)
        }
        
        // Option B: Start from Custom Date
        cardOptionCustom.setOnClickListener {
            selectOptionCustom(cardOptionToday, cardOptionCustom, radioToday, radioCustom, tvSelectedDate)
        }
        
        radioCustom.setOnClickListener {
            selectOptionCustom(cardOptionToday, cardOptionCustom, radioToday, radioCustom, tvSelectedDate)
        }
        
        // Confirm Button
        btnConfirm.setOnClickListener {
            confirmAndSave()
        }
    }
    
    private fun selectOptionToday(
        cardToday: CardView,
        cardCustom: CardView,
        radioToday: RadioButton,
        radioCustom: RadioButton
    ) {
        isCustomDateSelected = false
        selectedDate = LocalDate.now()
        
        radioToday.isChecked = true
        radioCustom.isChecked = false
        
        // Update card styles
        cardToday.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green_background))
        cardCustom.setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey_100))
        
        Log.d(TAG, "Selected: Start from Today ($selectedDate)")
    }
    
    private fun selectOptionCustom(
        cardToday: CardView,
        cardCustom: CardView,
        radioToday: RadioButton,
        radioCustom: RadioButton,
        tvSelectedDate: TextView
    ) {
        isCustomDateSelected = true
        
        radioToday.isChecked = false
        radioCustom.isChecked = true
        
        // Update card styles
        cardToday.setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey_100))
        cardCustom.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green_background))
        
        // Show date picker
        showDatePicker(tvSelectedDate)
    }
    
    private fun showDatePicker(tvSelectedDate: TextView) {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            context,
            R.style.QadaDatePickerTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                
                // Format and display the selected date
                val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault())
                val formattedDate = selectedDate.format(formatter)
                tvSelectedDate.text = formattedDate
                
                Log.d(TAG, "Selected custom date: $selectedDate")
            },
            year,
            month,
            day
        )
        
        // Set max date to today (can't select future dates)
        datePickerDialog.datePicker.maxDate = today.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun confirmAndSave() {
        val startDateStr = selectedDate.toString() // YYYY-MM-DD format
        
        Log.d(TAG, "Confirming Qada start date: $startDateStr (isCustom: $isCustomDateSelected)")
        
        // Save to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.setQadaStartDate(startDateStr)
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "✅ Qada start date saved successfully")
                    Toast.makeText(context, "Qada' tracking configured", Toast.LENGTH_SHORT).show()
                    
                    dialog?.dismiss()
                    onConfigured(startDateStr)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving Qada start date", e)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

