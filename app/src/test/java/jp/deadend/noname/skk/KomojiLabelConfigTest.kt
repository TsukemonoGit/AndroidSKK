package jp.deadend.noname.skk

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * KomojiLabelConfigのユニットテスト
 * 
 * 小キーのポップアップラベル設定ロジックをテストする
 * 
 * 期待動作:
 *   - softCancelKey=true → "小\n ◻゙CXL◻゚ \n▽"
 *   - softTransKey=true → "CXL\n ◻゙□゚ \n▽"
 *   - デフォルト → "CXL\n ◻゙小◻゚ \n▽"
 */
class KomojiLabelConfigTest {

    private lateinit var config: KomojiLabelConfig

    @Before
    fun setUp() {
        config = KomojiLabelConfig()
    }

    // ==================== softCancelKey=true テスト ====================

    @Test
    fun testKomojiLabelConfig_softCancel() {
        val result = config.getKomojiLabelConfig(true, false)
        assertEquals("小\n ◻゙CXL◻゚ \n▽", result.mainLabel)
        assertArrayEquals(
            arrayOf("CXL", "◻゙", "小", "◻゚", "▽", "", ""),
            result.flickGuide
        )
    }

    // ==================== softTransKey=true テスト ====================

    @Test
    fun testKomojiLabelConfig_softTrans() {
        val result = config.getKomojiLabelConfig(false, true)
        assertEquals("CXL\n ◻゙□゚ \n▽", result.mainLabel)
        assertArrayEquals(
            arrayOf("◻゙□゚", "◻゙", "CXL", "◻゚", "▽", "", ""),
            result.flickGuide
        )
    }

    // ==================== 両方true テスト ====================

    @Test
    fun testKomojiLabelConfig_bothTrue_softCancelWins() {
        // softCancelKeyが優先される
        val result = config.getKomojiLabelConfig(true, true)
        assertEquals("小\n ◻゙CXL◻゚ \n▽", result.mainLabel)
    }

    // ==================== デフォルトテスト ====================

    @Test
    fun testKomojiLabelConfig_default() {
        val result = config.getKomojiLabelConfig(false, false)
        assertEquals("CXL\n ◻゙小◻゚ \n▽", result.mainLabel)
        assertArrayEquals(
            arrayOf("小", "◻゙", "CXL", "◻゚", "▽", "", ""),
            result.flickGuide
        )
    }
}
