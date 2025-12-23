# Hadith Data Size Optimization Guide (v2.0)

## Overview

This optimization reduces the APK size by approximately **39 MB** additional savings by moving **ALL** Hadith JSON files (including Arabic) from bundled assets to on-demand download.

**Total savings: ~76 MB from original 83MB → Target: ~30-35 MB**

## Changes Made

### 1. Build Configuration Optimizations (`app/build.gradle`)
- ✅ Enabled `shrinkResources true` for release builds
- ✅ Removed x86/x86_64 ABI support (only needed for emulators)
- ✅ Fixed duplicate dependency declarations
- ✅ Updated ProGuard to enable code optimization

### 2. Hadith Data Management System

Created a new on-demand download system:
- `HadithDataManager.kt` - Core manager for downloading and caching hadith data
- `HadithDataHelper.java` - Java helper for compatibility
- `HadithDownloadDialog.java` - UI for download progress

### 3. Data Strategy (v2.0 - All On-Demand)

| Language | Size | Strategy |
|----------|------|----------|
| Arabic (ara-*) | ~39 MB | **🆕 DOWNLOAD ON-DEMAND** (auto-download on entering Hadith module) |
| Urdu (urd-*) | ~47 MB | **DOWNLOAD ON-DEMAND** ✅ Uploaded |
| Indonesian (ind-*) | ~29 MB | **DOWNLOAD ON-DEMAND** ✅ Uploaded |

**Note:** English hadith data does not exist in this project.

## Server Setup

### Step 1: Upload Arabic Hadith Files (🆕 Required)

**Please upload these files to: `https://apis.dochubai.com/quran/hadith/`**

From `app/src/main/assets/`:
- `ara-abudawud.min.json` (6.3MB)
- `ara-bukhari.min.json` (8.5MB)
- `ara-ibnmajah.min.json` (4.8MB)
- `ara-muslim.min.json` (7.8MB)
- `ara-nasai.min.json` (6.2MB)
- `ara-tirmidhi.min.json` (6.2MB)

### Step 2: Verify Upload

```bash
curl -I https://apis.dochubai.com/quran/hadith/ara-bukhari.min.json
curl -I https://apis.dochubai.com/quran/hadith/ara-muslim.min.json
curl -I https://apis.dochubai.com/quran/hadith/ara-nasai.min.json
curl -I https://apis.dochubai.com/quran/hadith/ara-abudawud.min.json
curl -I https://apis.dochubai.com/quran/hadith/ara-tirmidhi.min.json
curl -I https://apis.dochubai.com/quran/hadith/ara-ibnmajah.min.json
```

Expected: `HTTP/2 200`

### Step 3: Delete Local Arabic Files

**⚠️ Only after successful upload verification!**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
rm app/src/main/assets/ara-*.min.json
```

### Step 4: Rebuild

```bash
./gradlew clean assembleRelease
```

## Previously Uploaded (Completed ✅)

✅ **Urdu (urd):** 6 files uploaded  
✅ **Indonesian (ind):** 6 files uploaded

## User Experience

### First Time Entering Hadith Module
1. User taps on Hadith section
2. App detects Arabic data not available
3. Download dialog appears automatically
4. Progress shows download status
5. Once complete, Hadith content displays normally

### Selecting Other Languages
1. User selects Urdu or Indonesian
2. If not downloaded, shows download dialog
3. After download, translation displays

## Size Savings Summary

| Optimization | Savings | Status |
|-------------|---------|--------|
| Remove Arabic hadith | **~39 MB** | 🆕 Ready to implement |
| Remove Urdu hadith | ~47 MB | ✅ Done |
| Remove Indonesian hadith | ~29 MB | ✅ Done |
| WebP image conversion | ~2 MB | ✅ Done |
| shrinkResources | ~3 MB | ✅ Enabled |
| Remove x86 ABI | ~3 MB | ✅ Done |
| ProGuard optimization | ~1 MB | ✅ Enabled |
| **Total** | **~124 MB** | - |

### Expected Results
- **Current APK size:** 71 MB
- **After Arabic removal:** ~30-35 MB

## Testing

1. Build release APK: `./gradlew assembleRelease`
2. Install on device
3. Open Hadith section
4. **Verify Arabic download dialog appears automatically**
5. Verify hadith loads after download
6. Select Urdu or Indonesian language
7. Verify download dialog appears for translation
8. Verify offline access works after download

## Code Changes Summary

### HadithDataManager.kt
- Changed `BUNDLED_LANGUAGES` from `setOf("ara")` to `emptySet()`
- Added `"ara"` to `DOWNLOADABLE_LANGUAGES`
- Added `ensureArabicAvailable()` method

### HadithActivity.java
- Added `checkAndDownloadArabicHadith()` method
- Auto-downloads Arabic data when entering Hadith module

## Rollback

If issues occur:
1. Restore hadith files from git: `git checkout app/src/main/assets/ara-*.min.json`
2. Revert HadithDataManager.kt changes (restore "ara" to BUNDLED_LANGUAGES)
3. Revert HadithActivity.java changes
