package com.quran.quranaudio.quiz

import android.os.Parcelable
import androidx.annotation.Keep
import java.util.Random
import java.util.TreeMap
import kotlinx.parcelize.Parcelize
@Keep
@Parcelize
data class QuestionBean(
    val id: String = "",  // Changed to String to match "1-1-1" format
    val question: String = "",
    val options: TreeMap<String, String> = TreeMap(),
    val difficulty: Int = 0,
    val answer: String = "",
    val Category: String = "",
    val Subclass: String = "",
    val surah_id: Int = 0,
    val ayah_id: Int = 0,
    val tafsir_brief: String = "",  // Tafsir slug for brief commentary (e.g., "en-tafsir-muyassar")
    val tafsir_detailed: String = "", // Tafsir slug for detailed commentary (e.g., "en-tafsir-ibn-kathir")
    val explanation: String = ""
): Parcelable {
    fun getRightAnswer(): String {
        return options[answer] ?: ""
    }
    
    /**
     * 随机化选项内容，保持ABCD键名顺序不变
     * 返回: Pair<新的options, 新的answer键>
     */
    fun getShuffledQuestion(): Pair<TreeMap<String, String>, String> {
        // 获取所有选项的值（内容）
        val allValues = options.values.toMutableList()
        // 打乱内容
        allValues.shuffle()
        
        // 重新分配给A、B、C、D
        val newOptions = TreeMap<String, String>()
        val keys = listOf("A", "B", "C", "D")
        keys.forEachIndexed { index, key ->
            if (index < allValues.size) {
                newOptions[key] = allValues[index]
            }
        }
        
        // 找到正确答案现在在哪个位置
        val correctAnswer = getRightAnswer()
        var newAnswerKey = "A"
        for ((key, value) in newOptions) {
            if (value == correctAnswer) {
                newAnswerKey = key
                break
            }
        }
        
        return Pair(newOptions, newAnswerKey)
    }
    
    fun getRandomAnswer():Pair<String,String>{
        val rightAnswer = getRightAnswer()
        val filter = options.filter { !it.key.contains(answer) }
        val randomAnswer = options[filter.keys.random()]!!
        val nextInt = Random().nextInt(100)
        return if (nextInt % 2 == 0) {
            Pair(rightAnswer, randomAnswer)
        } else {
            Pair(randomAnswer, rightAnswer)
        }
    }
}