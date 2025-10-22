package com.quran.quranaudio.online.quests.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quran.quranaudio.online.quests.data.DailyProgressModel
import com.quran.quranaudio.online.quests.data.StreakStats
import com.quran.quranaudio.online.quests.data.UserQuestConfig
import com.quran.quranaudio.online.quests.repository.QuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Home screen Quest feature.
 * 
 * Responsibilities:
 * - Expose quest configuration, streak stats, and daily progress as LiveData
 * - Trigger cross-day streak check on initialization
 * - Provide loading states
 */
class HomeQuestsViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeQuestsViewModel"
    }

    private lateinit var questRepository: QuestRepository

    // LiveData for quest configuration
    private val _questConfig = MutableLiveData<UserQuestConfig?>()
    val questConfig: LiveData<UserQuestConfig?> = _questConfig

    // LiveData for streak statistics
    private val _streakStats = MutableLiveData<StreakStats>()
    val streakStats: LiveData<StreakStats> = _streakStats

    // LiveData for today's progress
    private val _todayProgress = MutableLiveData<DailyProgressModel?>()
    val todayProgress: LiveData<DailyProgressModel?> = _todayProgress

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Initializes the repository (called from DailyQuestsManager)
     */
    fun initializeRepository(repository: QuestRepository) {
        this.questRepository = repository
        observeData()
    }

    /**
     * Observes Firebase data
     */
    private fun observeData() {
        viewModelScope.launch {
            try {
                // Observe quest configuration
                questRepository.observeUserQuestConfig().asLiveData().observeForever { config ->
                    _questConfig.value = config
                    Log.d(TAG, "Quest config updated: ${if (config != null) "exists" else "null"}")
                }

                // Observe streak stats
                questRepository.observeStreakStats().asLiveData().observeForever { stats ->
                    _streakStats.value = stats
                    Log.d(TAG, "Streak stats updated: ${stats.currentStreak} days")
                }

                // Observe today's progress
                questRepository.observeTodayProgress().asLiveData().observeForever { progress ->
                    _todayProgress.value = progress
                    Log.d(TAG, "Today's progress updated: ${progress?.allTasksCompleted ?: false}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error observing data", e)
            }
        }
    }

    /**
     * Triggers cross-day streak check
     */
    fun checkAndResetStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking and resetting streak if needed...")
                questRepository.checkAndResetStreak()
                Log.d(TAG, "Streak check completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking streak", e)
            }
        }
    }

    // Note: Kotlin properties automatically generate getters (getQuestConfig(), getStreakStats(), getTodayProgress())
    // which can be accessed from Java code. No need to define explicit getter functions.
}



