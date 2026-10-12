package jp.deadend.noname.skk

/**
 * シフトキーのトグル操作を処理するクラス
 * 
 * SHIFTキーは押すごとにシフト状態を切り替える
 */
class ShiftToggleProcessor {

    /**
     * シフトキー押下後の新しいシフト状態を計算する
     *
     * @param currentIsShifted 現在のシフト状態
     * @return 新しいシフト状態
     */
    fun toggleShift(currentIsShifted: Boolean): Boolean {
        return !currentIsShifted
    }
}
