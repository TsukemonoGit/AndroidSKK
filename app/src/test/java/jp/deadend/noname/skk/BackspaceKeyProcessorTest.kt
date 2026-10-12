package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * BackspaceKeyProcessorのユニットテスト
 * 
 * バックスペースキーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - サービスがバックスペースを処理した場合 → そのまま処理
 *   - サービスがバックスペースを処理できない場合 → DELを入力
 */
class BackspaceKeyProcessorTest {

    private lateinit var processor: BackspaceKeyProcessor

    @Before
    fun setUp() {
        processor = BackspaceKeyProcessor()
    }

    // ==================== サービス処理済みの場合 ====================

    @Test
    fun `testBackspace_serviceHandled()`() {
        // サービスがバックスペースを処理した場合
        val result = processor.processBackspaceKey(true)
        assertEquals(BackspaceKeyProcessor.BackspaceAction.HANDLE_BACKSPACE, result)
    }

    // ==================== サービス処理できない場合 ====================

    @Test
    fun `testBackspace_serviceNotHandled()`() {
        // サービスがバックスペースを処理できない場合
        val result = processor.processBackspaceKey(false)
        assertEquals(BackspaceKeyProcessor.BackspaceAction.PRESS_DEL, result)
    }
}
