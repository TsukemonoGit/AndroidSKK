package jp.deadend.noname.skk

/**
 * Enterキーの操作を処理するクラス
 * 
 * EnterキーはSERVICEの処理結果に応じて分岐する
 */
class EnterKeyProcessor {

    /**
     * Enterキーの操作結果を表す列挙型
     */
    enum class EnterAction {
        HANDLE_ENTER,   // サービスがEnterを処理
        PRESS_ENTER     // サービスが処理できないため直接Enterを入力
    }

    /**
     * Enterキーの操作結果を計算する
     *
     * @param serviceHandled サービスがEnterを処理したかどうか
     * @return 計算された操作結果
     */
    fun processEnterKey(serviceHandled: Boolean): EnterAction {
        return if (serviceHandled) {
            EnterAction.HANDLE_ENTER
        } else {
            EnterAction.PRESS_ENTER
        }
    }
}
