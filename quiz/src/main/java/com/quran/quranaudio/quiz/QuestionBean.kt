package com.quran.quranaudio.quiz

import android.os.Parcelable
import androidx.annotation.Keep
import java.util.Random
import java.util.TreeMap
import kotlinx.parcelize.Parcelize
@Keep
@Parcelize
data class QuestionBean(
    val id: Int = 0,
    val question: String = "",
    val options: TreeMap<String, String> = TreeMap(),
    val difficulty: Int = 0,
    val answer: String = ""
): Parcelable {
    fun getRightAnswer(): String {
        return options[answer] ?: ""
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