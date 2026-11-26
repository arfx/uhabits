package org.isoron.uhabits.core.database

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.TestDatabaseHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HabitCustomColorRepositoryTest {
    @Test
    fun testCustomColorInsertUpdateClearAndDelete() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = HabitRepository(db)
        val firstColor = 0xFF12AB34.toInt()
        val secondColor = 0xFF987654.toInt()
        val data = HabitData(
            name = "Custom color habit",
            position = 0,
            uuid = "custom-color-habit",
            customColor = firstColor
        )

        data.id = repo.insert(data)
        assertEquals(firstColor, repo.findAll().single().customColor)

        data.customColor = secondColor
        repo.update(data)
        assertEquals(secondColor, repo.findAll().single().customColor)

        data.customColor = null
        repo.update(data)
        assertNull(repo.findAll().single().customColor)

        data.customColor = firstColor
        repo.update(data)
        repo.delete(data.id!!)
        assertEquals(0, repo.findAll().size)

        db.close()
    }

    @Test
    fun testCustomColorsAreAssociatedWithTheirHabitUuid() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = HabitRepository(db)
        val firstColor = 0xFF112233.toInt()
        val secondColor = 0xFF445566.toInt()
        val quotedUuid = "habit'with-quote"
        val quotedColor = 0xFF778899.toInt()

        repo.insert(HabitData(name = "First", position = 0, uuid = "first-uuid", customColor = firstColor))
        repo.insert(HabitData(name = "Second", position = 1, uuid = "second-uuid", customColor = secondColor))
        repo.insert(HabitData(name = "Quoted UUID", position = 2, uuid = quotedUuid, customColor = quotedColor))

        val colors = repo.findAll().associate { it.uuid to it.customColor }
        assertEquals(firstColor, colors["first-uuid"])
        assertEquals(secondColor, colors["second-uuid"])
        assertEquals(quotedColor, colors[quotedUuid])

        db.close()
    }
}
