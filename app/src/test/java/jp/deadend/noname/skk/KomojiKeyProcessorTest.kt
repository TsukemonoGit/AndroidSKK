package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * KomojiKeyProcessorのユニットテスト
 * 
 * 小（こもじ）キーの操作ロジックをテストする
 * 
 * 期待動作（useSoftCancelKey=false, デフォルト）:
 *   - タップ（NONE）→ 小文字変換 (SMALL)
 *   - 左フリック（LEFT）→ 濁点 (DAKUTEN)
 *   - 上フリック（UP）→ キャンセル (CANCEL)
 *   - 右フリック（RIGHT）→ 半濁点 (HANDAKUTEN)
 *   - 下フリック（DOWN）→ 全角/半角切替 (SHIFT)
 */
class KomojiKeyProcessorTest {

    private lateinit var processor: KomojiKeyProcessor

    @Before
    fun setUp() {
        processor = KomojiKeyProcessor()
    }

    // ==================== デフォルト設定（useSoftCancelKey=false）テスト ====================

    @Test
    fun `testKomoji_default_tap_smallConversion`() {
        // タップ = NONE = 小文字変換
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.SMALL, result)
    }

    @Test
    fun `testKomoji_default_left_dakutenConversion`() {
        // 左フリック = 濁点変換
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.DAKUTEN, result)
    }

    @Test
    fun `testKomoji_default_up_cancel`() {
        // 上フリック = キャンセル
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.CANCEL, result)
    }

    @Test
    fun `testKomoji_default_right_handakutenConversion`() {
        // 右フリック = 半濁点変換
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.HANDAKUTEN, result)
    }

    @Test
    fun `testKomoji_default_down_shiftConversion`() {
        // 下フリック = 全角/半角切替
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.SHIFT, result)
    }

    // ==================== カスタム設定（useSoftCancelKey=true）テスト ====================

    @Test
    fun `testKomoji_softCancel_tap_cancel`() {
        // useSoftCancelKey=trueのタップ = NONE = キャンセル
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processKomojiKey(flickState, true, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.CANCEL, result)
    }

    @Test
    fun `testKomoji_softCancel_up_smallConversion`() {
        // useSoftCancelKey=trueの上フリック = UP = 小文字変換
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processKomojiKey(flickState, true, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.SMALL, result)
    }

    @Test
    fun `testKomoji_softCancel_left_dakutenConversion`() {
        // 左フリック = 濁点変換（設定変更なし）
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processKomojiKey(flickState, true, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.DAKUTEN, result)
    }

    @Test
    fun `testKomoji_softCancel_right_handakutenConversion`() {
        // 右フリック = 半濁点変換（設定変更なし）
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processKomojiKey(flickState, true, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.HANDAKUTEN, result)
    }

    @Test
    fun `testKomoji_softCancel_down_shiftConversion`() {
        // 下フリック = 全角/半角切替（設定変更なし）
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processKomojiKey(flickState, true, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.SHIFT, result)
    }

    // ==================== 連打設定（useSoftTransKey=true）テスト ====================

    @Test
    fun `testKomoji_softTrans_tap_transConversion`() {
        // useSoftCancelKey=false, useSoftTransKey=trueのタップ = TRANS
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processKomojiKey(flickState, false, true)
        assertEquals(KomojiKeyProcessor.KomojiAction.TRANS, result)
    }

    @Test
    fun `testKomoji_softCancel_softTrans_up_transConversion`() {
        // useSoftCancelKey=true, useSoftTransKey=trueの上フリック = TRANS
        // ※ SMALL時にのみTRANS判定（useSoftCancelKey=false && useSoftTransKey=true）
        // useSoftCancelKey=trueの場合はTRANSにならない
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processKomojiKey(flickState, true, true)
        assertEquals(KomojiKeyProcessor.KomojiAction.SMALL, result)
    }

    // ==================== バウンド状態テスト ====================

    @Test
    fun `testKomoji_leftCurveLeft_ignored`() {
        // LEFT + CURVE_LEFT = 未知の操作
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    @Test
    fun `testKomoji_rightCurveRight_ignored`() {
        // RIGHT + CURVE_RIGHT = 未知の操作
        val flickState = EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    @Test
    fun `testKomoji_leftCurveRight_ignored`() {
        // LEFT + CURVE_RIGHT = 未知の操作
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_RIGHT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    @Test
    fun `testKomoji_upCurveLeft_ignored`() {
        // UP + CURVE_LEFT = 未知の操作
        val flickState = EnumSet.of(FlickState.UP, FlickState.CURVE_LEFT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    @Test
    fun `testKomoji_upCurveRight_ignored`() {
        // UP + CURVE_RIGHT = 未知の操作
        val flickState = EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    @Test
    fun `testKomoji_emptyState_unknown`() {
        // 空の状態 = 未知の操作
        val flickState = EnumSet.noneOf(FlickState::class.java)
        val result = processor.processKomojiKey(flickState, false, false)
        assertEquals(KomojiKeyProcessor.KomojiAction.UNKNOWN, result)
    }

    // ==================== calculateKomojiStatesテスト ====================

    @Test
    fun `testCalculateStates_default`() {
        // useSoftCancelKey=false: (NONE, UP)
        val (smallState, cancelState) = processor.calculateKomojiStates(false)
        assertEquals(FlickState.NONE, smallState)
        assertEquals(FlickState.UP, cancelState)
    }

    @Test
    fun `testCalculateStates_softCancel`() {
        // useSoftCancelKey=true: (UP, NONE)
        val (smallState, cancelState) = processor.calculateKomojiStates(true)
        assertEquals(FlickState.UP, smallState)
        assertEquals(FlickState.NONE, cancelState)
    }
}
