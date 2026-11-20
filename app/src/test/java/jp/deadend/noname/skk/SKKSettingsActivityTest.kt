package jp.deadend.noname.skk

import androidx.preference.Preference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SKKSettingsActivityTest {

    @Test
    fun testBackWhileAssigningKeyCancelsAssignment() {
        val activity = Robolectric.buildActivity(SKKSettingsActivity::class.java).setup().get()
        activity.keyPref = Preference(activity).apply { key = "test_key" }

        activity.onBackPressedDispatcher.onBackPressed()

        // 戻る操作でキー割り当てをキャンセルし、画面は閉じない
        assertNull(activity.keyPref)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun testBackWithoutAssignmentFinishesActivity() {
        val activity = Robolectric.buildActivity(SKKSettingsActivity::class.java).setup().get()
        activity.keyPref = null

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }
}
