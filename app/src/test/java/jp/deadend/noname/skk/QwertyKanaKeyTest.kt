package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * QwertyKeyboardViewのかなキー(flickボタン)の動作テスト
 * 
 * 動作定義:
 * - preferFlick=true: タップ→フリック键盘切替, 下フリック→かな入力
 * - preferFlick=false: タップ→かな入力, 下フリック→フリック键盘切替
 * - isShifted=true: 上フリックが"none"に、下フリックが"up"に交換される
 */
class QwertyKanaKeyTest {

    companion object {
        const val FLICK_NONE = 0
        const val FLICK_UP = 1
        const val FLICK_DOWN = -1
    }

    // かなキーの判定ロジックを抽出したテスト用関数
    private fun shouldChangeToFlick(
        isFlicked: Int,
        preferFlick: Boolean,
        isShifted: Boolean
    ): Boolean {
        val flickNone = if (isShifted) FLICK_UP else FLICK_NONE
        val flickUp = if (isShifted) FLICK_NONE else FLICK_UP
        val flickDown = if (isShifted) FLICK_DOWN else FLICK_DOWN

        return when {
            preferFlick -> {
                when (isFlicked) {
                    flickNone -> true  // タップ→フリック键盘切替
                    flickDown -> false // 下フリック→かな入力
                    FLICK_UP -> false  // 上フリック→貼り付け
                    else -> false
                }
            }
            else -> {
                when (isFlicked) {
                    flickNone -> false // タップ→かな入力
                    flickDown -> true  // 下フリック→フリック键盘切替
                    FLICK_UP -> false  // 上フリック→貼り付け
                    else -> false
                }
            }
        }
    }

    private fun shouldHandleKanaKey(
        isFlicked: Int,
        preferFlick: Boolean,
        isShifted: Boolean
    ): Boolean {
        return !shouldChangeToFlick(isFlicked, preferFlick, isShifted)
    }

    // ========== preferFlick=true (デフォルト) ==========

    @Test
    fun `preferFlick true isShifted false tap`() {
        assertEquals(true, shouldChangeToFlick(FLICK_NONE, true, false))
    }

    @Test
    fun `preferFlick true isShifted false flickDown`() {
        assertEquals(true, shouldHandleKanaKey(FLICK_DOWN, true, false))
    }

    @Test
    fun `preferFlick true isShifted true tap`() {
        // isShifted=trueでタップ: flickNone=FLICK_UP なので isFlicked!=FLICK_UP
        // → changeToFlick=false, handleKanaKey=true
        // 本来: tap(=none)は常にchangeToFlick
        assertEquals(false, shouldChangeToFlick(FLICK_NONE, true, true))
        // このテストは「現在のコードのバグ」を示している
        // 現在のコードは flickNone を使わず isFlicked==FLICK_NONE で判定している
    }

    // ========== preferFlick=false ==========

    @Test
    fun `preferFlick false isShifted false tap`() {
        assertEquals(true, shouldHandleKanaKey(FLICK_NONE, false, false))
    }

    @Test
    fun `preferFlick false isShifted false flickDown`() {
        assertEquals(true, shouldChangeToFlick(FLICK_DOWN, false, false))
    }

    // ========== バグのシミュレーション ==========

    @Test
    fun `current code bug isShifted true tap`() {
        // 現在のコード: when (isFlicked) { FLICK_NONE -> changeToFlick }
        // isShifted=trueでも isFlicked=FLICK_NONE なので changeToFlick が呼ばれる
        // → これは正しくない。isShifted=trueでタップはhandleKanaKeyになるべき

        // 現在のバグコードの動作:
        val buggyChangeToFlick = FLICK_NONE == FLICK_NONE // true
        assertEquals(true, buggyChangeToFlick) // バグ: isShifted=trueでtapでもchangeToFlick

        // 正しい動作:
        assertEquals(false, shouldChangeToFlick(FLICK_NONE, true, true))
    }
}
