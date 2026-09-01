package com.quran.quranaudio.online.tafsir

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TafsirSessionAdFreeManagerTest {
    @Before
    fun setUp() = TafsirSessionAdFreeManager.resetForTests()

    @After
    fun tearDown() = TafsirSessionAdFreeManager.resetForTests()

    @Test
    fun subscriptionReturnOnlyEnablesRewardedAlternative() {
        TafsirSessionAdFreeManager.enableRewardedAlternative()

        assertTrue(TafsirSessionAdFreeManager.shouldOfferRewardedAlternative())
        assertFalse(TafsirSessionAdFreeManager.isActive())
    }

    @Test
    fun earnedRewardActivatesSessionAndRemovesAlternative() {
        TafsirSessionAdFreeManager.enableRewardedAlternative()
        TafsirSessionAdFreeManager.activate()

        assertTrue(TafsirSessionAdFreeManager.isActive())
        assertFalse(TafsirSessionAdFreeManager.shouldOfferRewardedAlternative())
    }
}
