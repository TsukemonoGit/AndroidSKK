package jp.deadend.noname.skk

/**
 * ポップアップガイドの設定を管理するクラス
 * 
 * ポップアップの有無と固定位置設定を管理する
 */
class PopupConfig {

    /**
     * ポップアップ設定を表すデータクラス
     */
    data class Config(
        val enabled: Boolean,
        val fixed: Boolean
    ) {
        companion object {
            val DEFAULT = Config(enabled = false, fixed = false)
        }
    }

    /**
     * 設定値からポップアップ設定を生成する
     *
     * @param usePopup ポップアップを使用するか
     * @param useFixedPopup 固定位置を使用するか
     * @return ポップアップ設定
     */
    fun getPopupConfig(usePopup: Boolean, useFixedPopup: Boolean): Config {
        return Config(enabled = usePopup, fixed = useFixedPopup)
    }

    /**
     * ポップアップが有効かどうか
     *
     * @param config ポップアップ設定
     * @return ポップアップ有効フラグ
     */
    fun isEnabled(config: Config): Boolean {
        return config.enabled
    }

    /**
     * ポップアップが固定位置かどうか
     *
     * @param config ポップアップ設定
     * @return 固定位置フラグ
     */
    fun isFixed(config: Config): Boolean {
        return config.fixed
    }
}
