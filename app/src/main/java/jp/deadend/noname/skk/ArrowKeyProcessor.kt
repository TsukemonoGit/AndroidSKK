package jp.deadend.noname.skk

/**
 * アローキー（左右）の操作を処理するクラス
 * 
 * 左右アローキーはフリックフラグとSERVICEの処理結果に応じて分岐する
 */
class ArrowKeyProcessor {

    /**
     * アローキーの操作結果を表す列挙型
     */
    enum class ArrowAction {
        HANDLE_DPAD,        // サービスがD-padを処理
        KEY_DOWN_UP,        // サービスが処理できないため直接キーを入力
        NO_OP               // フリック中は何もしない
    }

    /**
     * 左右アローキーの操作結果を計算する
     *
     * @param isArrowFlicked フリック中かどうか
     * @param serviceHandled サービスがD-padを処理したかどうか
     * @param keyEventKeyCode キーイベントコード
     * @return 計算された操作結果
     */
    fun processArrowKey(
        isArrowFlicked: Boolean,
        serviceHandled: Boolean,
        keyEventKeyCode: Int
    ): ArrowAction {
        return when {
            isArrowFlicked -> ArrowAction.NO_OP
            serviceHandled -> ArrowAction.HANDLE_DPAD
            else -> ArrowAction.KEY_DOWN_UP
        }
    }

    /**
     * アローキーの種類を判定する
     *
     * @param keyCode キーコード
     * @return アローキーの種類（LEFT, RIGHT, UNKNOWN）
     */
    fun getArrowDirection(keyCode: Int): ArrowType {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> ArrowType.LEFT
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> ArrowType.RIGHT
            else -> ArrowType.UNKNOWN
        }
    }

    /**
     * アローキーの種類
     */
    enum class ArrowType {
        LEFT,
        RIGHT,
        UNKNOWN
    }
}
