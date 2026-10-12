package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet
import jp.deadend.noname.skk.engine.SKKEngine

/**
 * FlickKeyProcessorのユニットテスト
 * 
 * FlickJPKeyboardViewからロジックを抽出した純粋な関数のテスト
 */
class FlickKeyProcessorTest {

    private lateinit var processor: FlickKeyProcessor

    @Before
    fun setUp() {
        processor = FlickKeyProcessor()
    }

    // ==================== getVowelForFlickテスト ====================

    @Test
    fun testGetVowelForFlick_none_returns_a() {
        val flick = EnumSet.of(FlickState.NONE)
        assertEquals('a'.code, processor.getVowelForFlick(flick))
    }

    @Test
    fun testGetVowelForFlick_left_returns_i() {
        val flick = EnumSet.of(FlickState.LEFT)
        assertEquals('i'.code, processor.getVowelForFlick(flick))
    }

    @Test
    fun testGetVowelForFlick_up_returns_u() {
        val flick = EnumSet.of(FlickState.UP)
        assertEquals('u'.code, processor.getVowelForFlick(flick))
    }

    @Test
    fun testGetVowelForFlick_right_returns_e() {
        val flick = EnumSet.of(FlickState.RIGHT)
        assertEquals('e'.code, processor.getVowelForFlick(flick))
    }

    @Test
    fun testGetVowelForFlick_down_returns_o() {
        val flick = EnumSet.of(FlickState.DOWN)
        assertEquals('o'.code, processor.getVowelForFlick(flick))
    }

    // ==================== processAKeyテスト ====================

    @Test
    fun testProcessAKey_normal_hiragana() {
        val flick = EnumSet.of(FlickState.UP) // u
        val result = processor.processAKey('u'.code, flick, false, false).toList()
        assertEquals(listOf('u'.code), result)
    }

    @Test
    fun testProcessAKey_shifted() {
        val flick = EnumSet.of(FlickState.UP) // u
        val result = processor.processAKey('u'.code, flick, false, true).toList()
        assertEquals(listOf('U'.code), result)
    }

    @Test
    fun testProcessAKey_leftCurve() {
        val flick = EnumSet.of(FlickState.CURVE_LEFT, FlickState.UP)
        val result = processor.processAKey('u'.code, flick, true, false).toList()
        assertEquals(listOf('x'.code, 'u'.code), result)
    }

    // ==================== processKanaKeyテスト ====================

    @Test
    fun testProcessKanaKey_normal() {
        val flick = EnumSet.of(FlickState.UP) // u
        val result = processor.processKanaKey('k'.code, flick, false, false).toList()
        assertEquals(listOf('k'.code, 'u'.code), result)
    }

    @Test
    fun testProcessKanaKey_shifted() {
        val flick = EnumSet.of(FlickState.UP) // u
        val result = processor.processKanaKey('k'.code, flick, false, true).toList()
        assertEquals(listOf('K'.code, 'u'.code), result)
    }

    @Test
    fun testProcessKanaKey_t_leftCurve_returns_small_tsu() {
        val flick = EnumSet.of(FlickState.LEFT) // i
        val result = processor.processKanaKey('t'.code, flick, true, false).toList()
        // t + i + LAST_CONVERSION_SMALL
        assertEquals(3, result.size)
        assertEquals('t'.code, result[0])
        assertEquals('i'.code, result[1])
        assertEquals(SKKEngine.LAST_CONVERSION_SMALL, result[2])
    }

    // ==================== processYaKeyテスト ====================

    @Test
    fun testProcessYaKey_normal() {
        val flick = EnumSet.of(FlickState.UP) // u
        val result = processor.processYaKey(flick, false, false).toList()
        assertEquals(listOf('y'.code, 'u'.code), result)
    }

    @Test
    fun testProcessYaKey_left_noCurve() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processYaKey(flick, false, false).toList()
        assertEquals(listOf('(' .code), result)
    }

    @Test
    fun testProcessYaKey_left_withCurve() {
        val flick = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        val result = processor.processYaKey(flick, true, false).toList()
        assertEquals(listOf('[' .code), result)
    }

    @Test
    fun testProcessYaKey_right_noCurve() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processYaKey(flick, false, false).toList()
        assertEquals(listOf(')' .code), result)
    }

    @Test
    fun testProcessYaKey_right_withCurve() {
        val flick = EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT)
        val result = processor.processYaKey(flick, true, true).toList()
        assertEquals(listOf('z'.code, ']'.code), result)
    }

    // ==================== processWaKeyテスト ====================

    @Test
    fun testProcessWaKey_none() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('w'.code, 'a'.code), result)
    }

    @Test
    fun testProcessWaKey_none_shifted() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processWaKey(flick, true).toList()
        assertEquals(listOf('W'.code, 'a'.code), result)
    }

    @Test
    fun testProcessWaKey_curveLeft() {
        val flick = EnumSet.of(FlickState.NONE, FlickState.CURVE_LEFT)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('x'.code, 'w'.code, 'a'.code), result)
    }

    @Test
    fun testProcessWaKey_left() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('w'.code, 'o'.code), result)
    }

    @Test
    fun testProcessWaKey_up() {
        val flick = EnumSet.of(FlickState.UP)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('n'.code, 'n'.code), result)
    }

    @Test
    fun testProcessWaKey_right() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('-'.code), result)
    }

    @Test
    fun testProcessWaKey_down() {
        val flick = EnumSet.of(FlickState.DOWN)
        val result = processor.processWaKey(flick, false).toList()
        assertEquals(listOf('~'.code), result)
    }

    // ==================== processTenKeyテスト ====================

    @Test
    fun testProcessTenKey_none() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processTenKey(flick).toList()
        assertEquals(listOf('、'.code), result)
    }

    @Test
    fun testProcessTenKey_left() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processTenKey(flick).toList()
        assertEquals(listOf('。'.code), result)
    }

    @Test
    fun testProcessTenKey_up() {
        val flick = EnumSet.of(FlickState.UP)
        val result = processor.processTenKey(flick).toList()
        assertEquals(listOf('？'.code), result)
    }

    @Test
    fun testProcessTenKey_right() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processTenKey(flick).toList()
        assertEquals(listOf('！'.code), result)
    }

    @Test
    fun testProcessTenKey_down() {
        val flick = EnumSet.of(FlickState.DOWN)
        val result = processor.processTenKey(flick).toList()
        assertEquals(listOf('z'.code, '。'.code), result)
    }

    // ==================== processTenShiftedKeyテスト ====================

    @Test
    fun testProcessTenShiftedKey_none() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processTenShiftedKey(flick).toList()
        assertEquals(listOf(' '.code), result)
    }

    @Test
    fun testProcessTenShiftedKey_left() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processTenShiftedKey(flick).toList()
        assertEquals(listOf('（'.code), result)
    }

    @Test
    fun testProcessTenShiftedKey_up() {
        val flick = EnumSet.of(FlickState.UP)
        val result = processor.processTenShiftedKey(flick).toList()
        assertEquals(listOf('「'.code), result)
    }

    @Test
    fun testProcessTenShiftedKey_right() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processTenShiftedKey(flick).toList()
        assertEquals(listOf('）'.code), result)
    }

    @Test
    fun testProcessTenShiftedKey_down() {
        val flick = EnumSet.of(FlickState.DOWN)
        val result = processor.processTenShiftedKey(flick).toList()
        assertEquals(listOf('」'.code), result)
    }

    // ==================== processTenNumKeyテスト ====================

    @Test
    fun testProcessTenNumKey_none() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processTenNumKey(flick).toList()
        assertEquals(listOf("，"), result)
    }

    @Test
    fun testProcessTenNumKey_left() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processTenNumKey(flick).toList()
        assertEquals(listOf("．"), result)
    }

    @Test
    fun testProcessTenNumKey_up() {
        val flick = EnumSet.of(FlickState.UP)
        val result = processor.processTenNumKey(flick).toList()
        assertEquals(listOf("－"), result)
    }

    @Test
    fun testProcessTenNumKey_right() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processTenNumKey(flick).toList()
        assertEquals(listOf("："), result)
    }

    @Test
    fun testProcessTenNumKey_down() {
        val flick = EnumSet.of(FlickState.DOWN)
        val result = processor.processTenNumKey(flick).toList()
        assertEquals(listOf("／"), result)
    }

    // ==================== processTenNumLeftKeyテスト ====================

    @Test
    fun testProcessTenNumLeftKey_none() {
        val flick = EnumSet.of(FlickState.NONE)
        val result = processor.processTenNumLeftKey(flick).toList()
        assertEquals(listOf("＃"), result)
    }

    @Test
    fun testProcessTenNumLeftKey_left() {
        val flick = EnumSet.of(FlickState.LEFT)
        val result = processor.processTenNumLeftKey(flick).toList()
        assertEquals(listOf("￥"), result)
    }

    @Test
    fun testProcessTenNumLeftKey_up() {
        val flick = EnumSet.of(FlickState.UP)
        val result = processor.processTenNumLeftKey(flick).toList()
        assertEquals(listOf("＋"), result)
    }

    @Test
    fun testProcessTenNumLeftKey_right() {
        val flick = EnumSet.of(FlickState.RIGHT)
        val result = processor.processTenNumLeftKey(flick).toList()
        assertEquals(listOf("＄"), result)
    }

    @Test
    fun testProcessTenNumLeftKey_down() {
        val flick = EnumSet.of(FlickState.DOWN)
        val result = processor.processTenNumLeftKey(flick).toList()
        assertEquals(listOf("＊"), result)
    }
}
