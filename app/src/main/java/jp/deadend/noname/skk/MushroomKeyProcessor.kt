package jp.deadend.noname.skk

import java.util.EnumSet

/**
 * マッシュルーム（花丸）キーの操作を処理するクラス
 * 
 * スペースキーの上フリックでマッシュルーム（褒め表現）を送信する
 */
class MushroomKeyProcessor {

    /**
     * マッシュルームの操作結果を表す列挙型
     */
    enum class MushroomAction {
        SEND_MUSHROOM,    // マッシュルームを送信
        NO_OP             // マッシュルームを送信しない
    }

    /**
     * マッシュルームキーのフリック状態から操作結果を計算する
     *
     * @param flickState フリック状態
     * @return 計算された操作結果
     */
    fun processMushroomKey(flickState: EnumSet<FlickState>): MushroomAction {
        return if (flickState == EnumSet.of(FlickState.UP)) {
            MushroomAction.SEND_MUSHROOM
        } else {
            MushroomAction.NO_OP
        }
    }
}
