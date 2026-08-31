package com.quranaudio.common.ad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryAdFreeManagerTest {
    @Test fun activeInsideOneHourWindow() {
        assertTrue(TemporaryAdFreeManager.isWindowValid(1_000L, 1_000L + 3_600_000L, 2_000L))
    }

    @Test fun expiresAtBoundary() {
        assertFalse(TemporaryAdFreeManager.isWindowValid(1_000L, 4_000L, 4_000L))
    }

    @Test fun rejectsClockRollbackAndOversizedGrant() {
        assertFalse(TemporaryAdFreeManager.isWindowValid(2_000L, 3_000L, 1_999L))
        assertFalse(TemporaryAdFreeManager.isWindowValid(1_000L, 1_000L + 3_600_001L, 2_000L))
    }
}
