package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * EnterKeyProcessorのユニットテスト
 * 
 * Enterキーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - サービスがEnterを処理した場合 → そのまま処理
 *   - サービスがEnterを処理できない場合 → 直接Enterを入力
 */
class EnterKeyProcessorTest {

    private lateinit var processor: EnterKeyProcessor

    @Before
    fun setUp() {
        processor = EnterKeyProcessor()
    }

    // ==================== サービス処理済みの場合 ====================

    @Test
    fun `testEnter_serviceHandled()`() {
        // サービスがEnterを処理した場合
        val result = processor.processEnterKey(true)
        assertEquals(EnterKeyProcessor.EnterAction.HANDLE_ENTER, result)
    }

    // ==================== サービス処理できない場合 ====================

    @Test
    fun `testEnter_serviceNotHandled()`() {
        // サービスがEnterを処理できない場合
        val result = processor.processEnterKey(false)
        assertEquals(EnterKeyProcessor.EnterAction.PRESS_ENTER, result)
    }
}
