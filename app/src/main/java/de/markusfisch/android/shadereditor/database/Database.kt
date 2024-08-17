package de.markusfisch.android.shadereditor.database

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.Build
import android.widget.Toast
import de.markusfisch.android.shadereditor.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Database {
    private lateinit var db: SQLiteDatabase
    private var textureThumbnailSize: Int = 0

    fun importDatabase(context: Context, fileName: String): String? {
        var edb: SQLiteDatabase? = null
        return try {
            edb = ImportHelper(ExternalDatabaseContext(context), fileName).readableDatabase
            db.beginTransaction()
            if (importShaders(db, edb) && importTextures(db, edb)) {
                db.setTransactionSuccessful()
                null
            } else {
                context.getString(R.string.import_failed, "")
            }
        } catch (e: SQLException) {
            e.message
        } finally {
            if (db.inTransaction()) db.endTransaction()
            edb?.close()
        }
    }

    fun open(context: Context) {
        textureThumbnailSize = (context.resources.displayMetrics.density * 48f).toInt()
        try {
            db = OpenHelper(context).writableDatabase
        } catch (e: SQLException) {
            Toast.makeText(context, R.string.cannot_open_database, Toast.LENGTH_LONG).show()
        }
    }

    val isOpen: Boolean
        get() = ::db.isInitialized

    val shaders: Cursor
        get() = getShaders(false)

    fun getShaders(sortByLastModification: Boolean): Cursor {
        return db.rawQuery(
            "SELECT $SHADERS_ID,$SHADERS_THUMB,$SHADERS_NAME,$SHADERS_MODIFIED FROM $SHADERS ORDER BY " + (if (sortByLastModification) "$SHADERS_MODIFIED DESC"
            else SHADERS_ID), null
        )
    }

    fun getTextures(substring: String?): Cursor {
        val useSubstring = substring != null
        return db.rawQuery(
            "SELECT $TEXTURES_ID,$TEXTURES_NAME,$TEXTURES_WIDTH,$TEXTURES_HEIGHT,$TEXTURES_THUMB FROM $TEXTURES WHERE $TEXTURES_RATIO = 1" + (if (useSubstring) " AND $TEXTURES_NAME LIKE ?"
            else "") + " ORDER BY " + TEXTURES_ID, if (useSubstring) arrayOf("%$substring%")
            else null
        )
    }

    fun getSamplerCubeTextures(substring: String?): Cursor {
        val useSubstring = substring != null
        return db.rawQuery(
            "SELECT $TEXTURES_ID,$TEXTURES_NAME,$TEXTURES_WIDTH,$TEXTURES_HEIGHT,$TEXTURES_THUMB FROM $TEXTURES WHERE $TEXTURES_RATIO = 1.5" + (if (useSubstring) " AND $TEXTURES_NAME LIKE ?"
            else "") + " ORDER BY " + TEXTURES_ID, if (useSubstring) arrayOf("%$substring%")
            else null
        )
    }

    fun isShaderAvailable(id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT $SHADERS_ID FROM $SHADERS WHERE $SHADERS_ID = ?", arrayOf(id.toString())
        )
        if (cursor == null) {
            return false
        }
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getShader(id: Long): Cursor {
        return db.rawQuery(
            "SELECT $SHADERS_ID,$SHADERS_FRAGMENT_SHADER,$SHADERS_NAME,$SHADERS_MODIFIED,$SHADERS_QUALITY FROM $SHADERS WHERE $SHADERS_ID = ?",
            arrayOf(id.toString())
        )
    }

    val randomShader: Cursor
        get() = db.rawQuery(
            "SELECT $SHADERS_ID,$SHADERS_FRAGMENT_SHADER,$SHADERS_MODIFIED,$SHADERS_QUALITY FROM $SHADERS ORDER BY RANDOM() LIMIT 1",
            null
        )

    fun getThumbnail(id: Long): ByteArray? {
        val cursor = db.rawQuery(
            "SELECT $SHADERS_THUMB FROM $SHADERS WHERE $SHADERS_ID = ?", arrayOf(id.toString())
        )

        if (closeIfEmpty(cursor)) {
            return null
        }

        val thumbnail = getBlob(cursor, SHADERS_THUMB)
        cursor.close()

        return thumbnail
    }

    val firstShaderId: Long
        get() {
            val cursor = db.rawQuery(
                "SELECT $SHADERS_ID FROM $SHADERS ORDER BY $SHADERS_ID LIMIT 1", null
            )

            if (closeIfEmpty(cursor)) {
                return 0
            }

            val id = getLong(cursor, SHADERS_ID)
            cursor.close()

            return id
        }

    fun getTexture(id: Long): Cursor {
        return db.rawQuery(
            "SELECT $TEXTURES_NAME,$TEXTURES_WIDTH,$TEXTURES_HEIGHT,$TEXTURES_MATRIX FROM $TEXTURES WHERE $TEXTURES_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun getTextureBitmap(name: String): Bitmap? {
        val cursor = db.rawQuery(
            "SELECT $TEXTURES_MATRIX FROM $TEXTURES WHERE $TEXTURES_NAME = ?", arrayOf(name)
        )
        val bm = getTextureBitmap(cursor)
        cursor.close()
        return bm
    }

    fun getTextureBitmap(cursor: Cursor): Bitmap? {
        return if (closeIfEmpty(cursor)) null
        else textureFromCursor(cursor)
    }

    fun insertShader(
        shader: String?, thumbnail: ByteArray?, quality: Float
    ): Long {
        return insertShader(db, shader, null, thumbnail, quality)
    }

    fun insertShader(
        context: Context, shader: String?, name: String?
    ): Long {
        return insertShader(
            db, shader, name, loadBitmapResource(context, R.drawable.thumbnail_new_shader), 1f
        )
    }

    fun insertNewShader(context: Context): Long {
        return insertShaderFromResource(
            context, null, R.raw.new_shader, R.drawable.thumbnail_new_shader, 1f
        )
    }

    fun insertShaderFromResource(
        context: Context, name: String?, sourceId: Int, thumbId: Int, quality: Float
    ): Long {
        return try {
            insertShader(
                db,
                loadRawResource(context, sourceId),
                name,
                loadBitmapResource(context, thumbId),
                quality
            )
        } catch (e: IOException) {
            0
        }
    }

    fun insertTexture(name: String?, bitmap: Bitmap): Long {
        return insertTexture(
            db, name, bitmap, textureThumbnailSize
        )
    }

    fun updateShader(
        id: Long, shader: String?, thumbnail: ByteArray?, quality: Float
    ) {
        val cv = ContentValues()
        cv.put(SHADERS_FRAGMENT_SHADER, shader)
        cv.put(SHADERS_MODIFIED, currentTime())
        cv.put(SHADERS_QUALITY, quality)

        if (thumbnail != null) {
            cv.put(SHADERS_THUMB, thumbnail)
        }

        db.update(
            SHADERS, cv, "$SHADERS_ID = ?", arrayOf(id.toString())
        )
    }

    fun updateShaderQuality(
        id: Long, quality: Float
    ) {
        val cv = ContentValues()
        cv.put(SHADERS_QUALITY, quality)

        db.update(
            SHADERS, cv, "$SHADERS_ID = ?", arrayOf(id.toString())
        )
    }

    fun updateShaderName(id: Long, name: String?) {
        val cv = ContentValues()
        cv.put(SHADERS_NAME, name)
        db.update(
            SHADERS, cv, "$SHADERS_ID = ?", arrayOf(id.toString())
        )
    }

    fun removeShader(id: Long) {
        db.delete(
            SHADERS, "$SHADERS_ID = ?", arrayOf(id.toString())
        )
    }

    fun removeTexture(id: Long) {
        db.delete(
            TEXTURES, "$TEXTURES_ID = ?", arrayOf(id.toString())
        )
    }

    private fun createShadersTable(db: SQLiteDatabase, context: Context) {
        db.execSQL("DROP TABLE IF EXISTS $SHADERS")
        db.execSQL(
            "CREATE TABLE $SHADERS ($SHADERS_ID INTEGER PRIMARY KEY AUTOINCREMENT,$SHADERS_FRAGMENT_SHADER TEXT NOT NULL,$SHADERS_THUMB BLOB,$SHADERS_NAME TEXT,$SHADERS_CREATED DATETIME,$SHADERS_MODIFIED DATETIME,$SHADERS_QUALITY REAL);"
        )

        insertInitalShaders(db, context)
    }

    private fun insertInitalShaders(db: SQLiteDatabase, context: Context) {
        try {
            insertShader(
                db, loadRawResource(
                    context, R.raw.default_shader
                ), context.getString(R.string.default_shader), loadBitmapResource(
                    context, R.drawable.thumbnail_default
                ), 1f
            )
        } catch (e: IOException) {
            // Shouldn't ever happen in production
            // and nothing can be done if it does.
        }
    }

    private fun createTexturesTable(db: SQLiteDatabase, context: Context) {
        db.execSQL("DROP TABLE IF EXISTS $TEXTURES")
        db.execSQL(
            "CREATE TABLE $TEXTURES ($TEXTURES_ID INTEGER PRIMARY KEY AUTOINCREMENT,$TEXTURES_NAME TEXT NOT NULL UNIQUE,$TEXTURES_WIDTH INTEGER,$TEXTURES_HEIGHT INTEGER,$TEXTURES_RATIO REAL,$TEXTURES_THUMB BLOB,$TEXTURES_MATRIX BLOB);"
        )

        insertInitalTextures(db, context)
    }

    private fun insertInitalTextures(db: SQLiteDatabase, context: Context) {
        insertTexture(
            db, context.getString(R.string.texture_name_noise), BitmapFactory.decodeResource(
                context.resources, R.drawable.texture_noise
            ), textureThumbnailSize
        )
    }

    private inner class OpenHelper(private val context: Context) :
        SQLiteOpenHelper(context, FILE_NAME, null, 5) {
        override fun onCreate(db: SQLiteDatabase) {
            createShadersTable(db, context)
            createTexturesTable(db, context)
        }

        override fun onDowngrade(
            db: SQLiteDatabase, oldVersion: Int, newVersion: Int
        ) {
            // Without onDowngrade(), a downgrade will throw
            // an exception. Can never happen in production.
        }

        override fun onUpgrade(
            db: SQLiteDatabase, oldVersion: Int, newVersion: Int
        ) {
            if (oldVersion < 2) {
                createTexturesTable(db, context)
                insertInitalShaders(db, context)
            }

            if (oldVersion < 3) {
                addShadersQuality(db)
            }

            if (oldVersion < 4) {
                addTexturesWidthHeightRatio(db)
            }

            if (oldVersion < 5) {
                addShaderNames(db)
            }
        }
    }

    private class ImportHelper(context: Context, path: String) :
        SQLiteOpenHelper(context, path, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            // Do nothing.
        }

        override fun onDowngrade(
            db: SQLiteDatabase, oldVersion: Int, newVersion: Int
        ) {
            // Do nothing, but without that method we cannot open
            // different versions.
        }

        override fun onUpgrade(
            db: SQLiteDatabase, oldVersion: Int, newVersion: Int
        ) {
            // Do nothing, but without that method we cannot open
            // different versions.
        }
    }

    // Somehow it's required to use this ContextWrapper to access the
    // tables in an external database. Without this, the database will
    // only contain the table "android_metadata".
    class ExternalDatabaseContext(base: Context?) : ContextWrapper(base) {
        override fun getDatabasePath(name: String): File {
            return File(filesDir, name)
        }

        override fun openOrCreateDatabase(
            name: String, mode: Int, factory: CursorFactory, errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase {
            return openOrCreateDatabase(name, mode, factory)
        }

        override fun openOrCreateDatabase(
            name: String, mode: Int, factory: CursorFactory
        ): SQLiteDatabase {
            return SQLiteDatabase.openOrCreateDatabase(
                getDatabasePath(name), null
            )
        }
    }

    companion object {
        const val FILE_NAME = "shaders.db"

        const val SHADERS = "shaders"
        const val SHADERS_ID = "_id"
        const val SHADERS_FRAGMENT_SHADER = "shader"
        const val SHADERS_THUMB = "thumb"
        const val SHADERS_NAME = "name"
        const val SHADERS_CREATED = "created"
        const val SHADERS_MODIFIED = "modified"
        const val SHADERS_QUALITY = "quality"

        const val TEXTURES = "textures"
        const val TEXTURES_ID = "_id"
        const val TEXTURES_NAME = "name"
        const val TEXTURES_WIDTH = "width"
        const val TEXTURES_HEIGHT = "height"
        const val TEXTURES_RATIO = "ratio"
        const val TEXTURES_THUMB = "thumb"
        const val TEXTURES_MATRIX = "matrix"

        @JvmStatic
        fun getLong(cursor: Cursor, column: String?): Long {
            val index = cursor.getColumnIndex(column)
            if (index < 0) {
                return 0L
            }
            return cursor.getLong(index)
        }

        @JvmStatic
        fun getFloat(cursor: Cursor, column: String?): Float {
            val index = cursor.getColumnIndex(column)
            if (index < 0) {
                return 0f
            }
            return cursor.getFloat(index)
        }

        @JvmStatic
        fun getString(cursor: Cursor, column: String?): String {
            val index = cursor.getColumnIndex(column)
            if (index < 0) {
                return ""
            }
            return cursor.getString(index)
        }

        @JvmStatic
        fun getBlob(cursor: Cursor, column: String?): ByteArray? {
            val index = cursor.getColumnIndex(column)
            if (index < 0) {
                return null
            }
            return cursor.getBlob(index)
        }

        @JvmStatic
        fun closeIfEmpty(cursor: Cursor?): Boolean {
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    return false
                }
            } catch (e: SQLException) {
                // Cursor.moveToFirst() may throw an exception when
                // the row is too big to fit into CursorWindow.
                // Catch this exception but still try to close the
                // Cursor object to free its resources.
            }

            cursor?.close()

            return true
        }

        private fun insertShader(
            db: SQLiteDatabase,
            shader: String?,
            name: String?,
            created: String?,
            modified: String?,
            thumbnail: ByteArray?,
            quality: Float
        ): Long {
            val cv = ContentValues()
            cv.put(SHADERS_FRAGMENT_SHADER, shader)
            cv.put(SHADERS_THUMB, thumbnail)
            cv.put(SHADERS_NAME, name)
            cv.put(SHADERS_CREATED, created)
            cv.put(SHADERS_MODIFIED, modified)
            cv.put(SHADERS_QUALITY, quality)
            return db.insert(SHADERS, null, cv)
        }

        @JvmStatic
        fun insertShader(
            db: SQLiteDatabase,
            shader: String?,
            name: String?,
            thumbnail: ByteArray?,
            quality: Float
        ): Long {
            val now = currentTime()
            return insertShader(db, shader, name, now, now, thumbnail, quality)
        }

        private fun insertTexture(
            db: SQLiteDatabase,
            name: String?,
            width: Int,
            height: Int,
            ratio: Float,
            thumb: ByteArray?,
            matrix: ByteArray?
        ): Long {
            val cv = ContentValues()
            cv.put(TEXTURES_NAME, name)
            cv.put(TEXTURES_WIDTH, width)
            cv.put(TEXTURES_HEIGHT, height)
            cv.put(TEXTURES_RATIO, ratio)
            cv.put(TEXTURES_THUMB, thumb)
            cv.put(TEXTURES_MATRIX, matrix)
            return db.insert(TEXTURES, null, cv)
        }

        @JvmStatic
        fun insertTexture(
            db: SQLiteDatabase, name: String?, bitmap: Bitmap, thumbnailSize: Int
        ): Long {
            val thumbnail: Bitmap

            try {
                thumbnail = Bitmap.createScaledBitmap(
                    bitmap, thumbnailSize, thumbnailSize, true
                )
            } catch (e: IllegalArgumentException) {
                return 0
            }

            val w = bitmap.width
            val h = bitmap.height

            return insertTexture(
                db, name, w, h, calculateRatio(w, h), bitmapToPng(thumbnail), bitmapToPng(bitmap)
            )
        }

        private fun currentTime(): String {
            return SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.US
            ).format(Date())
        }

        private fun textureFromCursor(cursor: Cursor): Bitmap? {
            val data = getBlob(cursor, TEXTURES_MATRIX)
            return if (data == null) null
            else BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        @Throws(IOException::class)
        private fun loadRawResource(
            context: Context, id: Int
        ): String? {
            var `in`: InputStream? = null
            try {
                `in` = context.resources.openRawResource(id)
                val l = `in`.available()
                val b = ByteArray(l)
                // StandardCharsets.UTF_8 would require API level 19.
                return if (`in`.read(b) == l) String(b, charset("UTF-8")) else null
            } finally {
                `in`?.close()
            }
        }

        private fun loadBitmapResource(context: Context, id: Int): ByteArray {
            return bitmapToPng(
                BitmapFactory.decodeResource(
                    context.resources, id
                )
            )
        }

        private fun bitmapToPng(bitmap: Bitmap): ByteArray {
            // Convert color space to SRGB because GLUtils.texImage2D()
            // can't handle other formats.
            var srgb = bitmap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && srgb.colorSpace !== ColorSpace.get(
                    ColorSpace.Named.SRGB
                )
            ) {
                val width = srgb.width
                val height = srgb.width
                val copy = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888
                )
                val pixels = IntArray(width * height)
                srgb.getPixels(pixels, 0, width, 0, 0, width, height)
                copy.setPixels(pixels, 0, width, 0, 0, width, height)
                srgb = copy
            }
            val out = ByteArrayOutputStream()
            srgb.compress(Bitmap.CompressFormat.PNG, 100, out)
            return out.toByteArray()
        }

        private fun calculateRatio(width: Int, height: Int): Float {
            // Round to two decimal places to avoid problems with
            // rounding errors. The query will filter precisely 1 or 1.5.
            return Math.round((height.toFloat() / width) * 100f) / 100f
        }

        private fun addShadersQuality(db: SQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE $SHADERS ADD COLUMN $SHADERS_QUALITY REAL;"
            )
            db.execSQL(
                "UPDATE $SHADERS SET $SHADERS_QUALITY = 1;"
            )
        }

        private fun addTexturesWidthHeightRatio(db: SQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE $TEXTURES ADD COLUMN $TEXTURES_WIDTH INTEGER;"
            )
            db.execSQL(
                "ALTER TABLE $TEXTURES ADD COLUMN $TEXTURES_HEIGHT INTEGER;"
            )
            db.execSQL(
                "ALTER TABLE $TEXTURES ADD COLUMN $TEXTURES_RATIO REAL;"
            )

            val cursor = db.rawQuery(
                "SELECT $TEXTURES_ID,$TEXTURES_MATRIX FROM $TEXTURES", null
            )

            if (closeIfEmpty(cursor)) {
                return
            }

            do {
                val bm = textureFromCursor(cursor) ?: continue

                val width = bm.width
                val height = bm.height
                val ratio = calculateRatio(width, height)
                bm.recycle()

                db.execSQL(
                    "UPDATE $TEXTURES SET $TEXTURES_WIDTH = $width, $TEXTURES_HEIGHT = $height, $TEXTURES_RATIO = $ratio WHERE $TEXTURES_ID = " + getLong(
                        cursor, TEXTURES_ID
                    ) + ";"
                )
            } while (cursor.moveToNext())

            cursor.close()
        }

        private fun addShaderNames(db: SQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE $SHADERS ADD COLUMN $SHADERS_NAME TEXT;"
            )
        }

        private fun importShaders(
            dst: SQLiteDatabase, src: SQLiteDatabase
        ): Boolean {
            val cursor = src.rawQuery(
                "SELECT * FROM $SHADERS ORDER BY $SHADERS_ID", null
            )
            if (cursor == null) {
                return false
            }
            val shaderIndex = cursor.getColumnIndex(SHADERS_FRAGMENT_SHADER)
            val thumbIndex = cursor.getColumnIndex(SHADERS_THUMB)
            val nameIndex = cursor.getColumnIndex(SHADERS_NAME)
            val createdIndex = cursor.getColumnIndex(SHADERS_CREATED)
            val modifiedIndex = cursor.getColumnIndex(SHADERS_MODIFIED)
            val qualityIndex = cursor.getColumnIndex(SHADERS_QUALITY)
            var success = true
            if (cursor.moveToFirst()) {
                do {
                    val createdDate = cursor.getString(createdIndex)
                    val modifiedDate = cursor.getString(modifiedIndex)
                    if (createdDate == null || modifiedDate == null || shaderExists(
                            dst, createdDate, modifiedDate
                        )
                    ) {
                        continue
                    }
                    val shaderId = insertShader(
                        dst,
                        cursor.getString(shaderIndex),
                        cursor.getString(nameIndex),
                        createdDate,
                        modifiedDate,
                        cursor.getBlob(thumbIndex),
                        cursor.getFloat(qualityIndex)
                    )
                    if (shaderId < 1) {
                        success = false
                        break
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return success
        }

        private fun shaderExists(
            db: SQLiteDatabase, createdDate: String, modifiedDate: String
        ): Boolean {
            val cursor = db.rawQuery(
                "SELECT $SHADERS_ID FROM $SHADERS WHERE $SHADERS_CREATED = ? AND $SHADERS_MODIFIED = ?",
                arrayOf(createdDate, modifiedDate)
            )
            if (cursor == null) {
                return false
            }
            val exists = cursor.moveToFirst() && cursor.count > 0
            cursor.close()
            return exists
        }

        private fun importTextures(
            dst: SQLiteDatabase, src: SQLiteDatabase
        ): Boolean {
            val cursor = src.rawQuery(
                "SELECT $TEXTURES_ID, $TEXTURES_NAME FROM $TEXTURES ORDER BY $TEXTURES_ID", null
            )
            if (cursor == null) {
                return false
            }
            val idIndex = cursor.getColumnIndex(TEXTURES_ID)
            val nameIndex = cursor.getColumnIndex(TEXTURES_NAME)
            var success = true
            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(nameIndex)
                    if (name == null || textureExists(dst, name)) {
                        continue
                    }
                    // Transfer textures one at a time because all of them
                    // may be too big for one cursor window.
                    if (!importTexture(dst, src, cursor.getLong(idIndex))) {
                        success = false
                        break
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return success
        }

        private fun importTexture(
            dst: SQLiteDatabase, src: SQLiteDatabase, srcId: Long
        ): Boolean {
            val cursor = src.rawQuery(
                "SELECT *  FROM $TEXTURES WHERE $TEXTURES_ID = ?", arrayOf(srcId.toString())
            )
            if (cursor == null) {
                return false
            }
            val nameIndex = cursor.getColumnIndex(TEXTURES_NAME)
            val widthIndex = cursor.getColumnIndex(TEXTURES_WIDTH)
            val heightIndex = cursor.getColumnIndex(TEXTURES_HEIGHT)
            val ratioIndex = cursor.getColumnIndex(TEXTURES_RATIO)
            val thumbIndex = cursor.getColumnIndex(TEXTURES_THUMB)
            val matrixIndex = cursor.getColumnIndex(TEXTURES_MATRIX)
            var success = true
            if (moveToFirstAndCatchOutOfMemory(cursor)) {
                do {
                    val textureId = insertTexture(
                        dst,
                        cursor.getString(nameIndex),
                        cursor.getInt(widthIndex),
                        cursor.getInt(heightIndex),
                        cursor.getFloat(ratioIndex),
                        cursor.getBlob(thumbIndex),
                        cursor.getBlob(matrixIndex)
                    )
                    if (textureId < 1) {
                        success = false
                        break
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return success
        }

        private fun moveToFirstAndCatchOutOfMemory(cursor: Cursor): Boolean {
            return try {
                cursor.moveToFirst()
            } catch (e: SQLException) {
                // Catch Row too big exceptions when the BLOB larger than
                // the Cursor Window.
                false
            }
        }

        private fun textureExists(db: SQLiteDatabase, name: String): Boolean {
            val cursor = db.rawQuery(
                "SELECT $TEXTURES_ID FROM $TEXTURES WHERE $TEXTURES_NAME = ?", arrayOf(name)
            )
            if (cursor == null) {
                return false
            }
            val exists = cursor.moveToFirst() && cursor.count > 0
            cursor.close()
            return exists
        }
    }
}
