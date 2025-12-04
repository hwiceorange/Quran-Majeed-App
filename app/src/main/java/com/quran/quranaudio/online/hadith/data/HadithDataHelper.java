package com.quran.quranaudio.online.hadith.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java helper class for HadithDataManager
 * Provides synchronous and callback-based methods for Java code compatibility
 */
public class HadithDataHelper {
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Check if hadith data is available for the given language
     * If not, returns false and caller should show download dialog
     */
    public static boolean isHadithAvailable(@NonNull Context context, @NonNull String language, @NonNull String collection) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        return manager.isHadithAvailable(language, collection);
    }
    
    /**
     * Check if all hadith collections for a language are downloaded
     */
    public static boolean isLanguageDownloaded(@NonNull Context context, @NonNull String language) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        return manager.isLanguageFullyDownloaded(language);
    }
    
    /**
     * Get hadith data synchronously
     * Returns null if data is not available (not bundled and not downloaded)
     */
    @Nullable
    public static String getHadithData(@NonNull Context context, @NonNull String language, @NonNull String collection) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        return manager.getHadithDataSync(language, collection);
    }
    
    /**
     * Check if the language requires download (not bundled with app)
     */
    public static boolean requiresDownload(@NonNull String language) {
        return HadithDataManager.Companion.getDOWNLOADABLE_LANGUAGES().contains(language);
    }
    
    /**
     * Check if the language is bundled with the app
     */
    public static boolean isBundled(@NonNull String language) {
        return HadithDataManager.Companion.getBUNDLED_LANGUAGES().contains(language);
    }
    
    /**
     * Download hadith data for a language asynchronously
     */
    public static void downloadLanguage(
            @NonNull Context context,
            @NonNull String language,
            @Nullable ProgressCallback progressCallback,
            @Nullable CompletionCallback completionCallback
    ) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        
        executor.execute(() -> {
            try {
                // Download each collection one by one
                int totalCollections = HadithDataManager.Companion.getHADITH_COLLECTIONS().size();
                int completedCount = 0;
                
                for (String collection : HadithDataManager.Companion.getHADITH_COLLECTIONS()) {
                    // Check if already downloaded
                    if (manager.isHadithAvailable(language, collection)) {
                        completedCount++;
                        final int progress = (completedCount * 100) / totalCollections;
                        if (progressCallback != null) {
                            mainHandler.post(() -> progressCallback.onProgress(progress));
                        }
                        continue;
                    }
                    
                    // Download synchronously in background thread
                    // Using a simple blocking approach for Java compatibility
                    boolean success = downloadCollectionBlocking(context, language, collection);
                    
                    if (!success) {
                        if (completionCallback != null) {
                            mainHandler.post(() -> completionCallback.onComplete(false));
                        }
                        return;
                    }
                    
                    completedCount++;
                    final int progress = (completedCount * 100) / totalCollections;
                    if (progressCallback != null) {
                        mainHandler.post(() -> progressCallback.onProgress(progress));
                    }
                }
                
                if (completionCallback != null) {
                    mainHandler.post(() -> completionCallback.onComplete(true));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (completionCallback != null) {
                    mainHandler.post(() -> completionCallback.onComplete(false));
                }
            }
        });
    }
    
    private static boolean downloadCollectionBlocking(Context context, String language, String collection) {
        try {
            HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
            // Use runBlocking equivalent - simple thread.join approach
            final boolean[] result = {false};
            Thread downloadThread = new Thread(() -> {
                try {
                    // Simple HTTP download
                    String url = "https://apis.dochubai.com/quran/hadith/" + language + "-" + collection + ".json";
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    
                    if (connection.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
                        java.io.File hadithDir = new java.io.File(context.getFilesDir(), "hadith_data");
                        if (!hadithDir.exists()) hadithDir.mkdirs();
                        
                        java.io.File destFile = new java.io.File(hadithDir, language + "-" + collection + ".json");
                        try (java.io.InputStream in = connection.getInputStream();
                             java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                        result[0] = true;
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            downloadThread.start();
            downloadThread.join();
            return result[0];
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete downloaded hadith data for a language
     */
    public static boolean deleteLanguage(@NonNull Context context, @NonNull String language) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        return manager.deleteLanguage(language);
    }
    
    /**
     * Get the download progress for a language (0-100)
     */
    public static int getDownloadProgress(@NonNull Context context, @NonNull String language) {
        HadithDataManager manager = HadithDataManager.Companion.getInstance(context);
        return manager.getLanguageDownloadProgress(language);
    }
    
    public interface ProgressCallback {
        void onProgress(int progress);
    }
    
    public interface CompletionCallback {
        void onComplete(boolean success);
    }
}

