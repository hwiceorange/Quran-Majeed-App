package com.quran.quranaudio.online.quests.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quran.quranaudio.online.quests.data.SalahName
import com.quran.quranaudio.online.quests.data.SalahRecord
import com.quran.quranaudio.online.quests.repository.SalahRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Salah (Prayer) tracking feature.
 * 
 * Responsibilities:
 * - Observe today's salah record in real-time
 * - Toggle prayer completion status
 * - Expose prayer states as LiveData for UI updates
 */
class SalahViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SalahViewModel"
    }
    
    private val salahRepository = SalahRepository()
    
    // LiveData for today's salah record - directly expose Flow as LiveData
    val todaySalahRecord: LiveData<SalahRecord?> = salahRepository.observeTodaySalahRecord().asLiveData()
    
    // LiveData for operation status
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus
    
    /**
     * Toggles the completion status of a specific prayer.
     * 
     * @param salahName The prayer to toggle
     */
    fun toggleSalahStatus(salahName: SalahName) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationStatus.postValue(OperationStatus.Loading)
                
                salahRepository.toggleSalahStatus(salahName)
                
                _operationStatus.postValue(OperationStatus.Success("${salahName.getDisplayName()} status updated"))
                Log.d(TAG, "✅ ${salahName.name} status toggled successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling ${salahName.name} status", e)
                _operationStatus.postValue(OperationStatus.Error("Failed to update prayer: ${e.message}"))
            }
        }
    }
    
    /**
     * Sets the completion status of a specific prayer.
     * 
     * @param salahName The prayer to update
     * @param isCompleted The new completion status
     */
    fun setSalahStatus(salahName: SalahName, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationStatus.postValue(OperationStatus.Loading)
                
                salahRepository.setSalahStatus(salahName, isCompleted)
                
                _operationStatus.postValue(OperationStatus.Success("${salahName.getDisplayName()} marked as ${if (isCompleted) "completed" else "incomplete"}"))
                Log.d(TAG, "✅ ${salahName.name} status set to $isCompleted")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting ${salahName.name} status", e)
                _operationStatus.postValue(OperationStatus.Error("Failed to update prayer: ${e.message}"))
            }
        }
    }
    
    /**
     * Gets the completion status of a specific prayer from the current record.
     */
    fun getSalahStatus(salahName: SalahName): Boolean {
        return when (salahName) {
            SalahName.FAJR -> todaySalahRecord.value?.fajr ?: false
            SalahName.DHUHR -> todaySalahRecord.value?.dhuhr ?: false
            SalahName.ASR -> todaySalahRecord.value?.asr ?: false
            SalahName.MAGHRIB -> todaySalahRecord.value?.maghrib ?: false
            SalahName.ISHA -> todaySalahRecord.value?.isha ?: false
        }
    }
    
    /**
     * Returns the total number of prayers completed today.
     */
    fun getTotalCompleted(): Int {
        return todaySalahRecord.value?.getTotalCompleted() ?: 0
    }
    
    /**
     * Returns true if all 5 prayers are completed today.
     */
    fun areAllCompleted(): Boolean {
        return todaySalahRecord.value?.areAllCompleted() ?: false
    }
}

/**
 * Sealed class representing operation status.
 */
sealed class OperationStatus {
    object Idle : OperationStatus()
    object Loading : OperationStatus()
    data class Success(val message: String) : OperationStatus()
    data class Error(val message: String) : OperationStatus()
}

