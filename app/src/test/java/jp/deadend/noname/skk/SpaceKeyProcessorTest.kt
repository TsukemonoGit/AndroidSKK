package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * SpaceKeyProcessorのユニットテスト
 * 
 * スペースキーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - シフト状態 → 設定画面を開く
 *   - タップ（NONE）→ スペースを入力
 *   - フリック操作 → 何もしない
 */
class SpaceKeyProcessorTest {

    private lateinit var processor: SpaceKeyProcessor

    @Before
    fun setUp() {
        processor = SpaceKeyProcessor()
    }

    // ==================== シフト状態テスト ====================

    @Test
    fun `testSpace_shifted_openSettings()`() {
        // シフト+スペース = 設定画面を開く
        val result = processor.processSpaceKey(true, EnumSet.of(FlickState.NONE))
        assertEquals(SpaceKeyProcessor.SpaceAction.OPEN_SETTINGS, result)
    }

    @Test
    fun `testSpace_shifted_leftFlick_openSettings()`() {
        // シフト+左フリック = 設定画面を開く
        val result = processor.processSpaceKey(true, EnumSet.of(FlickState.LEFT))
        assertEquals(SpaceKeyProcessor.SpaceAction.OPEN_SETTINGS, result)
    }

    // ==================== タップ（NONE）テスト ====================

    @Test
    fun `testSpace_tap_inputSpace()`() {
        // タップ = スペースを入力
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.NONE))
        assertEquals(SpaceKeyProcessor.SpaceAction.INPUT_SPACE, result)
    }

    // ==================== フリック操作テスト ====================

    @Test
    fun `testSpace_leftFlick_noOp()`() {
        // 左フリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.LEFT))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }

    @Test
    fun `testSpace_upFlick_noOp()`() {
        // 上フリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.UP))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }

    @Test
    fun `testSpace_rightFlick_noOp()`() {
        // 右フリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.RIGHT))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }

    @Test
    fun `testSpace_downFlick_noOp()`() {
        // 下フリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.DOWN))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }

    @Test
    fun `testSpace_curveFlick_noOp()`() {
        // カーブフリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.of(FlickState.LEFT, FlickState.CURVE_LEFT))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }

    @Test
    fun `testSpace_emptyFlick_noOp()`() {
        // 空フリック = 何もしない
        val result = processor.processSpaceKey(false, EnumSet.noneOf(FlickState::class.java))
        assertEquals(SpaceKeyProcessor.SpaceAction.NO_OP, result)
    }
}
