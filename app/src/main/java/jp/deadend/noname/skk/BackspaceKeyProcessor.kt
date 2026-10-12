package jp.deadend.noname.skk

/**
 * バックスペースキーの操作を処理するクラス
 * 
 * BackspaceキーはSERVICEの処理結果に応じて分岐する
 */
class BackspaceKeyProcessor {

    /**
     * バックスペースキーの操作結果を表す列挙型
     */
    enum class BackspaceAction {
        HANDLE_BACKSPACE,   // サービスがバックスペースを処理
        PRESS_DEL           // サービスが処理できないためDELを入力
    }

    /**
     * バックスペースキーの操作結果を計算する
     *
     * @param serviceHandled サービスがバックスペースを処理したかどうか
     * @return 計算された操作結果
     */
    fun processBackspaceKey(serviceHandled: Boolean): BackspaceAction {
        return if (serviceHandled) {
            BackspaceAction.HANDLE_BACKSPACE
        } else {
            BackspaceAction.PRESS_DEL
        }
    }
}
