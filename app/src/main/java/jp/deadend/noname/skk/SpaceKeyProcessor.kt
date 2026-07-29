package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * スペースキーの操作を処理するクラス
 * 
 * スペースキーはシフト状態とフリック状態によって異なる動作を行う
 */
class SpaceKeyProcessor {

    /**
     * スペースキーの操作結果を表す列挙型
     */
    enum class SpaceAction {
        OPEN_SETTINGS,    // 設定画面を開く（シフト+スペース）
        INPUT_SPACE,      // スペースを入力（タップ）
        NO_OP             // 何もしない（無効な状態）
    }

    /**
     * スペースキーのフリック状態から操作結果を計算する
     *
     * @param isShifted シフト状態
     * @param flickState フリック状態
     * @return 計算された操作結果
     */
    fun processSpaceKey(isShifted: Boolean, flickState: EnumSet<FlickState>): SpaceAction {
        return when {
            isShifted -> SpaceAction.OPEN_SETTINGS
            flickState == EnumSet.of(FlickState.NONE) -> SpaceAction.INPUT_SPACE
            else -> SpaceAction.NO_OP
        }
    }
}
