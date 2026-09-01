package com.quran.quranaudio.online.home

import com.quran.quranaudio.online.tafsir.TafsirSessionAdFreeManager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeSessionAdFreeManagerTest {
    @Before
    fun setUp() {
        HomeSessionAdFreeManager.resetForTests()
        TafsirSessionAdFreeManager.resetForTests()
    }

    @After
    fun tearDown() {
        HomeSessionAdFreeManager.resetForTests()
        TafsirSessionAdFreeManager.resetForTests()
    }

    @Test
    fun subscriptionReturnDoesNotGrantAdRemoval() {
        HomeSessionAdFreeManager.enableRewardedAlternative()

        assertTrue(HomeSessionAdFreeManager.shouldOfferRewardedAlternative())
        assertFalse(HomeSessionAdFreeManager.isActive())
    }

    @Test
    fun earnedRewardActivatesOnlyTheHomeSessionState() {
        HomeSessionAdFreeManager.enableRewardedAlternative()
        HomeSessionAdFreeManager.activate()

        assertTrue(HomeSessionAdFreeManager.isActive())
        assertFalse(HomeSessionAdFreeManager.shouldOfferRewardedAlternative())
        assertFalse(TafsirSessionAdFreeManager.isActive())
    }
}
