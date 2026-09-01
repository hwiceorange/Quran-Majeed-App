## Validation

- `git diff --check`: passed
- `openspec validate optimize-tafsir-first-load-and-session-ad-free --strict`: passed
- `:app:testGoogleplayDebugUnitTest`: 2 tests passed
- `:adlib:testDebugUnitTest`: 4 tests passed
- `:adlib:compileDebugKotlin`: passed
- `:app:compileGoogleplayDebugKotlin`: passed
- `:app:compileGoogleplayDebugJavaWithJavac`: passed
- `:app:assembleGoogleplayDebug`: passed

## Static interaction review

- The owned removal action is outside `NativeAdView`, appears only after an ad is displayed, and is at least 48dp high.
- The initial action opens Premium; the explicit `AD` alternative appears only after returning without an entitlement.
- Cancel, timeout, no-fill, show failure, and early close do not activate the process entitlement.
- The entitlement is process memory only and suppresses Tafsir native requests without changing reading/navigation state.
- `start/end` layout, wrap-content height, semantic theme colors, and English/Arabic resources cover RTL, large text, dark theme, and compact layouts.
- Current and adjacent content work is asynchronous; stale activity requests are ignored and native loading starts only after WebView content finishes.

## Manual device checks still required

- Real AdMob fill and earned callback behavior.
- Billing return with both subscribed and non-subscribed accounts.
- Visual review on a small phone, landscape, Arabic RTL, dark theme, and large font scale.
- Cold/poor-network timing comparison using the `tafsir_load_performance` event.
