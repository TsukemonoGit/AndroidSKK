package jp.deadend.noname.skk

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ArrowKeyProcessorのユニットテスト
 * 
 * アローキー（左右）の操作ロジックをテストする
 * 
 * 期待動作:
 *   - フリック中 → 何もしない
 *   - サービスがD-padを処理した場合 → そのまま処理
 *   - サービスが処理できない場合 → 直接キーを入力
 */
class ArrowKeyProcessorTest {

    private lateinit var processor: ArrowKeyProcessor

    @Before
    fun setUp() {
        processor = ArrowKeyProcessor()
    }

    // ==================== フリック中の場合 ====================

    @Test
    fun `testArrow_flicking_noOp()`() {
        // フリック中は何もしない
        val result = processor.processArrowKey(true, false, KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(ArrowKeyProcessor.ArrowAction.NO_OP, result)
    }

    @Test
    fun `testArrow_flicking_right_noOp()`() {
        // フリック中（右）は何もしない
        val result = processor.processArrowKey(true, false, KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(ArrowKeyProcessor.ArrowAction.NO_OP, result)
    }

    // ==================== サービス処理済みの場合 ====================

    @Test
    fun `testArrow_serviceHandled_left()`() {
        // サービスがD-padを処理（左）
        val result = processor.processArrowKey(false, true, KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(ArrowKeyProcessor.ArrowAction.HANDLE_DPAD, result)
    }

    @Test
    fun `testArrow_serviceHandled_right()`() {
        // サービスがD-padを処理（右）
        val result = processor.processArrowKey(false, true, KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(ArrowKeyProcessor.ArrowAction.HANDLE_DPAD, result)
    }

    // ==================== サービス処理できない場合 ====================

    @Test
    fun `testArrow_serviceNotHandled_left()`() {
        // サービスが処理できない（左）
        val result = processor.processArrowKey(false, false, KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(ArrowKeyProcessor.ArrowAction.KEY_DOWN_UP, result)
    }

    @Test
    fun `testArrow_serviceNotHandled_right()`() {
        // サービスが処理できない（右）
        val result = processor.processArrowKey(false, false, KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(ArrowKeyProcessor.ArrowAction.KEY_DOWN_UP, result)
    }

    // ==================== アローキー判定 ====================

    @Test
    fun `testArrowDirection_left()`() {
        assertEquals(ArrowKeyProcessor.ArrowType.LEFT, processor.getArrowDirection(KeyEvent.KEYCODE_DPAD_LEFT))
    }

    @Test
    fun `testArrowDirection_right()`() {
        assertEquals(ArrowKeyProcessor.ArrowType.RIGHT, processor.getArrowDirection(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    @Test
    fun `testArrowDirection_unknown()`() {
        assertEquals(ArrowKeyProcessor.ArrowType.UNKNOWN, processor.getArrowDirection(KeyEvent.KEYCODE_UNKNOWN))
    }
}
