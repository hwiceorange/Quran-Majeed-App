# Release Notes - Version 1.7.3

## 📅 Release Date
November 5, 2025

## 📦 Version Information
- **Version Code**: 65
- **Version Name**: 1.7.3
- **Build Type**: Debug
- **Target Device**: Pixel 7 (35311FDH2000QP)

---

## 🐛 Critical Bug Fixes

### 1. Google Sign-In Fixed ✅

**Problem**: 
- After adding subscription feature, Google login failed with "Sign in canceled" error
- Network DNS resolution issues causing authentication failures

**Root Causes Identified**:
1. Hardcoded Web Client ID instead of using resource reference
2. Dependency version conflicts between Billing Library 7.1.1 and older Firebase Auth
3. Missing Google Play Services availability check
4. AndroidManifest AD_SERVICES_CONFIG conflict

**Solutions Implemented**:

#### A. GoogleAuthManager Optimization
- Changed from hardcoded Web Client ID to dynamic resource reference
- Added Google Play Services availability check
- Enhanced logging for better debugging

```java
// Before
.requestIdToken("517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com")

// After
String webClientId = context.getString(R.string.default_web_client_id);
.requestIdToken(webClientId)
```

#### B. Dependency Updates
```gradle
// Firebase & Google Play Services - Updated for compatibility
implementation "com.google.firebase:firebase-auth:22.3.1"         // was 21.1.0
implementation "com.google.android.gms:play-services-auth:20.7.0" // was 20.4.0
implementation 'com.google.firebase:firebase-analytics-ktx:21.5.0'
implementation 'com.google.firebase:firebase-crashlytics:18.6.0'
implementation 'com.google.firebase:firebase-ads:22.6.0'
implementation 'com.google.firebase:firebase-messaging:23.3.1'
implementation 'com.google.firebase:firebase-firestore:24.10.0'
```

#### C. AndroidManifest Fix
```xml
<!-- Fixed AD_SERVICES_CONFIG conflict -->
<property
    android:name="android.adservices.AD_SERVICES_CONFIG"
    android:resource="@xml/gma_ad_services_config"
    tools:replace="android:resource" />
```

#### D. Enhanced Diagnostics
- Added `logSignInDiagnostics()` method to capture detailed error information
- Improved error logging in FragMain, OnboardingLoginActivity, and PrayersFragment

**Testing Results**:
```
✅ Google Sign-In now works correctly
✅ Successfully retrieved user account: adochub@gmail.com
✅ Firebase authentication successful
✅ User display name and photo loaded
✅ No conflicts with Billing Library
```

**Logs Confirmation**:
```
D/GoogleAuthManager: Google Play Services is available and up to date
D/GoogleAuthManager: GoogleSignInAccount retrieved successfully
D/GoogleAuthManager:   - Display Name: ai Dochub
D/GoogleAuthManager:   - Email: adochub@gmail.com
D/GoogleAuthManager:   - ID Token: Present
D/GoogleAuthManager: signInWithCredential:success
```

---

## 🎨 UI/UX Improvements

### 2. Prayer Log Modal Styling ✅

**Changes**: Redesigned Prayer Log Bottom Sheet to match Learning Plan Setup visual style

#### Color Palette Update
- **Primary Accent**: #429971 (consistent app green)
- **Accent Light**: #E5F3EF (light green background)
- **Text Primary**: #212121 (dark gray)
- **Text Secondary**: #6F6F6F (medium gray)

#### Typography Enhancements
- **Title**: 22sp, bold, sans-serif-medium, accent color
- **Labels**: 16sp, bold, sans-serif-medium
- **Body Text**: 15-16sp, sans-serif
- **Chips**: 13sp, secondary text

#### Visual Updates
- Segmented Control: Light green container with accent borders when selected
- Chips: Light green background (#E5F3EF) instead of white with gray strokes
- Cancel Button: Light green background with accent text and stroke
- Save Button: Solid accent green (#429971) background
- Input Field: Accent color border on focus
- Rounded corners increased to 12dp for modern look

**Before**:
```xml
<item android:state_selected="true">
    <solid android:color="#E8F5E9" />
</item>
```

**After**:
```xml
<item android:state_selected="true">
    <solid android:color="@color/prayer_modal_accent_light" />
    <stroke android:width="1dp" android:color="@color/prayer_modal_accent" />
</item>
```

### 3. Timezone Display Fix ✅

**Problem**: "Recorded At" showed UTC time, not user's local time

**Solution**: 
- Changed to display user's local timezone
- Added timezone abbreviation (e.g., GMT+08:00, PST, CST)

**Before**:
```kotlin
val dateFormat = SimpleDateFormat("h:mm a '(UTC)'", Locale.ENGLISH)
dateFormat.timeZone = TimeZone.getTimeZone("UTC")
```

**After**:
```kotlin
val userTimeZone = TimeZone.getDefault()
val dateFormat = SimpleDateFormat("h:mm a (zzz)", Locale.getDefault())
dateFormat.timeZone = userTimeZone
```

**Result**: Display now shows "8:34 PM (GMT+08:00)" instead of "12:34 PM (UTC)"

---

## 📁 Files Modified

### Core Authentication
1. `app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java`
   - Dynamic Web Client ID loading
   - Google Play Services availability check
   - Diagnostic logging method

2. `app/build.gradle`
   - Updated Firebase Auth to 22.3.1
   - Updated Play Services Auth to 20.7.0
   - Updated other Firebase dependencies

3. `app/src/main/AndroidManifest.xml`
   - Added AD_SERVICES_CONFIG property with tools:replace

### Prayer Log UI
4. `app/src/main/res/layout/bottom_sheet_log_prayer.xml`
   - Updated all colors to use new palette
   - Applied consistent typography
   - Enhanced button styling

5. `app/src/main/res/values/colors.xml`
   - Added prayer_modal_accent (#429971)
   - Added prayer_modal_accent_light (#E5F3EF)
   - Added prayer_modal_text_primary (#212121)
   - Added prayer_modal_text_secondary (#6F6F6F)

6. `app/src/main/res/drawable/selector_status_button.xml`
   - Updated selected state with accent border
   - Increased corner radius to 12dp

7. `app/src/main/res/color/selector_status_text_color.xml`
   - Updated to use prayer_modal_accent

8. `app/src/main/res/drawable/bg_segmented_control_container.xml`
   - Changed background to prayer_modal_accent_light

9. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/PrayerLogBottomSheet.kt`
   - Updated to display local time with timezone
   - Changed locale from fixed ENGLISH to user's default

### Enhanced Logging
10. `app/src/main/java/com/quran/quranaudio/online/activities/OnboardingLoginActivity.java`
11. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/home/PrayersFragment.java`
12. `app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java`
13. `app/src/main/java/com/quran/quranaudio/online/subscription/BillingManager.kt`

---

## 🔧 Technical Details

### Build Configuration
- **Gradle Plugin**: 8.3.2
- **Kotlin Version**: 1.9.0
- **Compile SDK**: 35
- **Target SDK**: 35
- **Min SDK**: 26
- **Multi-Dex**: Enabled

### Dependency Highlights
- Billing Library: 7.1.1 (for subscriptions)
- Firebase Auth: 22.3.1
- Play Services Auth: 20.7.0
- All dependencies now fully compatible

### Build Performance
- **Build Time**: ~59 seconds (incremental)
- **Tasks Executed**: 16
- **Tasks Up-to-Date**: 113
- **APK Size**: Optimized
- **Warnings**: Only deprecation warnings (non-critical)

---

## 🧪 Testing

### Google Sign-In Testing
✅ First-time user login (Onboarding)
✅ Main page avatar login
✅ Sign-out functionality
✅ Account switching
✅ Compatibility with subscription feature
✅ Network error handling

### Prayer Log UI Testing
✅ Modal displays with new color scheme
✅ Segmented control shows accent colors when selected
✅ Chips have light green background
✅ Cancel button styled correctly
✅ Save button uses primary accent
✅ Recorded At shows local time with timezone

### Device Testing
- **Device**: Pixel 7 (35311FDH2000QP)
- **Android Version**: Latest
- **Installation**: Successful
- **App Launch**: Verified
- **Google Login**: Verified successful

---

## 📊 Metrics

### Code Quality
- ✅ Build successful with zero errors
- ⚠️ 100 deprecation warnings (expected, Android API evolution)
- ✅ All critical functionality tested
- ✅ No runtime crashes detected

### User Experience
- 🎨 Consistent visual design across app
- 🌍 Proper timezone handling
- 🔐 Secure Google authentication
- 💳 Subscription functionality working alongside login

---

## 🚀 Deployment

### Installation
```bash
# Installed on device
./gradlew installDebug

# Result
Installing APK 'app-debug.apk' on 'Pixel 7 - 16'
Installed on 1 device.
BUILD SUCCESSFUL in 59s
```

### Version Verification
```bash
adb shell dumpsys package com.quran.quranaudio.online | grep version
# versionCode=65
# versionName=1.7.3
```

---

## 📝 Developer Notes

### IDE Linter Warnings
The IDE may show numerous "symbol not found" errors in the Problems panel. These are **false positives** caused by incomplete classpath indexing. The actual Gradle build completes successfully. To clear these:
- Run: `File > Sync Project with Gradle Files` in Android Studio
- Or restart the IDE
- Or simply ignore them (Gradle is the source of truth)

### Google Sign-In Requirements
For production release, ensure:
1. Release keystore SHA-1 is added to Firebase Console
2. google-services.json is up to date
3. Web Client ID matches Firebase configuration

### Network Requirements
Google Sign-In requires:
- Internet connectivity
- Access to Google services (accounts.google.com)
- For regions with restricted access, use VPN or appropriate network configuration

---

## 🔍 Diagnostic Tools

Created diagnostic scripts for troubleshooting:
1. `diagnose_google_login_v2.sh` - Comprehensive Google Sign-In diagnostics
2. Enhanced logcat filtering for authentication flows

---

## ⚡ Known Issues

1. **Deprecation Warnings**: 100 warnings about deprecated Android APIs
   - Impact: None (runtime behavior unaffected)
   - Action: Will be addressed in future refactoring

2. **IDE False Positives**: Linter shows symbol errors
   - Impact: None (build succeeds)
   - Action: Sync project with Gradle or restart IDE

---

## 📚 Documentation

New documentation created:
- `GOOGLE_LOGIN_FIX_COMPLETE.md` - Detailed fix report
- `GOOGLE_LOGIN_TEST_GUIDE.md` - Comprehensive testing guide
- `PRAYER_LOG_FEATURE_IMPLEMENTATION.md` - Feature documentation

---

## ✅ Verification Checklist

- [x] Version number incremented (64 → 65, 1.7.2 → 1.7.3)
- [x] Application builds without errors
- [x] Application installs successfully on physical device
- [x] Google Sign-In functionality verified
- [x] Prayer Log UI matches design specifications
- [x] Timezone display shows local time correctly
- [x] No runtime crashes
- [x] All new features tested
- [x] Documentation updated

---

## 🎯 Summary

Version 1.7.3 successfully fixes the critical Google Sign-In issue introduced in 1.7.2 and enhances the Prayer Log UI to match the app's visual design language. All core functionality is working correctly, and the app is ready for testing on physical devices.

**Key Achievements**:
1. ✅ Google authentication restored and enhanced
2. ✅ Billing feature compatibility ensured
3. ✅ Prayer Log UI modernized and aligned with app design
4. ✅ Timezone handling improved for better UX
5. ✅ Comprehensive diagnostics added for future troubleshooting

---

**Status**: ✅ Ready for Testing
**Next Steps**: User acceptance testing on physical device


