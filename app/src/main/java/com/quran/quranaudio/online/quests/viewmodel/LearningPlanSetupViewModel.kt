package com.quran.quranaudio.online.quests.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quran.quranaudio.online.quests.data.ReadingGoalUnit
import com.quran.quranaudio.online.quests.data.UserQuestConfig
import com.quran.quranaudio.online.quests.repository.QuestRepository
import com.quran.quranaudio.online.quests.utils.QuranDataHelper
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * ViewModel for Learning Plan Setup screen.
 * 
 * Responsibilities:
 * - Load and expose user's saved quest configuration
 * - Support multiple reading units (Pages, Verses, Juz')
 * - Calculate challenge duration based on user's selected unit and goal
 * - Save user's quest configuration to Firestore
 * - Initialize streak stats after first-time setup
 */
class LearningPlanSetupViewModel(
    private val questRepository: QuestRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LearningPlanSetupVM"
    }

    // StateFlow for user's saved configuration (用于回显上次保存的配置)
    val userConfig: StateFlow<UserQuestConfig?> = questRepository.observeUserQuestConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Current selected reading unit
    private val _selectedUnit = MutableLiveData<ReadingGoalUnit>(ReadingGoalUnit.PAGES)
    val selectedUnit: LiveData<ReadingGoalUnit> = _selectedUnit

    // Dynamic slider range based on selected unit
    private val _sliderRange = MutableLiveData<QuranDataHelper.UnitRange>()
    val sliderRange: LiveData<QuranDataHelper.UnitRange> = _sliderRange

    // LiveData for challenge days calculation
    private val _challengeDays = MutableLiveData<Int>(QuranDataHelper.TOTAL_PAGES) // Default: 604 days
    val challengeDays: LiveData<Int> = _challengeDays

    // LiveData for save operation status
    private val _saveStatus = MutableLiveData<SaveStatus>()
    val saveStatus: LiveData<SaveStatus> = _saveStatus

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        // Initialize with default unit (VERSES - 最小可管理单位，降低新用户畏难心理)
        setReadingUnit(ReadingGoalUnit.VERSES)
    }

    /**
     * Set the reading goal unit (Pages, Verses, or Juz').
     * Automatically updates the slider range.
     */
    fun setReadingUnit(unit: ReadingGoalUnit) {
        _selectedUnit.value = unit
        
        // Update slider range based on unit
        val range = QuranDataHelper.getRangeForUnit(unit)
        _sliderRange.value = range
        
        Log.d(TAG, "Reading unit changed to: ${unit.displayName}, range: ${range.min}-${range.max}")
    }

    /**
     * Calculates the estimated challenge duration to complete Quran reading.
     * 
     * Supports multiple units:
     * - PAGES: Days = ⌈604 ÷ dailyGoal⌉
     * - VERSES: Days = ⌈6236 ÷ dailyGoal⌉
     * - JUZ: Days = ⌈30 ÷ dailyGoal⌉
     * 
     * @param unit The reading goal unit (PAGES, VERSES, or JUZ)
     * @param dailyGoal How many units per day
     * @param recitationMinutes Daily recitation minutes (not used for calculation)
     * @param recitationEnabled Whether recitation is enabled (not used for calculation)
     * @return Calculated challenge days
     */
    fun calculateChallengeDays(
        unit: ReadingGoalUnit,
        dailyGoal: Int,
        recitationMinutes: Int = 15,
        recitationEnabled: Boolean = true
    ): Int {
        // Use QuranDataHelper for calculation
        val days = QuranDataHelper.calculateChallengeDays(unit, dailyGoal)
        
        _challengeDays.value = days
        Log.d(TAG, "Challenge days calculated: $days (${unit.displayName}: $dailyGoal per day)")
        
        return days
    }
    
    /**
     * Legacy method for backward compatibility.
     * Delegates to the new method with PAGES unit.
     */
    fun calculateChallengeDays(
        readingPages: Int,
        recitationMinutes: Int,
        recitationEnabled: Boolean
    ): Int {
        return calculateChallengeDays(ReadingGoalUnit.PAGES, readingPages, recitationMinutes, recitationEnabled)
    }

    /**
     * Saves the user's quest configuration to Firestore.
     * Also initializes streak stats if this is the first time.
     * 
     * @param config The quest configuration to save
     */
    fun saveUserQuest(config: UserQuestConfig) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                Log.d(TAG, "开始保存配置: $config")
                
                // 添加超时保护（15秒）
                withTimeout(15000L) {
                    // 1. Save quest configuration
                    questRepository.saveUserQuestConfig(config)
                    Log.d(TAG, "Quest config saved successfully")
                    
                    // 2. Initialize streak stats (if first time)
                    questRepository.initializeStreakStats()
                    Log.d(TAG, "Streak stats initialized")
                }
                
                // 3. Notify success
                Log.d(TAG, "准备发送 Success 状态")
                _saveStatus.postValue(SaveStatus.Success)
                _isLoading.postValue(false)
                Log.d(TAG, "Success 状态已发送")
                
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "保存超时", e)
                _saveStatus.postValue(SaveStatus.Error("保存超时，请检查网络连接"))
                _isLoading.postValue(false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save quest config", e)
                _saveStatus.postValue(SaveStatus.Error(e.message ?: "Failed to save configuration"))
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Resets the save status (used after navigation)
     */
    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    /**
     * Sealed class representing save operation status
     */
    sealed class SaveStatus {
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}


