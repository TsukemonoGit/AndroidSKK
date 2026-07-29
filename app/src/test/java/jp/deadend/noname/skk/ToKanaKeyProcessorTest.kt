package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ToKanaKeyProcessorのユニットテスト
 * 
 * かな/ローマ字キーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - 日本語キーボードでない場合 → 日本語キーボードに切り替え
 *   - 日本語キーボードの場合 → 何もしない
 */
class ToKanaKeyProcessorTest {

    private lateinit var processor: ToKanaKeyProcessor

    @Before
    fun setUp() {
        processor = ToKanaKeyProcessor()
    }

    // ==================== 切り替えテスト ====================

    @Test
    fun testToKana_switchFromQwerty() {
        // 英数キーボードから → 日本語キーボードに切り替え
        val result = processor.processToKanaKey(false)
        assertEquals(ToKanaKeyProcessor.ToKanaAction.SWITCH_TO_JP, result)
    }

    @Test
    fun testToKana_noOpOnJapanese() {
        // 既に日本語キーボード → 何もしない
        val result = processor.processToKanaKey(true)
        assertEquals(ToKanaKeyProcessor.ToKanaAction.NO_OP, result)
    }

    // ==================== 半角モード状態テスト ====================

    @Test
    fun testCalculateHankakuState_hanKana() {
        // ハングアナ状態 → 半角モード
        val result = processor.calculateHankakuState("HAN_KANA")
        assertEquals(true, result)
    }

    @Test
    fun testCalculateHankakuState_hiragana() {
        // ひらがな状態 → 全角モード
        val result = processor.calculateHankakuState("HIRAGANA")
        assertEquals(false, result)
    }

    @Test
    fun testCalculateHankakuState_katakana() {
        // カタカナ状態 → 全角モード
        val result = processor.calculateHankakuState("KATAKANA")
        assertEquals(false, result)
    }

    @Test
    fun testCalculateHankakuState_zenkaku() {
        // 全角状態 → 全角モード
        val result = processor.calculateHankakuState("ZENKAKU")
        assertEquals(false, result)
    }

    @Test
    fun testCalculateHankakuState_ascii() {
        // ASCII状態 → 全角モード
        val result = processor.calculateHankakuState("ASCII")
        assertEquals(false, result)
    }

    @Test
    fun testCalculateHankakuState_null() {
        // null → 全角モード
        val result = processor.calculateHankakuState(null)
        assertEquals(false, result)
    }

    @Test
    fun testCalculateHankakuState_empty() {
        // 空文字列 → 全角モード
        val result = processor.calculateHankakuState("")
        assertEquals(false, result)
    }
}
