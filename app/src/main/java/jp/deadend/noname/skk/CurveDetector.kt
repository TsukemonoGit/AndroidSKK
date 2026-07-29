package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * カーブフリックを検出するクラス
 * 
 * フリック状態からカーブ方向を判定する
 */
class CurveDetector {

    /**
     * 左カーブフリックかどうか判定する
     *
     * @param flickState フリック状態
     * @return 左カーブフラグ
     */
    fun isLeftCurve(flickState: EnumSet<FlickState>): Boolean {
        return flickState.contains(FlickState.CURVE_LEFT)
    }

    /**
     * 右カーブフリックかどうか判定する
     *
     * @param flickState フリック状態
     * @return 右カーブフラグ
     */
    fun isRightCurve(flickState: EnumSet<FlickState>): Boolean {
        return flickState.contains(FlickState.CURVE_RIGHT)
    }

    /**
     * カーブフリックかどうか判定する（左右どちらか）
     *
     * @param flickState フリック状態
     * @return カーブフラグ
     */
    fun isCurve(flickState: EnumSet<FlickState>): Boolean {
        return isLeftCurve(flickState) || isRightCurve(flickState)
    }
}
