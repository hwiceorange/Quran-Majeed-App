## Validation

- `:adlib:testDebugUnitTest` passed, including active, expiry-boundary, clock-rollback, and oversized-window checks.
- `:adlib:compileDebugKotlin` passed.
- `:quiz:compileDebugKotlin` passed.
- `:app:compileGoogleplayDebugKotlin` passed.
- `:app:compileGoogleplayDebugJavaWithJavac` passed.
- `:app:assembleGoogleplayDebug` passed and produced `app-googleplay-debug.apk`.
- `git diff --check` passed.
- Static interaction review confirmed that reward callbacks are additive and separate from reading, streaming, plan save, quiz exit, download cancellation, and navigation.
- English and Arabic resources are present; layouts use start/end alignment and new owned actions meet the Android 48dp minimum touch target.
- No connected ADB device was available in this environment, so physical-device visual/runtime validation remains a release-candidate check rather than a claimed automated result.
