package de.markusfisch.android.shadereditor.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.io.IOException
import java.io.InputStream

object BitmapEditor {

    @JvmStatic
    fun getBitmapFromUri(
        context: Context, uri: Uri, maxSize: Int
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    setSampleSize(this, inputStream, maxSize, maxSize)
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    @JvmStatic
    fun crop(
        bitmap: Bitmap?, rect: RectF, rotation: Float
    ): Bitmap? {
        if (bitmap == null) return null

        return try {
            val rotatedBitmap = if (rotation % 360f != 0f) {
                val matrix = Matrix().apply {
                    setRotate(rotation)
                }
                Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
            } else {
                bitmap
            }

            val w = rotatedBitmap.width
            val h = rotatedBitmap.height

            Bitmap.createBitmap(
                rotatedBitmap,
                (rect.left * w).toInt(),
                (rect.top * h).toInt(),
                (rect.width() * w).toInt(),
                (rect.height() * h).toInt()
            )
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun setSampleSize(
        options: BitmapFactory.Options, inputStream: InputStream, maxWidth: Int, maxHeight: Int
    ) {
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateSampleSize(
            options.outWidth, options.outHeight, maxWidth, maxHeight
        )
    }

    private fun calculateSampleSize(
        width: Int, height: Int, maxWidth: Int, maxHeight: Int
    ): Int {
        var size = 1

        if (width > maxWidth || height > maxHeight) {
            val hw = width / 2
            val hh = height / 2

            while (hw / size > maxWidth && hh / size > maxHeight) {
                size *= 2
            }
        }

        return size
    }
}