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
package org.isoron.uhabits.core.io

import me.tatarka.inject.annotations.Inject
import org.isoron.platform.io.Database
import org.isoron.platform.io.DatabaseOpener
import org.isoron.platform.io.FileOpener
import org.isoron.platform.io.UserFile
import org.isoron.platform.io.getVersion
import org.isoron.platform.io.setVersion
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.platform.io.querySingle
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.GroupHabitCalculator
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.database.HabitData
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.isoron.uhabits.core.utils.isSQLite3File

/**
 * Class that imports data from database files exported by Loop Habit Tracker.
 */
@Inject
class LoopDBImporter(
    @AppScope val habitList: HabitList,
    @AppScope val modelFactory: ModelFactory,
    @AppScope val opener: DatabaseOpener,
    @AppScope val runner: CommandRunner,
    @AppScope logging: Logging,
    @AppScope val fileOpener: FileOpener
) : AbstractImporter() {

    private val logger = logging.getLogger("LoopDBImporter")

    override suspend fun canHandle(file: UserFile): Boolean {
        if (!isSQLite3File(file)) return false
        val db = opener.open(file.pathString)
        var canHandle = true
        val count = db.querySingle(
            "select count(*) from SQLITE_MASTER where name='Habits' or name='Repetitions'"
        ) { it.getInt(0) }
        if (count == null || count != 2) {
            logger.error("Cannot handle file: tables not found")
            canHandle = false
        }
        if (db.getVersion() > DATABASE_VERSION) {
            logger.error("Cannot handle file: incompatible version: ${db.getVersion()} > $DATABASE_VERSION")
            canHandle = false
        }
        db.close()
        return canHandle
    }

    override suspend fun importHabitsFromFile(file: UserFile) {
        val db = opener.open(file.pathString)
        reconcileSchemaVersion(db)
        db.migrateTo(DATABASE_VERSION) { version ->
            val filename = org.isoron.platform.io.format("%02d.sql", version)
            fileOpener.openResourceFile("migrations/$filename").lines().joinToString("\n")
        }

        val habitDataList = loadHabits(db)
        for (habitData in habitDataList) {
            var habit = habitList.getByUUID(habitData.uuid)

            if (habit == null) {
                habit = modelFactory.buildHabit()
                val imported = habitData.copy(id = null)
                SQLiteHabitList.copyTo(imported, habit)
                CreateHabitCommand(modelFactory, habitList, habit).run()
            } else {
                val modified = modelFactory.buildHabit()
                SQLiteHabitList.copyTo(habitData.copy(id = habit.id), modified)
                EditHabitCommand(habitList, habit.id!!, modified).run()
            }

            habit = habitList.getByUUID(habitData.uuid)!!
            val entries = habit.originalEntries

            db.query(
                "SELECT timestamp, value, notes FROM Repetitions WHERE habit = ? ORDER BY timestamp DESC",
                habitData.id.toString()
            ) { stmt ->
                val timestamp = stmt.getLongOrNull(0) ?: return@query
                val value = stmt.getIntOrNull(1) ?: return@query
                val notes = stmt.getTextOrNull(2) ?: ""
                val date = LocalDate.fromUnixTime(timestamp)
                val (_, existingValue, existingNotes) = entries.get(date)
                if (existingValue != value || existingNotes != notes) {
                    entries.add(Entry(date, value, notes))
                }
            }
            habit.recompute()
        }
        restoreGroups(db)
        habitList.resort()
        db.close()
    }

    /**
     * Some backups are created from a newer schema but retain an older
     * PRAGMA user_version. Reconcile the known post-v25 columns before
     * running migrations, otherwise ALTER TABLE fails with duplicate columns.
     */
    private fun reconcileSchemaVersion(db: Database) {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(Habits)") { stmt ->
            columns.add(stmt.getText(1))
        }

        if (db.getVersion() < 26 && "category" in columns) db.setVersion(26)
        if (db.getVersion() < 27 && "has_goal" in columns) db.setVersion(27)
        if (db.getVersion() < 28 && "is_group" in columns && "group_id" in columns) {
            db.setVersion(28)
        }
        if (db.getVersion() < 29 && "group_goal_enabled" in columns) db.setVersion(29)
    }

    /**
     * Restores group metadata from backups created by this fork. Older Loop
     * versions simply ignore these additional tables and import ordinary habits.
     */
    private fun restoreGroups(db: Database) {
        val hasGroupBackup = db.querySingle(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='GroupBackup'"
        ) { it.getInt(0) } ?: 0
        if (hasGroupBackup == 0) return

        // Read the backup metadata completely before changing the destination
        // HabitList. Keeping a cursor open while issuing nested queries on the
        // same SQLiteDatabase is fragile on Android and can interrupt iteration.
        data class GroupBackupData(
            val uuid: String,
            val name: String,
            val description: String,
            val question: String,
            val freqNum: Int,
            val freqDen: Int,
            val color: Int,
            val archived: Boolean,
            val category: String,
            val hasGoal: Boolean,
            val groupGoalEnabled: Boolean
        )

        val groups = mutableListOf<GroupBackupData>()
        db.query(
            """SELECT uuid, name, description, question, freq_num, freq_den,
                      color, position, archived, type, target_value, target_type,
                      unit, category, has_goal, group_goal_enabled
               FROM GroupBackup ORDER BY position"""
        ) { stmt ->
            val uuid = stmt.getTextOrNull(0) ?: return@query
            groups.add(
                GroupBackupData(
                    uuid = uuid,
                    name = stmt.getText(1),
                    description = stmt.getText(2),
                    question = stmt.getText(3),
                    freqNum = stmt.getInt(4),
                    freqDen = stmt.getInt(5),
                    color = stmt.getInt(6),
                    archived = stmt.getInt(8) != 0,
                    category = stmt.getText(13),
                    hasGoal = stmt.getInt(14) != 0,
                    groupGoalEnabled = stmt.getInt(15) != 0
                )
            )
        }

        val membersByGroup = mutableMapOf<String, MutableList<String>>()
        db.query(
            """SELECT group_uuid, habit_uuid FROM GroupBackupMembers
               ORDER BY group_uuid, position"""
        ) { stmt ->
            val groupUuid = stmt.getTextOrNull(0) ?: return@query
            val habitUuid = stmt.getTextOrNull(1) ?: return@query
            membersByGroup.getOrPut(groupUuid) { mutableListOf() }.add(habitUuid)
        }

        for (backupGroup in groups) {
            val group = habitList.getByUUID(backupGroup.uuid) ?: modelFactory.buildHabit()
            group.uuid = backupGroup.uuid
            group.name = backupGroup.name
            group.description = backupGroup.description
            group.question = backupGroup.question
            group.frequency = Frequency(backupGroup.freqNum, backupGroup.freqDen)
            group.color = PaletteColor(backupGroup.color)
            group.isArchived = backupGroup.archived
            group.type = HabitType.YES_NO
            group.targetValue = 0.0
            group.unit = ""
            group.category = backupGroup.category
            group.hasGoal = backupGroup.hasGoal
            group.isGroup = true
            group.groupId = null
            group.groupGoalEnabled = backupGroup.groupGoalEnabled

            if (group.id == null) habitList.add(group) else habitList.update(listOf(group))

            // Remove stale links before applying the membership from this backup.
            habitList.filter { it.groupId == group.id && !it.isGroup }.forEach { oldChild ->
                oldChild.groupId = null
                habitList.update(listOf(oldChild))
            }

            for (childUuid in membersByGroup[backupGroup.uuid].orEmpty()) {
                val child = habitList.getByUUID(childUuid) ?: continue
                child.groupId = group.id
                habitList.update(listOf(child))
            }

            val children = habitList.filter { it.groupId == group.id && !it.isGroup }
            GroupHabitCalculator.recompute(group, children)
            habitList.update(listOf(group))
        }
    }

    private fun loadHabits(db: Database): List<HabitData> {
        val result = mutableListOf<HabitData>()
        db.query(
            "SELECT id, name, description, question, freq_num, freq_den, color, " +
                "position, reminder_hour, reminder_min, reminder_days, highlight, " +
                "archived, type, target_value, target_type, unit, uuid, category, has_goal " +
                "FROM Habits ORDER BY position"
        ) { stmt ->
            result.add(
                HabitData(
                    id = stmt.getLongOrNull(0),
                    name = stmt.getTextOrNull(1) ?: "",
                    description = stmt.getTextOrNull(2) ?: "",
                    question = stmt.getTextOrNull(3) ?: "",
                    freqNum = stmt.getIntOrNull(4) ?: 1,
                    freqDen = stmt.getIntOrNull(5) ?: 1,
                    color = stmt.getIntOrNull(6) ?: 0,
                    position = stmt.getIntOrNull(7) ?: 0,
                    reminderHour = stmt.getIntOrNull(8),
                    reminderMin = stmt.getIntOrNull(9),
                    reminderDays = stmt.getIntOrNull(10) ?: 0,
                    highlight = stmt.getIntOrNull(11) ?: 0,
                    archived = stmt.getIntOrNull(12) ?: 0,
                    type = stmt.getIntOrNull(13) ?: 0,
                    targetValue = stmt.getRealOrNull(14) ?: 0.0,
                    targetType = stmt.getIntOrNull(15) ?: 0,
                    unit = stmt.getTextOrNull(16) ?: "",
                    uuid = stmt.getTextOrNull(17),
                    category = stmt.getText(18),
                    hasGoal = stmt.getInt(19)
                )
            )
        }
        return result
    }
}
