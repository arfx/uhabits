package org.isoron.uhabits.intents

import org.isoron.uhabits.BaseAndroidTest
import org.junit.Test
class IntentFactoryTest : BaseAndroidTest() {
    @Test
    fun testShowHabitIntentUsesForkContentUriAuthority() {
        val intent = IntentFactory().startShowHabitActivity(targetContext, 42L)

        assertEquals("content", intent.data?.scheme)
        assertEquals("org.arfx.habittracker", intent.data?.authority)
        assertEquals("/habit/42", intent.data?.path)
    }
}
