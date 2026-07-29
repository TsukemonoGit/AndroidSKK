package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PopupConfigのユニットテスト
 * 
 * ポップアップガイドの設定ロジックをテストする
 * 
 * 期待動作:
 *   - enabled=false → ポップアップ表示しない
 *   - enabled=true, fixed=true → 固定位置に表示
 *   - enabled=true, fixed=false → 追従位置に表示
 */
class PopupConfigTest {

    private lateinit var config: PopupConfig

    @Before
    fun setUp() {
        config = PopupConfig()
    }

    // ==================== デフォルトテスト ====================

    @Test
    fun testPopupConfig_default() {
        val default = PopupConfig.Config.DEFAULT
        assertFalse(config.isEnabled(default))
        assertFalse(config.isFixed(default))
    }

    // ==================== 無効設定テスト ====================

    @Test
    fun testPopupConfig_disabled() {
        val result = config.getPopupConfig(false, false)
        assertFalse(config.isEnabled(result))
        assertFalse(config.isFixed(result))
    }

    // ==================== 有効+固定テスト ====================

    @Test
    fun testPopupConfig_enabled_fixed() {
        val result = config.getPopupConfig(true, true)
        assertTrue(config.isEnabled(result))
        assertTrue(config.isFixed(result))
    }

    // ==================== 有効+非固定テスト ====================

    @Test
    fun testPopupConfig_enabled_notFixed() {
        val result = config.getPopupConfig(true, false)
        assertTrue(config.isEnabled(result))
        assertFalse(config.isFixed(result))
    }

    // ==================== 無効+固定テスト ====================

    @Test
    fun testPopupConfig_disabled_fixed() {
        val result = config.getPopupConfig(false, true)
        assertFalse(config.isEnabled(result))
        assertTrue(config.isFixed(result))
    }

    // ==================== isEnabledテスト ====================

    @Test
    fun testIsEnabled_true() {
        val result = config.getPopupConfig(true, false)
        assertTrue(config.isEnabled(result))
    }

    @Test
    fun testIsEnabled_false() {
        val result = config.getPopupConfig(false, false)
        assertFalse(config.isEnabled(result))
    }

    // ==================== isFixedテスト ====================

    @Test
    fun testIsFixed_true() {
        val result = config.getPopupConfig(true, true)
        assertTrue(config.isFixed(result))
    }

    @Test
    fun testIsFixed_false() {
        val result = config.getPopupConfig(true, false)
        assertFalse(config.isFixed(result))
    }
}
