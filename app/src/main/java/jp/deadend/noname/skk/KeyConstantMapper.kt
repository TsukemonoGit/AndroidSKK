package jp.deadend.noname.skk

/**
 * キーコード定数のマッパー
 * 
 * onKey()で直接SERVICEに渡すキーコードを管理する
 */
class KeyConstantMapper {

    /**
     * onKey()で直接SERVICEに渡すキーコードの集合
     */
    companion object {
        val DIRECT_SERVICE_KEYS = setOf(
            33,   // !
            40,   // (
            41,   // )
            44,   // ,
            46,   // .
            48,   // 0
            49,   // 1
            50,   // 2
            51,   // 3
            52,   // 4
            53,   // 5
            54,   // 6
            55,   // 7
            56,   // 8
            57,   // 9
            63,   // ?
            91,   // [
            93    // ]
        )

        /**
         * キーコードがSERVICEに直接渡すキーかどうか
         */
        fun isDirectServiceKey(keyCode: Int): Boolean {
            return keyCode in DIRECT_SERVICE_KEYS
        }

        /**
         * キーコードが数字かどうか
         */
        fun isNumericKey(keyCode: Int): Boolean {
            return keyCode in 48..57
        }

        /**
         * キーコードが記号かどうか
         */
        fun isSymbolKey(keyCode: Int): Boolean {
            return keyCode in DIRECT_SERVICE_KEYS && !isNumericKey(keyCode)
        }
    }
}
