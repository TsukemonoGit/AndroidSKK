package jp.deadend.noname.skk

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * TenKeyLabelConfigのユニットテスト
 * 
 * 句読点キーのポップアップラベル設定ロジックをテストする
 * 
 * 期待動作:
 *   - "en" → 英文句読点スタイル（．，！）
 *   - "jp_en" → 英混用句読点スタイル（。，！）
 *   - その他 → 和文句読点スタイル（。、！）
 */
class TenKeyLabelConfigTest {

    private lateinit var config: TenKeyLabelConfig

    @Before
    fun setUp() {
        config = TenKeyLabelConfig()
    }

    // ==================== "en" テスト ====================

    @Test
    fun `testTenKeyLabelConfig_en()`() {
        val result = config.getTenKeyLabelConfig("en")
        assertEquals("？\n．，！\n…", result.mainLabel)
        assertArrayEquals(
            arrayOf("，", "．", "？", "！", "…", "", ""),
            result.flickGuide
        )
    }

    // ==================== "jp_en" テスト ====================

    @Test
    fun `testTenKeyLabelConfig_jp_en()`() {
        val result = config.getTenKeyLabelConfig("jp_en")
        assertEquals("？\n。，！\n…", result.mainLabel)
        assertArrayEquals(
            arrayOf("，", "。", "？", "！", "…", "", ""),
            result.flickGuide
        )
    }

    // ==================== デフォルト（和文）テスト ====================

    @Test
    fun `testTenKeyLabelConfig_default_jp()`() {
        val result = config.getTenKeyLabelConfig("jp")
        assertEquals("？\n。、！\n…", result.mainLabel)
        assertArrayEquals(
            arrayOf("、", "。", "？", "！", "…", "", ""),
            result.flickGuide
        )
    }

    @Test
    fun `testTenKeyLabelConfig_null()`() {
        val result = config.getTenKeyLabelConfig(null)
        assertEquals("？\n。、！\n…", result.mainLabel)
        assertArrayEquals(
            arrayOf("、", "。", "？", "！", "…", "", ""),
            result.flickGuide
        )
    }

    @Test
    fun `testTenKeyLabelConfig_empty()`() {
        val result = config.getTenKeyLabelConfig("")
        assertEquals("？\n。、！\n…", result.mainLabel)
    }
}
