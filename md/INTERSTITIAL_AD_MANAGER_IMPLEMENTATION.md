# Interstitial Ad Manager Implementation

## Overview
Implemented a centralized interstitial ad management system that maintains a cache pool with 1 available ad, preloads on cold start, and shows ads when users complete Daily Quests setup.

## Features Implemented

### ✅ Phase 1: Core Ad Manager (`InterstitialAdManager`)
**File:** `adlib/src/main/java/com/quranaudio/common/ad/InterstitialAdManager.kt`

**Key Capabilities:**
- **Singleton Pattern:** Thread-safe singleton for centralized ad management
- **Ad Unit ID:** Reuses existing `AD_INTERS` ID (`ca-app-pub-3966802724737141/2182661506`)
- **Cache Management:** Maintains exactly 1 cached ad at all times
- **Premium User Check:** Automatically skips ad loading/showing for subscribed users

**Properties:**
- `cachedAd: InterstitialAd?` - Stores the available ad object
- `loadTimeMillis: Long` - Timestamp when ad was loaded (for TTL calculation)
- `adRefreshTimer: Timer?` - Periodic timer to check ad expiry
- `isLoading: Boolean` - Prevents duplicate concurrent load requests

### ✅ Phase 2: Loading, Caching & Refresh Logic

#### `loadNewAd()` Method
1. Checks if user is subscribed (skips if premium)
2. Clears old cached ad
3. Sets `isLoading = true` to prevent duplicates
4. Loads new ad using Google AdMob SDK
5. On success:
   - Stores ad in `cachedAd`
   - Records `loadTimeMillis`
   - Attaches `FullScreenContentCallback`
6. On failure:
   - Logs error
   - Retries after 30 seconds using `scheduleRetry()`

#### `startAdTimer()` Method
- Starts a periodic timer that checks every 5 minutes
- Calls `checkAndRefreshExpiredAd()` on each interval

#### `checkAndRefreshExpiredAd()` Method
- Calculates ad age: `currentTime - loadTimeMillis`
- If age > 58 minutes:
  - Deletes expired ad (`cachedAd = null`)
  - Calls `loadNewAd()` to get fresh ad
- Logs remaining validity time

### ✅ Phase 3: Display & Replacement Logic

#### `showAdIfAvailable(activity: Activity)` Method
1. Checks subscription status (returns false if premium)
2. Checks if `cachedAd != null`
3. If available:
   - Calls `ad.show(activity)` to display fullscreen
   - Clears cache (`cachedAd = null`)
   - **Immediately calls `loadNewAd()`** to maintain pool
   - Returns `true`
4. If unavailable:
   - Attempts to load new ad anyway
   - Returns `false`

#### `FullScreenContentCallback` Events
- `onAdDismissedFullScreenContent()`: User closed ad (no action needed, already replaced)
- `onAdFailedToShowFullScreenContent()`: Ad failed to show → calls `loadNewAd()`
- `onAdShowedFullScreenContent()`: Logs successful display

### ✅ Phase 4: Integration

#### Cold Start Preload - `App.java` (Application class)
**Location:** `app/src/main/java/com/quran/quranaudio/online/App.java`

**Changes in `onCreate()`:**
```java
// Initialize and preload interstitial ad manager
com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().initialize(this);
com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().preloadAd();
```

**What happens:**
1. `initialize(context)` - Stores application context
2. `preloadAd()` - Calls `loadNewAd()` and `startAdTimer()`
3. First ad is loaded in background during app launch
4. Timer starts checking for expiry every 5 minutes

#### Daily Quests Exit Point - `LearningPlanSetupFragment.kt`
**Location:** `app/src/main/java/com/quran/quranaudio/online/quests/ui/LearningPlanSetupFragment.kt`

**Changes in `observeViewModel()` → `SaveStatus.Success`:**
```kotlin
// Show interstitial ad before returning to home
val adShown = InterstitialAdManager.getInstance().showAdIfAvailable(requireActivity())

if (adShown) {
    // Wait 2 seconds for ad to be dismissed before navigating
    Handler.postDelayed({ navigateBackToHome() }, 2000)
} else {
    // No ad available or user subscribed - navigate immediately
    navigateBackToHome()
}
```

**Flow:**
1. User completes Daily Quests setup (saves configuration)
2. Success toast is shown
3. After 500ms, attempts to show interstitial ad
4. If ad shown:
   - Waits 2 seconds for user to close ad
   - Navigates back to home screen
5. If no ad (premium or unavailable):
   - Navigates immediately to home screen

---

## Technical Details

### Ad Lifecycle Timeline

```
App Launch (Cold Start)
  ↓
InterstitialAdManager.initialize()
  ↓
InterstitialAdManager.preloadAd()
  ↓
loadNewAd() → Request ad from Google
  ↓
onAdLoaded() → cachedAd stored, loadTimeMillis = now
  ↓
startAdTimer() → Check every 5 minutes
  ↓
[55 minutes later] ✅ Ad still valid
  ↓
[User completes Daily Quests setup]
  ↓
showAdIfAvailable() → Show ad
  ↓
cachedAd = null, loadNewAd() called immediately
  ↓
[58 minutes after old ad loaded]
  ↓
checkAndRefreshExpiredAd() → Delete and request new ad
```

### TTL (Time To Live) Management
- **Max Age:** 58 minutes (`58 * 60 * 1000` ms)
- **Check Interval:** 5 minutes (`5 * 60 * 1000` ms)
- **Logic:** If `(currentTime - loadTimeMillis) > 58 minutes` → delete & reload

### Retry Logic
- **Delay:** 30 seconds after failure
- **Implementation:** Uses `Timer().schedule()` to retry `loadNewAd()`

### Subscription Handling
- Uses `SubscriptionChecker.isUserSubscribed(context)`
- Checked in:
  - `loadNewAd()` - Skips ad request if premium
  - `showAdIfAvailable()` - Skips ad display if premium

---

## Key Benefits

1. **Always Ready:** Preloads on app start, always has 1 ad cached
2. **Fresh Ads:** Automatically refreshes ads older than 58 minutes
3. **Instant Replacement:** New ad requested immediately when one is consumed
4. **Smart Retry:** Retries failed loads after 30 seconds
5. **Premium-Aware:** Respects subscription status (no ads for premium users)
6. **Memory Efficient:** Only caches 1 ad at a time
7. **Thread-Safe:** Singleton with synchronized initialization
8. **Robust Error Handling:** Graceful fallback on all failure scenarios

---

## Testing Checklist

### Cold Start Preload
- [ ] Launch app → Check logs for "InterstitialAdManager initialized and preload started"
- [ ] Verify ad request in logs: "Loading new interstitial ad with ID: ca-app-pub-..."
- [ ] Confirm ad loaded: "Interstitial ad loaded successfully"

### Daily Quests Exit Flow
- [ ] Navigate to Daily Quests setup (Home → Create Card or Settings icon)
- [ ] Fill out configuration and save
- [ ] Verify success toast appears
- [ ] **If not subscribed:** Interstitial ad should appear after ~500ms
- [ ] Close ad → Should navigate back to home screen
- [ ] **If subscribed:** Should navigate immediately (no ad)

### TTL Refresh (58 minutes)
- [ ] Wait 58+ minutes after app launch
- [ ] Check logs for: "Cached ad expired (age: X minutes), requesting new ad"
- [ ] Verify new ad is loaded

### Subscription Check
- [ ] Test with premium user → Ads should NOT load or show
- [ ] Logs should show: "User is subscribed, skipping ad load"

### Failure Retry
- [ ] Simulate ad load failure (e.g., no internet)
- [ ] Verify logs show: "Failed to load interstitial ad"
- [ ] Verify retry: "Retrying ad load in 30 seconds..."
- [ ] After 30s: "Retry: Loading ad after failure"

---

## File Changes Summary

| File | Type | Changes |
|------|------|---------|
| `InterstitialAdManager.kt` | **NEW** | Created singleton ad manager (300+ lines) |
| `App.java` | Modified | Added initialization & preload (2 lines in `onCreate()`) |
| `LearningPlanSetupFragment.kt` | Modified | Added ad display before navigation (15 lines) |

---

## Notes

- **Ad Unit ID:** Reuses existing `AD_INTERS` → `ca-app-pub-3966802724737141/2182661506`
- **No UI Changes:** Pure logic integration, no new layouts or resources
- **English-Only Logs:** All log messages are in English [[memory:7192069]]
- **Production Ready:** Includes error handling, retry logic, and subscription checks

---

## Future Enhancements (Optional)

1. **Analytics:** Track ad impressions, clicks, and conversion rates
2. **A/B Testing:** Test different ad display timings (e.g., 1s vs 2s delay)
3. **Multiple Ad Units:** Support different ad IDs for different exit points
4. **Configurable TTL:** Load TTL from Firebase Remote Config (e.g., 50-60 minutes)
5. **Ad Frequency Cap:** Limit ads to X per day per user

---

**Implementation Date:** December 2025  
**Status:** ✅ Complete and Ready for Testing

