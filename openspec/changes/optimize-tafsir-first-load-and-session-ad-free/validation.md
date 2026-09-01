## Validation

- `git diff --check`: passed
- `openspec validate optimize-tafsir-first-load-and-session-ad-free --strict`: passed
- `:app:testGoogleplayDebugUnitTest`: 4 tests passed
- `:adlib:testDebugUnitTest`: 4 tests passed
- `:adlib:compileDebugKotlin`: passed
- `:app:compileGoogleplayDebugKotlin`: passed
- `:app:compileGoogleplayDebugJavaWithJavac`: passed
- `:app:assembleGoogleplayDebug`: passed

## Static interaction review

- Both owned removal actions are outside `NativeAdView`, appear only after their native ad is displayed, and retain a 48dp semantic/touch height with 8dp vertical background insets and unchanged 14sp text.
- The initial action opens Premium; the explicit `AD` alternative appears only after returning without an entitlement.
- Cancel, timeout, no-fill, show failure, and early close do not activate the process entitlement.
- The independent Tafsir and home entitlements are process memory only and suppress only their scoped native ad without changing reading, home, or navigation state.
- `start/end` layout, wrap-content height, semantic theme colors, and English/Arabic resources cover RTL, large text, dark theme, and compact layouts.
- Current and adjacent content work is asynchronous; stale activity requests are ignored and native loading starts only after WebView content finishes.

## Pixel 7 runtime checks

- Installed the generated APK with `adb install -r`, preserving application data.
- Cold-launched through `SplashScreenActivity`; the final focused activity was `MainActivity`, with no fatal exception or ANR.
- Scrolled to the actual `FragMain` VOTD-bottom native placement: a Google test native ad filled, the AdMob native ad validator reported `No implementation issues found`, and `Remove ads · VIP` rendered directly above it.
- UI Automator reported the action as a clickable semantic button with bounds `[21,1388][1059,1514]`, which is 48dp at the device density; the action text and content description matched.
- Did not click the owned entry, purchase UI, native ad, or rewarded ad during the non-mutating runtime review.

## Manual production-flow checks still required

- Billing return with both subscribed and non-subscribed accounts.
- Rewarded-ad cancel, no-fill, early-close, and earned callbacks with live provider responses.
- Visual review on a small phone, landscape, Arabic RTL, dark theme, and large font scale; static resource/constraint review passed for these states.
- Cold/poor-network timing comparison using the `tafsir_load_performance` event.
