package jp.deadend.noname.skk

import androidx.test.core.app.ApplicationProvider
import android.content.Intent
import android.view.KeyEvent
import jp.deadend.noname.skk.engine.SKKHiraganaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * IME サービスのライフサイクルが「落ちないこと」を確認する。
 * onCreate を経由せずに onDestroy が呼ばれても super.onDestroy() まで到達すること、
 * 候補ビューに null が渡されても NPE にならないことを対象にする。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SKKServiceLifecycleTest {

    @Test
    fun testCreateAndDestroy() {
        val controller = Robolectric.buildService(SKKService::class.java).create()
        val service = controller.get()
        assertTrue(SKKService.isRunning())
        // 後始末が例外なく完了し、静的インスタンスが残らないこと。
        controller.destroy()
        assertFalse(SKKService.isRunning())
    }

    @Test
    fun testSetCandidatesViewAcceptsNull() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        // フレームワークが候補ビューを隠すときに null を渡す経路。
        service.setCandidatesView(null)
    }

    @Test
    fun testApplicationContextIsAvailable() {
        assertNotNull(ApplicationProvider.getApplicationContext<SKKApplication>())
    }

    @Test
    fun testUninitializedEngineDoesNotCrashLifecycle() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        // 辞書オープン失敗で onCreate が早期 return した状態を再現する
        val f = SKKService::class.java.getDeclaredField("mEngine")
        f.isAccessible = true
        f.set(service, null)

        assertEquals(SKKHiraganaState, service.engineState)
        service.onFinishInput()
        service.onKeyDown(KeyEvent.KEYCODE_A, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))

        val intent = Intent().putExtra(SKKService.KEY_COMMAND, SKKService.COMMAND_CLOSE_USER_DICT)
        service.onStartCommand(intent, 0, 1)
    }
}
