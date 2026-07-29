package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * MojiKeyProcessorのユニットテスト
 * 
 * 文字（もじ）キーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - タップ（NONE）→ 'q'（またはシフト状態なら'Q'）
 *   - 左フリック（LEFT）→ ':'
 *   - 上フリック（UP）→ 数字キーボード
 *   - 右フリック（RIGHT）→ '>'
 *   - 下フリック（DOWN）→ 音声キーボード
 */
class MojiKeyProcessorTest {

    private lateinit var processor: MojiKeyProcessor

    @Before
    fun setUp() {
        processor = MojiKeyProcessor()
    }

    // ==================== タップ（NONE）テスト ====================

    @Test
    fun `testMoji_tap_normal_q`() {
        // タップ + シフトオフ = 'q'
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.Q, result)
        assertEquals('q'.code, processor.getKeyCode(result))
    }

    @Test
    fun `testMoji_tap_shifted_q`() {
        // タップ + シフトオン = 'Q' (キーコード17)
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processMojiKey(flickState, true)
        assertEquals(MojiKeyProcessor.MojiAction.SHIFTED_Q, result)
        assertEquals(17, processor.getKeyCode(result))
    }

    // ==================== 左フリック（LEFT）テスト ====================

    @Test
    fun `testMoji_left_colon`() {
        // 左フリック = ':'
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.COLON, result)
        assertNull(processor.getKeyCode(result)) // キーコード不要（直接処理）
    }

    @Test
    fun `testMoji_left_colon_shifted_ignored`() {
        // 左フリックはシフト状態に関係なく':'
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processMojiKey(flickState, true)
        assertEquals(MojiKeyProcessor.MojiAction.COLON, result)
    }

    // ==================== 上フリック（UP）テスト ====================

    @Test
    fun `testMoji_up_numboard`() {
        // 上フリック = 数字キーボード
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.NUMBOARD, result)
        assertNull(processor.getKeyCode(result))
    }

    @Test
    fun `testMoji_up_numboard_shifted_ignored`() {
        // 上フリックはシフト状態に関係なく数字キーボード
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processMojiKey(flickState, true)
        assertEquals(MojiKeyProcessor.MojiAction.NUMBOARD, result)
    }

    // ==================== 右フリック（RIGHT）テスト ====================

    @Test
    fun `testMoji_right_greater`() {
        // 右フリック = '>'
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.GREATER, result)
        assertNull(processor.getKeyCode(result))
    }

    @Test
    fun `testMoji_right_greater_shifted_ignored`() {
        // 右フリックはシフト状態に関係なく'>'
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processMojiKey(flickState, true)
        assertEquals(MojiKeyProcessor.MojiAction.GREATER, result)
    }

    // ==================== 下フリック（DOWN）テスト ====================

    @Test
    fun `testMoji_down_voiceboard`() {
        // 下フリック = 音声キーボード
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.VOICEBOARD, result)
        assertNull(processor.getKeyCode(result))
    }

    @Test
    fun `testMoji_down_voiceboard_shifted_ignored`() {
        // 下フリックはシフト状態に関係なく音声キーボード
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processMojiKey(flickState, true)
        assertEquals(MojiKeyProcessor.MojiAction.VOICEBOARD, result)
    }

    // ==================== バウンド状態テスト ====================

    @Test
    fun `testMoji_leftCurveLeft_default_q`() {
        // CURVE_LEFTを含む場合はデフォルト（Q）
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.Q, result)
    }

    @Test
    fun `testMoji_upCurveRight_default_q`() {
        // CURVE_RIGHTを含む場合はデフォルト（Q）
        val flickState = EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.Q, result)
    }

    @Test
    fun `testMoji_emptyState_default_q`() {
        // 空の状態はデフォルト（Q）
        val flickState = EnumSet.noneOf(FlickState::class.java)
        val result = processor.processMojiKey(flickState, false)
        assertEquals(MojiKeyProcessor.MojiAction.Q, result)
    }

    // ==================== getKeyCodeテスト ====================

    @Test
    fun `testGetCode_q`() {
        assertEquals('q'.code, processor.getKeyCode(MojiKeyProcessor.MojiAction.Q))
    }

    @Test
    fun `testGetCode_shiftedQ`() {
        assertEquals(17, processor.getKeyCode(MojiKeyProcessor.MojiAction.SHIFTED_Q))
    }

    @Test
    fun `testGetCode_colon_null`() {
        assertNull(processor.getKeyCode(MojiKeyProcessor.MojiAction.COLON))
    }

    @Test
    fun `testGetCode_numboard_null`() {
        assertNull(processor.getKeyCode(MojiKeyProcessor.MojiAction.NUMBOARD))
    }

    @Test
    fun `testGetCode_greater_null`() {
        assertNull(processor.getKeyCode(MojiKeyProcessor.MojiAction.GREATER))
    }

    @Test
    fun `testGetCode_voiceboard_null`() {
        assertNull(processor.getKeyCode(MojiKeyProcessor.MojiAction.VOICEBOARD))
    }
}
