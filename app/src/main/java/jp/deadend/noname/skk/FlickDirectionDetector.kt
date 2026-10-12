package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * フリックの方向を検出するクラス
 * 
 * diamondAngle()に基づいて、タッチ座標からフリック方向を判定する
 */
class FlickDirectionDetector {

    companion object {
        // diamondAngleの境界値
        private const val ANGLE_DOWN_LOWER = 0.5f
        private const val ANGLE_DOWN_UPPER = 1.5f
        private const val ANGLE_LEFT_LOWER = 1.5f
        private const val ANGLE_LEFT_UPPER = 2.29f
        private const val ANGLE_CURVE_LEFT_LOWER = 2.29f
        private const val ANGLE_CURVE_LEFT_UPPER = 2.71f
        private const val ANGLE_UP_LOWER = 2.71f
        private const val ANGLE_UP_UPPER = 3.29f
        private const val ANGLE_CURVE_RIGHT_LOWER = 3.29f
        private const val ANGLE_CURVE_RIGHT_UPPER = 3.71f
    }

    /**
     * 最初のフリック（単方向）からフリック状態を計算する
     *
     * @param dx X軸の移動量
     * @param dy Y軸の移動量
     * @param hasLeftCurve 左カーブが既に検出されているか（特別処理用）
     * @param hasRightCurve 右カーブが既に検出されているか（特別処理用）
     * @return 計算されたフリック状態
     */
    fun detectFirstFlick(
        dx: Float,
        dy: Float,
        hasLeftCurve: Boolean = false,
        hasRightCurve: Boolean = false
    ): EnumSet<FlickState> {
        val dAngle = diamondAngle(dx, dy)

        return when (dAngle) {
            in ANGLE_DOWN_LOWER..ANGLE_DOWN_UPPER -> EnumSet.of(FlickState.DOWN)
            in ANGLE_LEFT_LOWER..ANGLE_LEFT_UPPER -> EnumSet.of(FlickState.LEFT)
            in ANGLE_CURVE_LEFT_LOWER..ANGLE_CURVE_LEFT_UPPER -> {
                when {
                    hasLeftCurve -> EnumSet.of(FlickState.NONE, FlickState.CURVE_LEFT)
                    dAngle < 2.5f -> EnumSet.of(FlickState.LEFT)
                    else -> EnumSet.of(FlickState.UP)
                }
            }
            in ANGLE_UP_LOWER..ANGLE_UP_UPPER -> EnumSet.of(FlickState.UP)
            in ANGLE_CURVE_RIGHT_LOWER..ANGLE_CURVE_RIGHT_UPPER -> {
                when {
                    hasRightCurve -> EnumSet.of(FlickState.NONE, FlickState.CURVE_RIGHT)
                    dAngle < 3.5f -> EnumSet.of(FlickState.UP)
                    else -> EnumSet.of(FlickState.RIGHT)
                }
            }
            else -> EnumSet.of(FlickState.RIGHT)
        }
    }

    /**
     * カーブフリックからフリック状態を計算する
     *
     * @param initialFlick 最初のフリック状態（LEFTまたはUPのみ）
     * @param dx X軸の移動量
     * @param dy Y軸の移動量
     * @return 計算されたフリック状態
     */
    fun detectCurveFlick(
        initialFlick: EnumSet<FlickState>,
        dx: Float,
        dy: Float
    ): EnumSet<FlickState> {
        return when {
            initialFlick.contains(FlickState.LEFT) -> {
                when (val angle = diamondAngle(-dx, -dy)) {
                    in 0.45f..2f -> EnumSet.of(FlickState.LEFT, FlickState.CURVE_RIGHT)
                    in 2f..3.55f -> EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT)
                    else -> EnumSet.of(FlickState.LEFT)
                }
            }
            initialFlick.contains(FlickState.UP) -> {
                when (val angle = diamondAngle(-dy, dx)) {
                    in 0.45f..2f -> EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT)
                    in 2f..3.55f -> EnumSet.of(FlickState.UP, FlickState.CURVE_LEFT)
                    else -> EnumSet.of(FlickState.UP)
                }
            }
            initialFlick.contains(FlickState.RIGHT) -> {
                when (val angle = diamondAngle(dx, dy)) {
                    in 0.45f..2f -> EnumSet.of(FlickState.RIGHT, FlickState.CURVE_LEFT)
                    in 2f..3.55f -> EnumSet.of(FlickState.RIGHT, FlickState.CURVE_RIGHT)
                    else -> EnumSet.of(FlickState.RIGHT)
                }
            }
            initialFlick.contains(FlickState.DOWN) -> {
                when (val angle = diamondAngle(dx, -dy)) {
                    in 0.45f..2f -> EnumSet.of(FlickState.DOWN, FlickState.CURVE_LEFT)
                    in 2f..3.55f -> EnumSet.of(FlickState.DOWN, FlickState.CURVE_RIGHT)
                    else -> EnumSet.of(FlickState.DOWN)
                }
            }
            else -> initialFlick
        }
    }

    /**
     * diamondAngleを計算する
     *
     * ダイヤモンド型の座標系で、角度を0〜4の範囲で返す
     * - 0.0〜0.5: 右（RIGHT）
     * - 0.5〜1.5: 下（DOWN）
     * - 1.5〜2.29: 左（LEFT）
     * - 2.29〜2.71: 左上（CURVE_LEFT判定用）
     * - 2.71〜3.29: 上（UP）
     * - 3.29〜3.71: 右上（CURVE_RIGHT判定用）
     * - 3.71〜4.0: 右（RIGHT）
     *
     * @param x X軸座標
     * @param y Y軸座標
     * @return ダイヤモンド角度
     */
    fun diamondAngle(x: Float, y: Float): Float {
        return if (y >= 0) {
            if (x >= 0) y / (x + y) else 1 - x / (-x + y)
        } else {
            if (x < 0) 2 - y / (-x - y) else 3 + x / (x - y)
        }
    }
}
