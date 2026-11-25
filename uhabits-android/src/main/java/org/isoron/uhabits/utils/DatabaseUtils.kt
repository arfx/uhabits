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
     * Creates an export-only copy that can be imported by the Play Store version
     * Loop 2.3.1 (DATABASE_VERSION 25). The live database remains at
     * DATABASE_VERSION (30); only the copy's user_version is lowered to 25.
     * Group rows and their derived repetitions are moved into side tables before export.
     * Loop 2.3.1 ignores those additional tables and imports only ordinary habits.
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
                exportedDatabase.beginTransaction()
                try {
                    exportedDatabase.execSQL(
                        """CREATE TABLE IF NOT EXISTS GroupBackup (
                            uuid TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            description TEXT NOT NULL,
                            question TEXT NOT NULL,
                            freq_num INTEGER NOT NULL,
                            freq_den INTEGER NOT NULL,
                            color INTEGER NOT NULL,
                            position INTEGER NOT NULL,
                            archived INTEGER NOT NULL,
                            type INTEGER NOT NULL,
                            target_value REAL NOT NULL,
                            target_type INTEGER NOT NULL,
                            unit TEXT NOT NULL,
                            category TEXT NOT NULL,
                            has_goal INTEGER NOT NULL,
                            group_goal_enabled INTEGER NOT NULL,
                            group_goal_type INTEGER NOT NULL DEFAULT 0,
                            group_goal_threshold INTEGER NOT NULL DEFAULT 1
                        )"""
                    )
                    exportedDatabase.execSQL(
                        """CREATE TABLE IF NOT EXISTS GroupBackupMembers (
                            group_uuid TEXT NOT NULL,
                            habit_uuid TEXT NOT NULL,
                            position INTEGER NOT NULL,
                            PRIMARY KEY (group_uuid, habit_uuid)
                        )"""
                    )
                    exportedDatabase.execSQL(
                        """INSERT INTO GroupBackup
                            SELECT uuid, name, description, question, freq_num, freq_den,
                                   color, position, archived, type, target_value,
                                   target_type, unit, category, has_goal, group_goal_enabled,
                                   group_goal_type, group_goal_threshold
                            FROM Habits WHERE is_group = 1 AND uuid IS NOT NULL"""
                    )
                    exportedDatabase.execSQL(
                        """INSERT INTO GroupBackupMembers
                            SELECT groups.uuid, habits.uuid, habits.position
                            FROM Habits habits
                            INNER JOIN Habits groups ON habits.group_id = groups.id
                            WHERE groups.is_group = 1
                              AND habits.is_group = 0
                              AND groups.uuid IS NOT NULL
                              AND habits.uuid IS NOT NULL"""
                    )
                    exportedDatabase.execSQL(
                        "DELETE FROM Repetitions WHERE habit IN (SELECT id FROM Habits WHERE is_group = 1)"
                    )
                    exportedDatabase.execSQL("DELETE FROM Habits WHERE is_group = 1")
                    exportedDatabase.version = 25
                    exportedDatabase.setTransactionSuccessful()
                } finally {
                    exportedDatabase.endTransaction()
                }
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
