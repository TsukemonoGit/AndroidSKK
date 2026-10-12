package jp.deadend.noname.skk

/**
 * フリックの状態を表す列挙型
 * 
 * NONE: タップ（フリックなし）
 * LEFT/UP/RIGHT/DOWN: 各方向へのフリック
 * CURVE_LEFT/CURVE_RIGHT: 左/右カーブ
 */
enum class FlickState {
    NONE,
    LEFT,
    UP,
    RIGHT,
    DOWN,
    CURVE_LEFT,
    CURVE_RIGHT
}
