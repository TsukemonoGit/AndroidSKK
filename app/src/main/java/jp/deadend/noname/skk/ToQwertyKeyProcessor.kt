package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * 全角/半角（とうえー）キーの操作を処理するクラス
 * 
 * TO_QWERTYキーのフリック操作によって異なる入力モードに切り替える
 */
class ToQwertyKeyProcessor {

    /**
     * 全角/半角キーの操作結果を表す列挙型
     */
    enum class ToQwertyAction {
        EMOJI,        // 絵文字Picker
        ZENKAKU,      // 全角入力
        SYMBOL,       // 記号候補
        ASCII,        // ASCII入力
        UNKNOWN       // 不明な操作
    }

    /**
     * 全角/半角キーのフリック状態から操作結果を計算する
     *
     * @param flickState フリック状態
     * @return 計算された操作結果
     */
    fun processToQwertyKey(flickState: EnumSet<FlickState>): ToQwertyAction {
        return when {
            flickState == EnumSet.of(FlickState.LEFT) -> ToQwertyAction.EMOJI
            flickState == EnumSet.of(FlickState.UP) -> ToQwertyAction.ZENKAKU
            flickState == EnumSet.of(FlickState.RIGHT) -> ToQwertyAction.SYMBOL
            flickState == EnumSet.of(FlickState.NONE) -> ToQwertyAction.ASCII
            flickState == EnumSet.of(FlickState.DOWN) -> ToQwertyAction.ASCII
            else -> ToQwertyAction.UNKNOWN
        }
    }

    /**
     * 操作結果から状態名を取得する（デバッグ用）
     *
     * @param action ToQwertyAction
     * @return 状態名
     */
    fun getStateName(action: ToQwertyAction): String {
        return when (action) {
            ToQwertyAction.EMOJI -> "emoji_picker"
            ToQwertyAction.ZENKAKU -> "zenkaku"
            ToQwertyAction.SYMBOL -> "symbol_candidates"
            ToQwertyAction.ASCII -> "ascii"
            ToQwertyAction.UNKNOWN -> "unknown"
        }
    }
}
