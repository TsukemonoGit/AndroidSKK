package jp.deadend.noname.skk

/**
 * 句読点キーのポップアップラベル設定を管理するクラス
 * 
 * 句読点設定タイプによって異なるラベル文字列を生成する
 */
class TenKeyLabelConfig {

    /**
     * 句読点タイプに対応するラベル設定を取得する
     */
    data class LabelConfig(
        val mainLabel: String,
        val flickGuide: Array<String>
    )

    /**
     * 句読点タイプからラベル設定を生成する
     *
     * @param kutoutenType 句読点タイプ（"en", "jp_en", その他）
     * @return ラベル設定
     */
    fun getTenKeyLabelConfig(kutoutenType: String?): LabelConfig {
        return when (kutoutenType) {
            "en" -> LabelConfig(
                mainLabel = "？\n．，！\n…",
                flickGuide = arrayOf("，", "．", "？", "！", "…", "", "")
            )
            "jp_en" -> LabelConfig(
                mainLabel = "？\n。，！\n…",
                flickGuide = arrayOf("，", "。", "？", "！", "…", "", "")
            )
            else -> LabelConfig(
                mainLabel = "？\n。、！\n…",
                flickGuide = arrayOf("、", "。", "？", "！", "…", "", "")
            )
        }
    }
}
