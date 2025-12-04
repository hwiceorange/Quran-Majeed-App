package com.quran.quranaudio.online.hadith.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Hadith Data Manager - Handles on-demand downloading of Hadith collections
 * 
 * Strategy:
 * - ALL hadith data (Arabic, Urdu, Indonesian) is downloaded on first use
 * - Arabic is required and will be auto-downloaded when entering Hadith module
 * - Other languages are downloaded when user selects them
 * 
 * This significantly reduces APK size by ~70MB while maintaining full functionality.
 */
class HadithDataManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "HadithDataManager"
        
        // Server URL for hadith data
        private const val HADITH_BASE_URL = "https://apis.dochubai.com/quran/hadith/"
        
        // Hadith collections
        val HADITH_COLLECTIONS = listOf(
            "bukhari.min",
            "muslim.min",
            "nasai.min",
            "abudawud.min",
            "tirmidhi.min",
            "ibnmajah.min"
        )
        
        // No languages bundled with app - all downloaded on demand
        val BUNDLED_LANGUAGES = emptySet<String>()
        
        // All languages available for download from server
        // ara (Arabic) - required, auto-downloaded when entering Hadith module
        // urd (Urdu), ind (Indonesian) - downloaded when user selects
        val DOWNLOADABLE_LANGUAGES = setOf("ara", "urd", "ind")
        
        // Arabic is the base language required for Hadith display
        const val BASE_LANGUAGE = "ara"
        
        @Volatile
        private var instance: HadithDataManager? = null
        
        fun getInstance(context: Context): HadithDataManager {
            return instance ?: synchronized(this) {
                instance ?: HadithDataManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val hadithDir: File by lazy {
        File(context.filesDir, "hadith_data").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Check if hadith data for a specific language and collection is available
     */
    fun isHadithAvailable(language: String, collection: String): Boolean {
        // Bundled languages are always available
        if (language in BUNDLED_LANGUAGES) {
            return true
        }
        
        // Check if downloaded
        val fileName = "$language-$collection.json"
        return File(hadithDir, fileName).exists()
    }
    
    /**
     * Check if all hadith collections for a language are downloaded
     */
    fun isLanguageFullyDownloaded(language: String): Boolean {
        if (language in BUNDLED_LANGUAGES) return true
        
        return HADITH_COLLECTIONS.all { collection ->
            File(hadithDir, "$language-$collection.json").exists()
        }
    }
    
    /**
     * Get the download progress for a language (0-100)
     */
    fun getLanguageDownloadProgress(language: String): Int {
        if (language in BUNDLED_LANGUAGES) return 100
        
        val downloaded = HADITH_COLLECTIONS.count { collection ->
            File(hadithDir, "$language-$collection.json").exists()
        }
        return (downloaded * 100) / HADITH_COLLECTIONS.size
    }
    
    /**
     * Get hadith data as a string
     * - For bundled languages: reads from assets
     * - For downloaded languages: reads from local storage
     */
    suspend fun getHadithData(language: String, collection: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$language-$collection.json"
                
                // For bundled languages, read from assets
                if (language in BUNDLED_LANGUAGES) {
                    return@withContext readFromAssets(fileName)
                }
                
                // For other languages, check local cache
                val localFile = File(hadithDir, fileName)
                if (localFile.exists()) {
                    return@withContext localFile.readText()
                }
                
                // Not available - need to download first
                Log.w(TAG, "Hadith data not available: $fileName. Please download first.")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error reading hadith data: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Get hadith data synchronously (for Java compatibility)
     */
    fun getHadithDataSync(language: String, collection: String): String? {
        return try {
            val fileName = "$language-$collection.json"
            
            // For bundled languages, read from assets
            if (language in BUNDLED_LANGUAGES) {
                return readFromAssets(fileName)
            }
            
            // For other languages, check local cache
            val localFile = File(hadithDir, fileName)
            if (localFile.exists()) {
                return localFile.readText()
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading hadith data: ${e.message}", e)
            null
        }
    }
    
    private fun readFromAssets(fileName: String): String? {
        return try {
            IOUtils.toString(context.assets.open(fileName), "UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from assets: $fileName", e)
            null
        }
    }
    
    /**
     * Download hadith data for a specific language
     * @param language The language code (e.g., "urd", "ind")
     * @param progressCallback Optional callback for download progress (0-100)
     * @return true if download was successful
     */
    suspend fun downloadLanguage(
        language: String,
        progressCallback: ((Int) -> Unit)? = null
    ): Boolean {
        if (language in BUNDLED_LANGUAGES) {
            progressCallback?.invoke(100)
            return true // Already bundled
        }
        
        return withContext(Dispatchers.IO) {
            try {
                var completedCount = 0
                
                for (collection in HADITH_COLLECTIONS) {
                    val fileName = "$language-$collection.json"
                    val localFile = File(hadithDir, fileName)
                    
                    // Skip if already downloaded
                    if (localFile.exists()) {
                        completedCount++
                        progressCallback?.invoke((completedCount * 100) / HADITH_COLLECTIONS.size)
                        continue
                    }
                    
                    // Download from server
                    val url = "$HADITH_BASE_URL$fileName"
                    val success = downloadFile(url, localFile)
                    
                    if (!success) {
                        Log.e(TAG, "Failed to download: $fileName")
                        return@withContext false
                    }
                    
                    completedCount++
                    progressCallback?.invoke((completedCount * 100) / HADITH_COLLECTIONS.size)
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading hadith language: $language", e)
                false
            }
        }
    }
    
    /**
     * Download a single hadith collection
     */
    suspend fun downloadCollection(language: String, collection: String): Boolean {
        if (language in BUNDLED_LANGUAGES) return true
        
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$language-$collection.json"
                val localFile = File(hadithDir, fileName)
                
                if (localFile.exists()) return@withContext true
                
                val url = "$HADITH_BASE_URL$fileName"
                downloadFile(url, localFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading collection: $collection", e)
                false
            }
        }
    }
    
    private fun downloadFile(urlString: String, destFile: File): Boolean {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Downloaded: ${destFile.name}")
                return true
            } else {
                Log.e(TAG, "HTTP error: ${connection.responseCode} for $urlString")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            destFile.delete() // Clean up partial download
            return false
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Delete downloaded hadith data for a language (to free up space)
     */
    fun deleteLanguage(language: String): Boolean {
        if (language in BUNDLED_LANGUAGES) return false // Cannot delete bundled data
        
        var allDeleted = true
        for (collection in HADITH_COLLECTIONS) {
            val file = File(hadithDir, "$language-$collection.json")
            if (file.exists() && !file.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }
    
    /**
     * Get total size of downloaded hadith data in bytes
     */
    fun getDownloadedDataSize(): Long {
        return hadithDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Get available languages that can be downloaded
     * Note: English hadith data does not exist - only Arabic, Urdu, and Indonesian are available
     */
    fun getAvailableLanguages(): List<LanguageInfo> {
        return listOf(
            LanguageInfo("ara", "Arabic", isLanguageFullyDownloaded("ara"), getLanguageDownloadProgress("ara")),
            LanguageInfo("urd", "Urdu", isLanguageFullyDownloaded("urd"), getLanguageDownloadProgress("urd")),
            LanguageInfo("ind", "Indonesian", isLanguageFullyDownloaded("ind"), getLanguageDownloadProgress("ind"))
        )
    }
    
    /**
     * Check if Arabic (base language) is available
     * Arabic is required for displaying original Hadith text
     */
    fun isArabicAvailable(): Boolean {
        return isLanguageFullyDownloaded(BASE_LANGUAGE)
    }
    
    /**
     * Download Arabic hadith data (required for all Hadith functionality)
     */
    suspend fun ensureArabicAvailable(progressCallback: ((Int) -> Unit)? = null): Boolean {
        if (isArabicAvailable()) {
            progressCallback?.invoke(100)
            return true
        }
        return downloadLanguage(BASE_LANGUAGE, progressCallback)
    }
    
    data class LanguageInfo(
        val code: String,
        val name: String,
        val isDownloaded: Boolean,
        val downloadProgress: Int
    )
}

