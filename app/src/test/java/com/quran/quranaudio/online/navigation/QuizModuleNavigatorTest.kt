package com.quran.quranaudio.online.navigation

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.quran.quranaudio.online.prayertimes.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QuizModuleNavigatorTest {
    @Test
    fun quranEntryTargetsCanonicalMainQuizAndIsConsumedOnce() {
        val intent = QuizModuleNavigator.createIntent(ApplicationProvider.getApplicationContext())

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(QuizModuleNavigator.consumeOpenQuiz(intent))
        assertFalse(QuizModuleNavigator.consumeOpenQuiz(intent))
    }
}
