package jp.deadend.noname.skk

import androidx.preference.Preference
import jp.deadend.noname.dialog.SimpleMessageDialogFragment
import jp.deadend.noname.dialog.showDialogSafely
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun testShowDialogSafelyShowsWhenActivityIsActive() {
        val activity = Robolectric.buildActivity(SKKSettingsActivity::class.java).setup().get()

        activity.showDialogSafely(
            SimpleMessageDialogFragment.newInstance("test"),
            "test_dialog"
        )
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("test_dialog"))
    }

    @Test
    fun testShowDialogSafelySkipsWhenActivityIsFinishing() {
        val activity = Robolectric.buildActivity(SKKSettingsActivity::class.java).setup().get()
        activity.finish()

        activity.showDialogSafely(
            SimpleMessageDialogFragment.newInstance("test"),
            "test_dialog"
        )

        assertNull(activity.supportFragmentManager.findFragmentByTag("test_dialog"))
    }
}
