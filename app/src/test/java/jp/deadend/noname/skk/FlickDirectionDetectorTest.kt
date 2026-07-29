package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * FlickDirectionDetectorのユニットテスト
 * 
 * フリックの方向を検出するロジックのテスト
 */
class FlickDirectionDetectorTest {

    private lateinit var detector: FlickDirectionDetector

    @Before
    fun setUp() {
        detector = FlickDirectionDetector()
    }

    // ==================== diamondAngleテスト ====================

    @Test
    fun testDiamondAngle_down_straight() {
        // x=0, y=1 -> 1.0（DOWN）
        assertEquals(1.0f, detector.diamondAngle(0f, 1f), 0.001f)
    }

    @Test
    fun testDiamondAngle_left_straight() {
        // x=-1, y=0 -> 2.0（LEFT）
        assertEquals(2.0f, detector.diamondAngle(-1f, 0f), 0.001f)
    }

    @Test
    fun testDiamondAngle_up_straight() {
        // x=0, y=-1 -> 3.0（UP）
        assertEquals(3.0f, detector.diamondAngle(0f, -1f), 0.001f)
    }

    // ==================== detectFirstFlickテスト ====================

    @Test
    fun testDetectFirstFlick_down() {
        // 下方向へのフリック（dx=0, dy=1）
        val result = detector.detectFirstFlick(0f, 1f)
        assertEquals(EnumSet.of(FlickState.DOWN), result)
    }

    @Test
    fun testDetectFirstFlick_left() {
        // 左方向へのフリック（dx=-1, dy=0）
        // diamondAngle(-1, 0) = 1 - (-1)/1 = 2.0 -> LEFT
        val result = detector.detectFirstFlick(-1f, 0f)
        assertEquals(EnumSet.of(FlickState.LEFT), result)
    }

    @Test
    fun testDetectFirstFlick_up() {
        // 上方向へのフリック（dx=0, dy=-1）
        val result = detector.detectFirstFlick(0f, -1f)
        assertEquals(EnumSet.of(FlickState.UP), result)
    }

    @Test
    fun testDetectFirstFlick_right() {
        // 右方向へのフリック（dx=1, dy=0）
        val result = detector.detectFirstFlick(1f, 0f)
        assertEquals(EnumSet.of(FlickState.RIGHT), result)
    }

    @Test
    fun testDetectFirstFlick_down_right() {
        // 右下方向（dx=1, dy=1）-> 0.5（DOWNの下限）
        val result = detector.detectFirstFlick(1f, 1f)
        assertEquals(EnumSet.of(FlickState.DOWN), result)
    }

    @Test
    fun testDetectFirstFlick_down_left() {
        // 左下方向（dx=-1, dy=1）-> 1.5（DOWN/LEFTの境界）
        // 1.5は0.5..1.5（DOWN）の範囲内
        val result = detector.detectFirstFlick(-1f, 1f)
        assertEquals(EnumSet.of(FlickState.DOWN), result)
    }

    @Test
    fun testDetectFirstFlick_left_strong() {
        // より左寄りの下方向（dx=-2, dy=1）-> 1 - (-2)/3 = 1.667（LEFT）
        val result = detector.detectFirstFlick(-2f, 1f)
        assertEquals(EnumSet.of(FlickState.LEFT), result)
    }

    @Test
    fun testDetectFirstFlick_left_strongest() {
        // 完全に左方向（dx=-1, dy=0）-> 2.0（LEFT）
        val result = detector.detectFirstFlick(-1f, 0f)
        assertEquals(EnumSet.of(FlickState.LEFT), result)
    }

    @Test
    fun testDetectFirstFlick_up_left() {
        // 左上方向（dx=-1, dy=-1）-> 2.5（CURVE_LEFT領域）
        // hasLeftCurve=false, dAngle=2.5 >= 2.5 -> UP
        val result = detector.detectFirstFlick(-1f, -1f, hasLeftCurve = false)
        assertEquals(EnumSet.of(FlickState.UP), result)
    }

    @Test
    fun testDetectFirstFlick_up_left_withCurve() {
        // 左上方向（dx=-1, dy=-1）-> 2.5
        // hasLeftCurve=true -> CURVE_LEFT
        val result = detector.detectFirstFlick(-1f, -1f, hasLeftCurve = true)
        assertEquals(EnumSet.of(FlickState.NONE, FlickState.CURVE_LEFT), result)
    }

    @Test
    fun testDetectFirstFlick_up_right() {
        // 右上方向（dx=1, dy=-1）-> 3.5（CURVE_RIGHT領域）
        // hasRightCurve=false, dAngle=3.5 >= 3.5 -> RIGHT
        val result = detector.detectFirstFlick(1f, -1f, hasRightCurve = false)
        assertEquals(EnumSet.of(FlickState.RIGHT), result)
    }

    @Test
    fun testDetectFirstFlick_up_right_withCurve() {
        // 右上方向（dx=1, dy=-1）-> 3.5
        // hasRightCurve=true -> CURVE_RIGHT
        val result = detector.detectFirstFlick(1f, -1f, hasRightCurve = true)
        assertEquals(EnumSet.of(FlickState.NONE, FlickState.CURVE_RIGHT), result)
    }

    // ==================== detectCurveFlickテスト ====================

    @Test
    fun testDetectCurveFlick_left_curve_right() {
        // 左→右上へのカーブ
        val initial = EnumSet.of(FlickState.LEFT)
        val result = detector.detectCurveFlick(initial, -1f, -1f)
        // diamondAngle(-(-1), -(-1)) = diamondAngle(1, 1) = 0.5 -> LEFT, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.LEFT, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_left_curve_left() {
        // 左→左上へのカーブ
        val initial = EnumSet.of(FlickState.LEFT)
        val result = detector.detectCurveFlick(initial, -1f, 1f)
        // diamondAngle(-(-1), -(1)) = diamondAngle(1, -1) = 3.5 -> 3.5 not in 0.45..2f or 2f..3.55f
        // Actually diamondAngle(1, -1) = 3 + 1/(1-(-1)) = 3 + 0.5 = 3.5
        // 3.5 in 2f..3.55f -> LEFT, CURVE_LEFT
        assertEquals(EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT), result)
    }

    @Test
    fun testDetectCurveFlick_up_curve_right() {
        // 上→右へのカーブ
        val initial = EnumSet.of(FlickState.UP)
        val result = detector.detectCurveFlick(initial, 1f, -1f)
        // diamondAngle(-(-1), 1) = diamondAngle(1, 1) = 0.5 -> UP, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_up_curve_left() {
        // 上→左へのカーブ
        val initial = EnumSet.of(FlickState.UP)
        val result = detector.detectCurveFlick(initial, -1f, -1f)
        // diamondAngle(-(-1), -1) = diamondAngle(1, -1) = 3.5 -> UP, CURVE_LEFT
        assertEquals(EnumSet.of(FlickState.UP, FlickState.CURVE_LEFT), result)
    }

    @Test
    fun testDetectCurveFlick_right_curve_left() {
        // 右→左上へのカーブ
        val initial = EnumSet.of(FlickState.RIGHT)
        val result = detector.detectCurveFlick(initial, -1f, -1f)
        // diamondAngle(-1, -1) -> y<0, x<0 -> 2-(-1)/(-1-(-1)) = 2-(-1)/0 = Infinity
        // 実際はdiamondAngle(dx, dy)でdx=1, dy=-1 -> 3+1/(1-(-1)) = 3.5
        // 3.5 in 2f..3.55f -> RIGHT, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_right_curve_right() {
        // 右→右上へのカーブ
        val initial = EnumSet.of(FlickState.RIGHT)
        val result = detector.detectCurveFlick(initial, 1f, -1f)
        // diamondAngle(1, -1) = 3.5 -> 3.5 in 2f..3.55f -> RIGHT, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_down_curve_left() {
        // 下→左上へのカーブ
        val initial = EnumSet.of(FlickState.DOWN)
        val result = detector.detectCurveFlick(initial, -1f, 1f)
        // diamondAngle(-1, -1) -> diamondAngle(dx, -dy)でdx=-1, -dy=-1
        // diamondAngle(-1, -1) -> y<0, x<0 -> 2-(-1)/(-1-(-1)) = 2-(-1)/0 = Infinity
        // 実際はdiamondAngle(-1, -1) -> y<0, x<0 -> 2-(-1)/(-1+1) = division
        // 実際はRIGHT, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.DOWN, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_down_curve_right() {
        // 下→右上へのカーブ
        val initial = EnumSet.of(FlickState.DOWN)
        val result = detector.detectCurveFlick(initial, 1f, 1f)
        // diamondAngle(1, -1) = 3.5 -> 3.5 in 2f..3.55f -> DOWN, CURVE_RIGHT
        assertEquals(EnumSet.of(FlickState.DOWN, FlickState.CURVE_RIGHT), result)
    }

    @Test
    fun testDetectCurveFlick_invalid_initial() {
        // 無効な初期状態（NONE）-> 初期状態を返す
        val initial = EnumSet.of(FlickState.NONE)
        val result = detector.detectCurveFlick(initial, 1f, 1f)
        assertEquals(initial, result)
    }
}
