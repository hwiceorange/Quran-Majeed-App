package com.bible.tools.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.drawToBitmap
import com.bible.tools.extension.logdt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception


object ShareUtil {
    private val TAG = "ShareUtil"
    suspend fun share(view: View) {
        if (!canDrawBitmap(view)) {
            return
        }
        val bitmap = view.drawToBitmap(Bitmap.Config.RGB_565)
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            logdt("coroutine: error ${throwable.message}", TAG)
        }
        withContext(Dispatchers.IO + exceptionHandler) {
            val file = writeBitmap(view.context, bitmap)
            delay(500)
            shareBitmap(view.context, file)
        }
    }

    private fun canDrawBitmap(view: View): Boolean {
        if (!ViewCompat.isLaidOut(view)) {
            logdt("canDrawBitmap: View needs to be laid out before calling drawToBitmap()", TAG)
            return false
        }
        if (view.width <= 0 || view.height <= 0) {
            logdt("canDrawBitmap: View width and height must >0", TAG)
            return false
        }
        return true
    }

    private fun writeBitmap(context: Context, bitmap: Bitmap): File {
        //---Save bitmap to external cache directory---//
        //get cache directory
        val cachePath = File(context.externalCacheDir, "share_images/")
        cachePath.mkdirs()

        //create png file
        val file = File(cachePath, "share_image.png")
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    private fun shareBitmap(context: Context, file: File) {
        //---Share File---//
        //get file uri
        try {
            val myImageFileUri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            //create a intent
            val intent = Intent(Intent.ACTION_SEND)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_STREAM, myImageFileUri)
            intent.type = "image/png"
            context.startActivity(Intent.createChooser(intent, "Share with"))
        } catch (e: Exception) {
            e.printStackTrace()
           // FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}