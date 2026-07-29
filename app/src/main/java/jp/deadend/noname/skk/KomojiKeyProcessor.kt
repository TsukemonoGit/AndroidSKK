package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * 小（こもじ）キーの操作を処理するクラス
 * 
 * KOMOJIキーのフリック操作によって異なる変換を行う
 */
class KomojiKeyProcessor {

    /**
     * 小キーの操作結果を表す列挙型
     */
    enum class KomojiAction {
        SMALL,       // 小文字変換
        TRANS,       // 変換交換（連打時の濁点/半濁点）
        DAKUTEN,     // 濁点
        CANCEL,      // キャンセル
        HANDAKUTEN,  // 半濁点
        SHIFT,       // 全角/半角切替
        UNKNOWN      // 不明な操作
    }

    /**
     * 小キーのフリック状態から操作結果を計算する
     *
     * @param flickState フリック状態
     * @param useSoftCancelKey キャンセルキーの代わりに小文字キーを使う設定
     * @param useSoftTransKey 小文字キーが連打で濁点/半濁点にもなる設定
     * @return 計算された操作結果
     */
    fun processKomojiKey(
        flickState: EnumSet<FlickState>,
        useSoftCancelKey: Boolean,
        useSoftTransKey: Boolean
    ): KomojiAction {
        val smallState = if (useSoftCancelKey) FlickState.UP else FlickState.NONE
        val cancelState = if (useSoftCancelKey) FlickState.NONE else FlickState.UP

        val smallEnumSet = EnumSet.of(smallState)
        val cancelEnumSet = EnumSet.of(cancelState)

        return when {
            flickState == smallEnumSet -> {
                if (!useSoftCancelKey && useSoftTransKey) {
                    KomojiAction.TRANS
                } else {
                    KomojiAction.SMALL
                }
            }
            flickState == EnumSet.of(FlickState.LEFT) -> KomojiAction.DAKUTEN
            flickState == cancelEnumSet -> KomojiAction.CANCEL
            flickState == EnumSet.of(FlickState.RIGHT) -> KomojiAction.HANDAKUTEN
            flickState == EnumSet.of(FlickState.DOWN) -> KomojiAction.SHIFT
            else -> KomojiAction.UNKNOWN
        }
    }

    /**
     * 小キーの動作設定を計算する
     *
     * @param useSoftCancelKey キャンセルキーの代わりに小文字キーを使う設定
     * @return 小文字状態とキャンセル状態のペア
     */
    fun calculateKomojiStates(useSoftCancelKey: Boolean): Pair<FlickState, FlickState> {
        return if (useSoftCancelKey) {
            FlickState.UP to FlickState.NONE  // (smallState, cancelState)
        } else {
            FlickState.NONE to FlickState.UP
        }
    }
}
