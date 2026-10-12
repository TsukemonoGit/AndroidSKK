package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * 文字（もじ）キーの操作を処理するクラス
 * 
 * MOJIキーのフリック操作によって異なる出力を行う
 */
class MojiKeyProcessor {

    /**
     * 文字キーの操作結果を表す列挙型
     */
    enum class MojiAction {
        Q,          // 小文字q
        SHIFTED_Q,  // 大文字Q（シフト状態）
        COLON,      // コロン（:）
        NUMBOARD,   // 数字キーボード
        GREATER,    // 大なり（>）
        VOICEBOARD  // 音声キーボード
    }

    /**
     * 文字キーのフリック状態から操作結果を計算する
     *
     * @param flickState フリック状態
     * @param isShifted シフト状態
     * @return 計算された操作結果
     */
    fun processMojiKey(flickState: EnumSet<FlickState>, isShifted: Boolean): MojiAction {
        return when {
            flickState == EnumSet.of(FlickState.NONE) -> 
                if (isShifted) MojiAction.SHIFTED_Q else MojiAction.Q
            flickState == EnumSet.of(FlickState.LEFT) -> MojiAction.COLON
            flickState == EnumSet.of(FlickState.UP) -> MojiAction.NUMBOARD
            flickState == EnumSet.of(FlickState.RIGHT) -> MojiAction.GREATER
            flickState == EnumSet.of(FlickState.DOWN) -> MojiAction.VOICEBOARD
            else -> MojiAction.Q  // デフォルト
        }
    }

    /**
     * 操作結果からキーコードを取得する（Qキーの場合）
     *
     * @param action MojiAction
     * @return キーコード（-1はキーコード不要）
     */
    fun getKeyCode(action: MojiAction): Int? {
        return when (action) {
            MojiAction.Q -> 'q'.code
            MojiAction.SHIFTED_Q -> 17
            else -> null
        }
    }
}
