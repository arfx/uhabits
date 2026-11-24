/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.isoron.platform.time.DateFormats.Companion.getBackupDateFormat
import org.isoron.uhabits.HabitsApplication.Companion.isTestMode
import org.isoron.uhabits.HabitsDatabaseOpener
import org.isoron.uhabits.core.DATABASE_FILENAME
import org.isoron.uhabits.core.DATABASE_VERSION
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

object DatabaseUtils {
    private var opener: HabitsDatabaseOpener? = null

    @JvmStatic
    fun getDatabaseFile(context: Context): File {
        val databaseFilename = databaseFilename
        val root = context.filesDir.path
        return File("$root/../databases/$databaseFilename")
    }

    private val databaseFilename: String
        get() {
            var databaseFilename: String = DATABASE_FILENAME
            if (isTestMode()) databaseFilename = "test.db"
            return databaseFilename
        }

    fun initializeDatabase(context: Context?) {
        opener = HabitsDatabaseOpener(
            context!!,
            databaseFilename,
            DATABASE_VERSION
        )
    }

    @JvmStatic
    @Throws(IOException::class)
    fun saveDatabaseCopy(context: Context, dir: File): String {
        val dateFormat: SimpleDateFormat = getBackupDateFormat()
        val date = dateFormat.format(System.currentTimeMillis())
        val filename = "${dir.absolutePath}/Loop Habits Backup $date.db"
        Log.i("DatabaseUtils", "Writing: $filename")
        val dbCopy = createMainCompatibleDatabaseCopy(context)
        try {
            val destination = File(filename)
            dbCopy.copyTo(destination, overwrite = true)
            return destination.absolutePath
        } finally {
            dbCopy.delete()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun saveDatabaseCopy(context: Context, dir: DocumentFile): String {
        val dateFormat: SimpleDateFormat = getBackupDateFormat()
        val date = dateFormat.format(System.currentTimeMillis())
        val file = dir.createFile("application/octet-stream", "Loop Habits Backup $date.db")
            ?: throw IOException("Unable to create backup file")
        Log.i("DatabaseUtils", "Writing: ${file.uri}")
        val dbCopy = createMainCompatibleDatabaseCopy(context)
        try {
            dbCopy.inputStream().use { input ->
                context.contentResolver.openOutputStream(file.uri)?.use { output ->
                    input.copyTo(output)
                } ?: throw IOException("Unable to open backup destination")
            }
        } finally {
            dbCopy.delete()
        }
        return file.uri.toString()
    }

    /**
     * Creates an export-only copy that can be imported by the current main branch.
     * The live database remains at DATABASE_VERSION (27); only the copy's
     * user_version is lowered to 26. Main ignores the additional has_goal column.
     */
    private fun createMainCompatibleDatabaseCopy(context: Context): File {
        val source = getDatabaseFile(context)
        val copy = File.createTempFile("loop-backup-", ".db", context.cacheDir)
        try {
            source.copyTo(copy, overwrite = true)
            SQLiteDatabase.openDatabase(
                copy.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { exportedDatabase ->
                exportedDatabase.version = 26
            }
            return copy
        } catch (error: Exception) {
            copy.delete()
            throw error
        }
    }

    fun openDatabase(): SQLiteDatabase {
        checkNotNull(opener)
        return opener!!.writableDatabase
    }
}
