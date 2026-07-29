package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ShiftToggleProcessorのユニットテスト
 * 
 * シフトキーのトグルロジックをテストする
 * 
 * 期待動作:
 *   - シフトオフ → シフトオン
 *   - シフトオン → シフトオフ
 */
class ShiftToggleProcessorTest {

    private lateinit var processor: ShiftToggleProcessor

    @Before
    fun setUp() {
        processor = ShiftToggleProcessor()
    }

    // ==================== トグルテスト ====================

    @Test
    fun `testToggle_fromOff_toOn()`() {
        // シフトオフ → シフトオン
        val result = processor.toggleShift(false)
        assertEquals(true, result)
    }

    @Test
    fun `testToggle_fromOn_toOff()`() {
        // シフトオン → シフトオフ
        val result = processor.toggleShift(true)
        assertEquals(false, result)
    }

    // ==================== 連続トグルテスト ====================

    @Test
    fun `testToggle_double()`() {
        // 2回連続でトグル → 元の状態に戻る
        var isShifted = false
        isShifted = processor.toggleShift(isShifted)
        assertEquals(true, isShifted)
        isShifted = processor.toggleShift(isShifted)
        assertEquals(false, isShifted)
    }

    @Test
    fun `testToggle_triple()`() {
        // 3回連続でトグル → シフトオンの状態に
        var isShifted = false
        isShifted = processor.toggleShift(isShifted)
        isShifted = processor.toggleShift(isShifted)
        isShifted = processor.toggleShift(isShifted)
        assertEquals(true, isShifted)
    }
}
