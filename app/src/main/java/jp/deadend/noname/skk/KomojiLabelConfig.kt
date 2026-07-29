package jp.deadend.noname.skk

/**
 * 小キー（KOMOJI）のポップアップラベル設定を管理するクラス
 * 
 * 設定（softCancelKey/softTransKey）によって異なるラベルを生成する
 */
class KomojiLabelConfig {

    /**
     * 小キーのポップアップラベル設定を取得する
     */
    data class LabelConfig(
        val mainLabel: String,
        val flickGuide: Array<String>
    )

    /**
     * 小キー設定からラベル設定を生成する
     *
     * @param useSoftCancelKey ソフトキャンセルキーを使用するか
     * @param useSoftTransKey ソフト変換キーを使用するか
     * @return ラベル設定
     */
    fun getKomojiLabelConfig(
        useSoftCancelKey: Boolean,
        useSoftTransKey: Boolean
    ): LabelConfig {
        return when {
            useSoftCancelKey -> LabelConfig(
                mainLabel = "小\n ◻゙CXL◻゚ \n▽",
                flickGuide = arrayOf("CXL", "◻゙", "小", "◻゚", "▽", "", "")
            )
            useSoftTransKey -> LabelConfig(
                mainLabel = "CXL\n ◻゙□゚ \n▽",
                flickGuide = arrayOf("◻゙□゚", "◻゙", "CXL", "◻゚", "▽", "", "")
            )
            else -> LabelConfig(
                mainLabel = "CXL\n ◻゙小◻゚ \n▽",
                flickGuide = arrayOf("小", "◻゙", "CXL", "◻゚", "▽", "", "")
            )
        }
    }
}
