package jp.deadend.noname.skk

import jp.deadend.noname.skk.engine.SKKChooseState
import jp.deadend.noname.skk.engine.SKKEmojiState
import jp.deadend.noname.skk.engine.SKKKanjiState
import jp.deadend.noname.skk.engine.SKKNarrowingState
import jp.deadend.noname.skk.engine.SKKOkuriganaState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「削除? (y/N)」の確認待ちが入力欄をまたいで残らないことを確認する。
 * 状態は object（プロセス共有）なので、確定・入力開始で必ず破棄される必要がある。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SKKEnginePendingConfirmationTest {

    private lateinit var engine: jp.deadend.noname.skk.engine.SKKEngine

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

    private fun clearAll() {
        listOf(
            SKKChooseState, SKKEmojiState, SKKKanjiState,
            SKKNarrowingState, SKKOkuriganaState,
        ).forEach {
            it.pendingLambda = null
            it.oldComposingText = ""
        }
    }

    @Test
    fun testCommitComposingClearsPendingConfirmation() {
        var invoked = false
        SKKChooseState.pendingLambda = { invoked = true }
        SKKChooseState.oldComposingText = "削除? (y/N) /漢字/"

        engine.commitComposing()

        assertNull(SKKChooseState.pendingLambda)
        assertEquals("", SKKChooseState.oldComposingText)
        assertFalse("入力欄を閉じた時点で確認待ちは実行しない", invoked)
    }

    @Test
    fun testResetOnStartInputClearsAllConfirmingStates() {
        var invoked = false
        listOf(
            SKKChooseState, SKKEmojiState, SKKKanjiState,
            SKKNarrowingState, SKKOkuriganaState,
        ).forEach {
            it.pendingLambda = { invoked = true }
            it.oldComposingText = "pending"
        }

        engine.resetOnStartInput()

        listOf(
            SKKChooseState, SKKEmojiState, SKKKanjiState,
            SKKNarrowingState, SKKOkuriganaState,
        ).forEach {
            assertNull(it.pendingLambda)
            assertEquals("", it.oldComposingText)
        }
        assertFalse(invoked)
    }
}
