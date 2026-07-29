package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.EnumSet

/**
 * MushroomKeyProcessorのユニットテスト
 * 
 * マッシュルーム（花丸）キーの操作ロジックをテストする
 * 
 * 期待動作:
 *   - 上フリック（UP）→ マッシュルームを送信
 *   - その他 → 送信しない
 */
class MushroomKeyProcessorTest {

    private lateinit var processor: MushroomKeyProcessor

    @Before
    fun setUp() {
        processor = MushroomKeyProcessor()
    }

    // ==================== 上フリック（UP）テスト ====================

    @Test
    fun `testMushroom_up_send()`() {
        // 上フリック = マッシュルーム送信
        val flickState = EnumSet.of(FlickState.UP)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.SEND_MUSHROOM, result)
    }

    // ==================== その他のフリックテスト ====================

    @Test
    fun `testMushroom_tap_noOp()`() {
        // タップ = 送信しない
        val flickState = EnumSet.of(FlickState.NONE)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }

    @Test
    fun `testMushroom_left_noOp()`() {
        // 左フリック = 送信しない
        val flickState = EnumSet.of(FlickState.LEFT)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }

    @Test
    fun `testMushroom_right_noOp()`() {
        // 右フリック = 送信しない
        val flickState = EnumSet.of(FlickState.RIGHT)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }

    @Test
    fun `testMushroom_down_noOp()`() {
        // 下フリック = 送信しない
        val flickState = EnumSet.of(FlickState.DOWN)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }

    @Test
    fun `testMushroom_curve_noOp()`() {
        // カーブフリック = 送信しない
        val flickState = EnumSet.of(FlickState.UP, FlickState.CURVE_RIGHT)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }

    @Test
    fun `testMushroom_empty_noOp()`() {
        // 空フリック = 送信しない
        val flickState = EnumSet.noneOf(FlickState::class.java)
        val result = processor.processMushroomKey(flickState)
        assertEquals(MushroomKeyProcessor.MushroomAction.NO_OP, result)
    }
}
