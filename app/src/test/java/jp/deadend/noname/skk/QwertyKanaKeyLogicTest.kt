package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Qwertyかなキーの動作ロジックテスト
 * 
 * 期待動作（isShiftedに関係なくisFlickedで直接判定）:
 * preferFlick=true (デフォルト):
 *   - タップ(isFlicked=NONE) → フリック键盘切替
 *   - 下フリック(isFlicked=DOWN) → かな入力確定
 *   - 上フリック(isFlicked=UP) → 貼り付け
 *   - 左フリック(isFlicked=LEFT) → 絵文字
 * 
 * preferFlick=false:
 *   - タップ(isFlicked=NONE) → かな入力確定
 *   - 下フリック(isFlicked=DOWN) → フリック键盘切替
 *   - 上フリック(isFlicked=UP) → 貼り付け
 *   - 左フリック(isFlicked=LEFT) → 絵文字
 */
class QwertyKanaKeyLogicTest {

    companion object {
        const val FLICK_NONE = 0
        const val FLICK_UP = 1
        const val FLICK_DOWN = -1
        const val FLICK_LEFT = -2
    }

    sealed interface Action
    object ChangeToFlick : Action
    object HandleKanaKey : Action
    object PasteClip : Action
    object ShowEmojiPicker : Action
    object NoOp : Action

    /**
     * Qwertyかなキーの判定ロジック（修正版）
     * 
     * isShiftedに関係なくisFlickedで直接判定
     */
    private fun evaluateQwertyKanaKey(
        isFlicked: Int,
        preferFlick: Boolean
    ): Action {
        return when (isFlicked) {
            FLICK_LEFT -> ShowEmojiPicker
            FLICK_NONE -> if (preferFlick) ChangeToFlick else HandleKanaKey
            FLICK_UP -> PasteClip
            FLICK_DOWN -> if (preferFlick) HandleKanaKey else ChangeToFlick
            else -> NoOp
        }
    }

    // ========== preferFlick=true ==========

    @Test
    fun `preferFlick true tap`() {
        assertEquals(ChangeToFlick, evaluateQwertyKanaKey(FLICK_NONE, true))
    }

    @Test
    fun `preferFlick true flickDown`() {
        assertEquals(HandleKanaKey, evaluateQwertyKanaKey(FLICK_DOWN, true))
    }

    @Test
    fun `preferFlick true flickUp`() {
        assertEquals(PasteClip, evaluateQwertyKanaKey(FLICK_UP, true))
    }

    @Test
    fun `preferFlick true flickLeft`() {
        assertEquals(ShowEmojiPicker, evaluateQwertyKanaKey(FLICK_LEFT, true))
    }

    // ========== preferFlick=false ==========

    @Test
    fun `preferFlick false tap`() {
        assertEquals(HandleKanaKey, evaluateQwertyKanaKey(FLICK_NONE, false))
    }

    @Test
    fun `preferFlick false flickDown`() {
        assertEquals(ChangeToFlick, evaluateQwertyKanaKey(FLICK_DOWN, false))
    }

    @Test
    fun `preferFlick false flickUp`() {
        assertEquals(PasteClip, evaluateQwertyKanaKey(FLICK_UP, false))
    }

    @Test
    fun `preferFlick false flickLeft`() {
        assertEquals(ShowEmojiPicker, evaluateQwertyKanaKey(FLICK_LEFT, false))
    }
}
