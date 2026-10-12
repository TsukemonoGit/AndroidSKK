package jp.deadend.noname.skk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * CurveDetectorのユニットテスト
 * 
 * カーブフリック検出ロジックをテストする
 * 
 * 期待動作:
 *   - CURVE_LEFTを含む → isLeftCurve=true
 *   - CURVE_RIGHTを含む → isRightCurve=true
 *   - どちらかを含む → isCurve=true
 */
class CurveDetectorTest {

    private lateinit var detector: CurveDetector

    @Before
    fun setUp() {
        detector = CurveDetector()
    }

    // ==================== 左カーブテスト ====================

    @Test
    fun testLeftCurve_withCURVE_LEFT() {
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        assertTrue(detector.isLeftCurve(flickState))
        assertFalse(detector.isRightCurve(flickState))
        assertTrue(detector.isCurve(flickState))
    }

    @Test
    fun testLeftCurve_noCURVE_LEFT() {
        val flickState = EnumSet.of(FlickState.LEFT)
        assertFalse(detector.isLeftCurve(flickState))
        assertFalse(detector.isRightCurve(flickState))
        assertFalse(detector.isCurve(flickState))
    }

    // ==================== 右カーブテスト ====================

    @Test
    fun testRightCurve_withCURVE_RIGHT() {
        val flickState = EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT)
        assertFalse(detector.isLeftCurve(flickState))
        assertTrue(detector.isRightCurve(flickState))
        assertTrue(detector.isCurve(flickState))
    }

    @Test
    fun testRightCurve_noCURVE_RIGHT() {
        val flickState = EnumSet.of(FlickState.RIGHT)
        assertFalse(detector.isLeftCurve(flickState))
        assertFalse(detector.isRightCurve(flickState))
        assertFalse(detector.isCurve(flickState))
    }

    // ==================== カーブテスト ====================

    @Test
    fun testCurve_leftOnly() {
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
        assertTrue(detector.isCurve(flickState))
    }

    @Test
    fun testCurve_rightOnly() {
        val flickState = EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT)
        assertTrue(detector.isCurve(flickState))
    }

    @Test
    fun testCurve_both() {
        val flickState = EnumSet.of(FlickState.CURVE_LEFT, FlickState.CURVE_RIGHT)
        assertTrue(detector.isCurve(flickState))
    }

    @Test
    fun testCurve_none() {
        val flickState = EnumSet.of(FlickState.NONE)
        assertFalse(detector.isCurve(flickState))
    }

    @Test
    fun testCurve_empty() {
        val flickState = EnumSet.noneOf(FlickState::class.java)
        assertFalse(detector.isCurve(flickState))
    }

    // ==================== 複合状態テスト ====================

    @Test
    fun testLeftCurve_withMultipleStates() {
        val flickState = EnumSet.of(FlickState.LEFT, FlickState.UP, FlickState.CURVE_LEFT)
        assertTrue(detector.isLeftCurve(flickState))
    }

    @Test
    fun testRightCurve_withMultipleStates() {
        val flickState = EnumSet.of(FlickState.RIGHT, FlickState.DOWN, FlickState.CURVE_RIGHT)
        assertTrue(detector.isRightCurve(flickState))
    }
}
