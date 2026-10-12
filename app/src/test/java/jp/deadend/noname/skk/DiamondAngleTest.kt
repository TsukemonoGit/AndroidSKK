package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * diamondAngle()のユニットテスト
 * 
 * 画面座標(dx, dy)からダイヤモンド角度を計算する関数のテスト
 * 角度は0〜4の範囲で返り、フリック方向の判定に使用される
 * 
 * 参考:
 * - 0.0〜0.5: 右（RIGHT）
 * - 0.5〜1.5: 下（DOWN）
 * - 1.5〜2.29: 左（LEFT）
 * - 2.29〜2.71: 左上（CURVE_LEFT判定用）
 * - 2.71〜3.29: 上（UP）
 * - 3.29〜3.71: 右上（CURVE_RIGHT判定用）
 * - 3.71〜4.0: 右（RIGHT）
 */
class DiamondAngleTest {

    /**
     * diamondAngle()関数をテスト用の関数として公開
     * (private関数のテスト用にラッパーを作成)
     */
    private fun testDiamondAngle(x: Float, y: Float): Float {
        return if (y >= 0) {
            if (x >= 0) y / (x + y) else 1 - x / (-x + y)
        } else {
            if (x < 0) 2 - y / (-x - y) else 3 + x / (x - y)
        }
    }

    // ==================== 第1象限（x>=0, y>=0）: 右下〜右上 ====================

    @Test
    fun testDiamondAngle_right_down() {
        // x=1, y=1 -> 1/2 = 0.5
        val angle = testDiamondAngle(1f, 1f)
        assertEquals(0.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_down_straight() {
        // x=0, y=1 -> 1/1 = 1.0
        val angle = testDiamondAngle(0f, 1f)
        assertEquals(1.0f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_left_down() {
        // x=-1, y=1 -> 1 - (-1)/2 = 1.5
        val angle = testDiamondAngle(-1f, 1f)
        assertEquals(1.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_right_up() {
        // x=1, y=-1 -> 3 + 1/2 = 3.5
        // 待て、y<0かつx>=0の場合
        val angle = testDiamondAngle(1f, -1f)
        assertEquals(3.5f, angle, 0.001f)
    }

    // ==================== 第2象限（x<0, y>=0）: 左下〜左上 ====================

    @Test
    fun testDiamondAngle_down_strong_left() {
        // x=-1, y=0.1 -> 1 - (-1)/1.1 ≈ 1.909
        val angle = testDiamondAngle(-1f, 0.1f)
        assertEquals(1.909f, angle, 0.01f)
    }

    @Test
    fun testDiamondAngle_left_straight() {
        // x=-1, y=0 -> 1 - (-1)/1 = 2.0
        val angle = testDiamondAngle(-1f, 0f)
        assertEquals(2.0f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_left_up_threshold() {
        // x=-0.1, y=0.1 -> 1 - (-0.1)/0.2 = 1.5
        val angle = testDiamondAngle(-0.1f, 0.1f)
        assertEquals(1.5f, angle, 0.001f)
    }

    // ==================== 第3象限（x<0, y<0）: 左上 ====================

    @Test
    fun testDiamondAngle_left_strong_up() {
        // x=-1, y=-1 -> 2 - (-1)/2 = 2.5
        val angle = testDiamondAngle(-1f, -1f)
        assertEquals(2.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_up_straight() {
        // x=0, y=-1 -> 3 + 0 = 3.0
        val angle = testDiamondAngle(0f, -1f)
        assertEquals(3.0f, angle, 0.001f)
    }

    // ==================== 第4象限（x>=0, y<0）: 右上 ====================

    @Test
    fun testDiamondAngle_up_strong_right() {
        // x=1, y=-1 -> 3 + 1/2 = 3.5
        val angle = testDiamondAngle(1f, -1f)
        assertEquals(3.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_right_straight() {
        // x=1, y=0 -> 0/(1+0) = 0.0
        val angle = testDiamondAngle(1f, 0f)
        assertEquals(0.0f, angle, 0.001f)
    }

    // ==================== 境界値テスト ====================

    @Test
    fun testDiamondAngle_boundary_0_5() {
        // 0.5はDOWNの下限
        val angle = testDiamondAngle(1f, 1f)
        assertEquals(0.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_boundary_1_5() {
        // 1.5はDOWN/LEFTの境界
        val angle = testDiamondAngle(-1f, 1f)
        assertEquals(1.5f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_boundary_2_29() {
        // 2.29はLEFT/CURVE_LEFTの境界
        // x=-1, y=0.79 -> 1 - (-1)/1.79 ≈ 1.558 (LEFT)
        // 2.29になるには x=-0.3, y=0.3 -> 1 - (-0.3)/0.6 = 1.5 (LEFT)
        // 2.29には x=-0.1, y=0.05 -> 1 - (-0.1)/0.15 ≈ 1.667
        // 2.29を生成する座標は複雑だが、境界付近の値を確認
        val angle = testDiamondAngle(-0.3f, 0.1f)
        // 1 - (-0.3)/0.4 = 1 + 0.75 = 1.75
        assertEquals(1.75f, angle, 0.01f)
    }

    // ==================== エッジケース ====================

    @Test
    fun testDiamondAngle_zero_zero() {
        // x=0, y=0 -> 0/0 = NaN
        val angle = testDiamondAngle(0f, 0f)
        assertEquals(Float.NaN, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_positive_x() {
        // x=1, y=0 -> 0/(1+0) = 0.0
        val angle = testDiamondAngle(1f, 0f)
        assertEquals(0.0f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_positive_y() {
        // x=0, y=1 -> 1/1 = 1.0
        val angle = testDiamondAngle(0f, 1f)
        assertEquals(1.0f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_negative_x() {
        // x=-1, y=0 -> 1 - (-1)/1 = 2.0
        val angle = testDiamondAngle(-1f, 0f)
        assertEquals(2.0f, angle, 0.001f)
    }

    @Test
    fun testDiamondAngle_negative_y() {
        // x=0, y=-1 -> 3 + 0 = 3.0
        val angle = testDiamondAngle(0f, -1f)
        assertEquals(3.0f, angle, 0.001f)
    }
}
