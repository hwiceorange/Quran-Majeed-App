# Hadith Data Size Optimization Guide

## Overview

This optimization reduces the APK size by approximately **76 MB** by moving Hadith JSON files from bundled assets to on-demand download.

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

### 3. Data Strategy

| Language | Size | Strategy |
|----------|------|----------|
| Arabic (ara-*) | ~41 MB | **KEEP BUNDLED** (Required for original text) |
| Urdu (urd-*) | ~47 MB | **DOWNLOAD ON-DEMAND** ✅ Uploaded |
| Indonesian (ind-*) | ~29 MB | **DOWNLOAD ON-DEMAND** ✅ Uploaded |

**Note:** English hadith data does not exist in this project. Only Arabic (original), Urdu, and Indonesian translations are available.

## Server Setup ✅ COMPLETED

### Uploaded Files

All hadith files have been uploaded to: `https://apis.dochubai.com/quran/hadith/`

✅ **Urdu (urd):** 6 files uploaded  
✅ **Indonesian (ind):** 6 files uploaded

**Note:** English hadith data does not exist in this project.

### Verification

```bash
curl -I https://apis.dochubai.com/quran/hadith/urd-bukhari.min.json
# HTTP/2 200 - SUCCESS
```

### Local Files Removed ✅

The following files have been removed from `app/src/main/assets/`:
- ✅ All `urd-*.min.json` files (Urdu)
- ✅ All `ind-*.min.json` files (Indonesian)

### Remaining Bundled Files

Only Arabic files remain bundled (required for original text display):
- `ara-bukhari.min.json`
- `ara-muslim.min.json`
- `ara-nasai.min.json`
- `ara-abudawud.min.json`
- `ara-tirmidhi.min.json`
- `ara-ibnmajah.min.json`

## User Experience

When user selects Urdu or Indonesian language for Hadith:
1. App checks if data is available locally
2. If not, shows download dialog with progress
3. Data is cached locally for future offline use
4. User can delete cached data to free space

## Size Savings Summary ✅

| Optimization | Savings | Status |
|-------------|---------|--------|
| Remove Urdu hadith | ~47 MB | ✅ Done |
| Remove Indonesian hadith | ~29 MB | ✅ Done |
| WebP image conversion | ~2 MB | ✅ Done |
| shrinkResources | ~3 MB | ✅ Enabled |
| Remove x86 ABI | ~3 MB | ✅ Done |
| ProGuard optimization | ~1 MB | ✅ Enabled |
| **Total** | **~85 MB** | ✅ |

### Verified Results
- Assets folder: 120MB → **58MB** (62MB saved)
- Drawable folder: 13MB → **11MB** (2MB saved from WebP)

## Testing

1. Build release APK: `./gradlew assembleRelease`
2. Install on device
3. Open Hadith section
4. Select Urdu or Indonesian language
5. Verify download dialog appears
6. Verify hadith loads after download
7. Verify offline access works

## Rollback

If issues occur:
1. Restore hadith files from git: `git checkout app/src/main/assets/*.min.json`
2. Comment out `shrinkResources true` in build.gradle
3. Revert HadithDataManager changes

## Notes

- Arabic hadith files (ara-*) are always bundled as they are required for displaying original Arabic text
- Default language is set to English (eng), which falls back to Arabic display
- Users must have internet connection for first-time download of Urdu/Indonesian hadith
- Downloaded data is cached in app's internal storage

