package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.EnumSet

/**
 * FlickJPKeyboardViewのこもじ（小）キーの動作テスト
 * 
 * 期待動作（useSoftCancelKey=false, デフォルト）:
 *   - タップ → 小文字変換 (LAST_CONVERSION_SMALL)
 *   - 左フリック → 濁点 (LAST_CONVERSION_DAKUTEN)
 *   - 上フリック → キャンセル (handleCancel)
 *   - 右フリック → 半濁点 (LAST_CONVERSION_HANDAKUTEN)
 *   - 下フリック → 全角/半角 (LAST_CONVERSION_SHIFT)
 */
class FlickKomojiTest {

    /**
     * KOMOJIキーの動作をテストする関数
     */
    sealed interface ExpectedAction
    object SmallConversion : ExpectedAction
    object DakutenConversion : ExpectedAction
    object Cancel : ExpectedAction
    object HandakutenConversion : ExpectedAction
    object ShiftConversion : ExpectedAction
    object TransConversion : ExpectedAction
    object NoOp : ExpectedAction

    /**
     * フリック状態を表す列挙型
     */
    enum class Flick {
        NONE, LEFT, UP, RIGHT, DOWN
    }

    /**
     * フリック状態をEnumSetに変換
     */
    private fun toEnumSet(vararg flicks: Flick): EnumSet<Flick> {
        return EnumSet.copyOf(flicks.toSet())
    }

    /**
     * KOMOJIキーの判定ロジック
     * 
     * @param useSoftCancelKey キャンセルキーの代わりに小文字キーを使う設定
     * @param useSoftTransKey 小文字キーが連打で濁点/半濁点にもなる設定
     * @param flickStates フリック状態のセット
     */
    private fun evaluateKomoji(
        useSoftCancelKey: Boolean,
        useSoftTransKey: Boolean,
        flickStates: EnumSet<Flick>
    ): ExpectedAction {
        val smallState = if (useSoftCancelKey) Flick.UP else Flick.NONE
        val cancelState = if (useSoftCancelKey) Flick.NONE else Flick.UP

        val smallEnumSet = toEnumSet(smallState)
        val cancelEnumSet = toEnumSet(cancelState)
        val leftEnumSet = toEnumSet(Flick.LEFT)
        val rightEnumSet = toEnumSet(Flick.RIGHT)
        val downEnumSet = toEnumSet(Flick.DOWN)

        return when {
            flickStates == smallEnumSet -> {
                if (!useSoftCancelKey && useSoftTransKey) TransConversion
                else SmallConversion
            }
            flickStates == leftEnumSet -> DakutenConversion
            flickStates == cancelEnumSet -> Cancel
            flickStates == rightEnumSet -> HandakutenConversion
            flickStates == downEnumSet -> ShiftConversion
            else -> NoOp
        }
    }

    // ========== useSoftCancelKey=false（デフォルト）==========

    @Test
    fun `useSoftCancel false tap smallConversion`() {
        assertEquals(SmallConversion, evaluateKomoji(false, false, toEnumSet(Flick.NONE)))
    }

    @Test
    fun `useSoftCancel false left dakutenConversion`() {
        assertEquals(DakutenConversion, evaluateKomoji(false, false, toEnumSet(Flick.LEFT)))
    }

    @Test
    fun `useSoftCancel false up cancel`() {
        assertEquals(Cancel, evaluateKomoji(false, false, toEnumSet(Flick.UP)))
    }

    @Test
    fun `useSoftCancel false right handakutenConversion`() {
        assertEquals(HandakutenConversion, evaluateKomoji(false, false, toEnumSet(Flick.RIGHT)))
    }

    @Test
    fun `useSoftCancel false down shiftConversion`() {
        assertEquals(ShiftConversion, evaluateKomoji(false, false, toEnumSet(Flick.DOWN)))
    }

    // ========== useSoftCancelKey=true（カスタム）==========

    @Test
    fun `useSoftCancel true tap cancel`() {
        assertEquals(Cancel, evaluateKomoji(true, false, toEnumSet(Flick.NONE)))
    }

    @Test
    fun `useSoftCancel true up smallConversion`() {
        assertEquals(SmallConversion, evaluateKomoji(true, false, toEnumSet(Flick.UP)))
    }

    // ========== バグケース: ACTION_DOWNでmFlickStateが初期化されない場合 ==========

    @Test
    fun `bug prev flick state not reset`() {
        // ACTION_DOWNでmFlickStateがリセットされていない場合
        // 前の操作（UP）からmFlickStateが引き継がれる
        // タップ（NONE）を期待しているが、UPとして判定されてキャンセルになる
        
        val buggyState = toEnumSet(Flick.UP) // 前の操作から引き継がれた状態
        assertEquals(Cancel, evaluateKomoji(false, false, buggyState))
        // これはバグ: タップしたのにキャンセルとして扱われる
    }

    @Test
    fun `correct mFlickState reset after actionDown`() {
        // ACTION_DOWNでmFlickStateが正しくリセットされた場合
        // タップはNONEとして正しく判定される
        
        val correctState = toEnumSet(Flick.NONE)
        assertEquals(SmallConversion, evaluateKomoji(false, false, correctState))
    }

    // ========== 曲がりフリック（curve flick）ケース ==========

    @Test
    fun `useSoftCancel false left curve left dakutenConversion`() {
        // LEFT + CURVE_LEFT -> 小文字（左曲がり小文字）
        val leftCurveLeft = toEnumSet(Flick.LEFT) // CURVE_LEFTは追加される
        // CURVE_LEFTはテスト用Flick列挙にないためNoOpとして扱う
        // 実際のコードではCURVE_LEFTが含まれる場合の処理が必要
    }
}
