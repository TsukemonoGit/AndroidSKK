package jp.deadend.noname.skk

import jp.deadend.noname.skk.engine.SKKHanKanaState

/**
 * かな/ローマ字（トカナ）キーの操作を処理するクラス
 * 
 * TO_KANAキーはキーボードを日本語かな/ローマ字モードに切り替える
 */
class ToKanaKeyProcessor {

    /**
     * かなキーの操作結果を表す列挙型
     */
    enum class ToKanaAction {
        SWITCH_TO_JP,  // 日本語キーボードに切り替え
        NO_OP          // 既に日本語キーボードの場合、何もしない
    }

    /**
     * かなキーのフリック状態から操作結果を計算する
     * 
     * TO_KANAキーはフリック操作に関わらず常に同じ動作を行う
     * （キーボードが日本語キーボードでない場合にのみ切り替え）
     *
     * @param isOnJapaneseKeyboard 現在キーボードが日本語か
     * @return 計算された操作結果
     */
    fun processToKanaKey(isOnJapaneseKeyboard: Boolean): ToKanaAction {
        return if (isOnJapaneseKeyboard) {
            ToKanaAction.NO_OP
        } else {
            ToKanaAction.SWITCH_TO_JP
        }
    }

    /**
     * 切り替え後の半角モード状態を計算する
     * 
     * かな状態がハングアナ（半角）の場合のみ半角モードにする
     *
     * @param kanaState かな状態
     * @return 半角モードフラグ
     */
    fun calculateHankakuState(kanaState: Any?): Boolean {
        return kanaState === SKKHanKanaState
    }
}
