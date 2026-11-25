package jp.deadend.noname.skk

import jp.deadend.noname.skk.engine.SKKEmojiState
import jp.deadend.noname.skk.engine.SKKHiraganaState
import jp.deadend.noname.skk.engine.SKKOkuriganaState
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 状態機械が空バッファでも落ちないことを確認する。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SKKEngineStateTest {

    @Test
    fun testOkuriganaAfterBackspaceWithEmptyKanjiKey() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        val engine = service.mEngine
        engine.mKanjiKey.setLength(0)
        engine.mComposing.setLength(0)

        // 以前は deleteCharAt(-1) で StringIndexOutOfBoundsException になった
        SKKOkuriganaState.afterBackspace(engine)
    }

    @Test
    fun testPickCandidateInUnexpectedStateDoesNotThrow() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        val engine = service.mEngine
        engine.changeState(SKKHiraganaState)
        // 以前は RuntimeException("cannot pick candidate in ...") を投げていた
        engine.pickCandidatesViewManually(0)
    }

    @Test
    fun testChangeSoftKeyboardWithUnexpectedStateDoesNotThrow() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        skkPrefs.preferFlick = false
        // 以前は Exception("invalid state: ...") を投げていた
        service.changeSoftKeyboard(SKKEmojiState)
    }

    @Test
    fun testCommitComposingWithoutInputConnectionDoesNotCrash() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        assertNull(service.currentInputConnection)
        val engine = service.mEngine
        engine.mKanjiKey.setLength(0)
        engine.mComposing.setLength(0)

        // 以前は reset() が currentInputConnection を無チェックで触って NPE になった
        engine.commitComposing()
    }
}
