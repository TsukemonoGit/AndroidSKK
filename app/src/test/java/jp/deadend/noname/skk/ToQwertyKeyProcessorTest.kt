package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * ToQwertyKeyProcessorのユニットテスト
 * 
 * 全角/半角（とうえー）キーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - 左フリック（LEFT）→ 絵文字Picker
 *   - 上フリック（UP）→ 全角入力
 *   - 右フリック（RIGHT）→ 記号候補
 *   - タップ（NONE）→ ASCII入力
 *   - 下フリック（DOWN）→ ASCII入力
 */
class ToQwertyKeyProcessorTest {

    private lateinit var processor: ToQwertyKeyProcessor

    @Before
    fun setUp() {
        processor = ToQwertyKeyProcessor()
    }

    // ==================== 左フリック（LEFT）テスト ====================

    @Test
    fun `testToQwerty_left_emojiPicker`() {
        // 左フリック = 絵文字Picker
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.EMOJI, result)
        assertEquals("emoji_picker", processor.getStateName(result))
    }

    // ==================== 上フリック（UP）テスト ====================

    @Test
    fun `testToQwerty_up_zenkaku`() {
        // 上フリック = 全角入力
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.ZENKAKU, result)
        assertEquals("zenkaku", processor.getStateName(result))
    }

    // ==================== 右フリック（RIGHT）テスト ====================

    @Test
    fun `testToQwerty_right_symbolCandidates`() {
        // 右フリック = 記号候補
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.SYMBOL, result)
        assertEquals("symbol_candidates", processor.getStateName(result))
    }

    // ==================== タップ（NONE）テスト ====================

    @Test
    fun `testToQwerty_tap_ascii`() {
        // タップ = ASCII入力
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.ASCII, result)
        assertEquals("ascii", processor.getStateName(result))
    }

    // ==================== 下フリック（DOWN）テスト ====================

    @Test
    fun `testToQwerty_down_ascii`() {
        // 下フリック = ASCII入力（NONEと同じ）
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.ASCII, result)
        assertEquals("ascii", processor.getStateName(result))
    }

    // ==================== バウンド状態テスト ====================

    @Test
    fun `testToQwerty_leftCurveLeft_unknown`() {
        // CURVE_LEFTを含む場合はUNKNOWN
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.UNKNOWN, result)
        assertEquals("unknown", processor.getStateName(result))
    }

    @Test
    fun `testToQwerty_upCurveRight_unknown`() {
        // CURVE_RIGHTを含む場合はUNKNOWN
        val flickState = EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.UNKNOWN, result)
    }

    @Test
    fun `testToQwerty_emptyState_unknown`() {
        // 空の状態はUNKNOWN
        val flickState = EnumSet.noneOf(FlickState::class.java)
        val result = processor.processToQwertyKey(flickState)
        assertEquals(ToQwertyKeyProcessor.ToQwertyAction.UNKNOWN, result)
    }

    // ==================== getStateNameテスト ====================

    @Test
    fun `testStateName_emoji()`() {
        assertEquals("emoji_picker", processor.getStateName(ToQwertyKeyProcessor.ToQwertyAction.EMOJI))
    }

    @Test
    fun `testStateName_zenkaku()`() {
        assertEquals("zenkaku", processor.getStateName(ToQwertyKeyProcessor.ToQwertyAction.ZENKAKU))
    }

    @Test
    fun `testStateName_symbol()`() {
        assertEquals("symbol_candidates", processor.getStateName(ToQwertyKeyProcessor.ToQwertyAction.SYMBOL))
    }

    @Test
    fun `testStateName_ascii()`() {
        assertEquals("ascii", processor.getStateName(ToQwertyKeyProcessor.ToQwertyAction.ASCII))
    }

    @Test
    fun `testStateName_unknown()`() {
        assertEquals("unknown", processor.getStateName(ToQwertyKeyProcessor.ToQwertyAction.UNKNOWN))
    }
}
