package jp.deadend.noname.skk

import jp.deadend.noname.skk.engine.SKKConfirmingState
import jp.deadend.noname.skk.engine.SKKEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「削除? (y/N)」の確認待ちが入力欄をまたいで残らないことを確認する。
 * 状態はエンジンのインスタンスが持つので、確定・入力開始で必ず破棄され、
 * 別の入力欄（別エンジン）とは共有されない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SKKEnginePendingConfirmationTest {

    private lateinit var engine: SKKEngine

    @Before
    fun setUp() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        engine = service.mEngine
        clearAll()
    }

    @After
    fun tearDown() {
        clearAll()
    }

    private fun confirmingStates(target: SKKEngine = engine): List<SKKConfirmingState> =
            listOf(
                    target.chooseState,
                    target.emojiState,
                    target.kanjiState,
                    target.narrowingState,
                    target.okuriganaState,
            )

    private fun clearAll() {
        confirmingStates().forEach {
            it.pendingLambda = null
            it.oldComposingText = ""
        }
    }

    @Test
    fun testCommitComposingClearsPendingConfirmation() {
        var invoked = false
        engine.chooseState.pendingLambda = { invoked = true }
        engine.chooseState.oldComposingText = "削除? (y/N) /漢字/"

        engine.commitComposing()

        assertNull(engine.chooseState.pendingLambda)
        assertEquals("", engine.chooseState.oldComposingText)
        assertFalse("入力欄を閉じた時点で確認待ちは実行しない", invoked)
    }

    @Test
    fun testResetOnStartInputClearsAllConfirmingStates() {
        var invoked = false
        confirmingStates().forEach {
            it.pendingLambda = { invoked = true }
            it.oldComposingText = "pending"
        }

        engine.resetOnStartInput()

        confirmingStates().forEach {
            assertNull(it.pendingLambda)
            assertEquals("", it.oldComposingText)
        }
        assertFalse(invoked)
    }

    @Test
    fun testPendingConfirmationIsNotSharedBetweenEngines() {
        // 別の入力欄（別エンジン）の開始で、こちらの確認待ちが消えてはいけない
        val otherEngine =
                Robolectric.buildService(SKKService::class.java).create().get().mEngine
        engine.chooseState.pendingLambda = {}
        engine.chooseState.oldComposingText = "削除? (y/N) /漢字/"

        otherEngine.resetOnStartInput()

        assertNotNull("確認待ちがエンジン間で共有されている", engine.chooseState.pendingLambda)
        assertEquals("削除? (y/N) /漢字/", engine.chooseState.oldComposingText)
    }
}
