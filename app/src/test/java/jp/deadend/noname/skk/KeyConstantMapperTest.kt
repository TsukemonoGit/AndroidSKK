package jp.deadend.noname.skk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * KeyConstantMapperのユニットテスト
 * 
 * キーコード定数のマッピングロジックをテストする
 * 
 * 期待動作:
 *   - 記号/数字（!, (, ), ,, ., 0-9, ?, [, ]）はSERVICEに直接渡す
 *   - その他はSERVICEに直接渡さない
 */
class KeyConstantMapperTest {

    private lateinit var mapper: KeyConstantMapper

    @Before
    fun setUp() {
        mapper = KeyConstantMapper()
    }

    // ==================== 直接SERVICEに渡すキー ====================

    @Test
    fun `testDirectKey_exclamation()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(33))
    }

    @Test
    fun `testDirectKey_parentheses()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(40))
        assertTrue(KeyConstantMapper.isDirectServiceKey(41))
    }

    @Test
    fun `testDirectKey_comma_dot()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(44))
        assertTrue(KeyConstantMapper.isDirectServiceKey(46))
    }

    @Test
    fun `testDirectKey_numbers()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(48))
        assertTrue(KeyConstantMapper.isDirectServiceKey(49))
        assertTrue(KeyConstantMapper.isDirectServiceKey(50))
        assertTrue(KeyConstantMapper.isDirectServiceKey(51))
        assertTrue(KeyConstantMapper.isDirectServiceKey(52))
        assertTrue(KeyConstantMapper.isDirectServiceKey(53))
        assertTrue(KeyConstantMapper.isDirectServiceKey(54))
        assertTrue(KeyConstantMapper.isDirectServiceKey(55))
        assertTrue(KeyConstantMapper.isDirectServiceKey(56))
        assertTrue(KeyConstantMapper.isDirectServiceKey(57))
    }

    @Test
    fun `testDirectKey_question()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(63))
    }

    @Test
    fun `testDirectKey_brackets()`() {
        assertTrue(KeyConstantMapper.isDirectServiceKey(91))
        assertTrue(KeyConstantMapper.isDirectServiceKey(93))
    }

    // ==================== 直接SERVICEに渡さないキー ====================

    @Test
    fun `testNonDirectKey_space()`() {
        assertFalse(KeyConstantMapper.isDirectServiceKey(32))
    }

    @Test
    fun `testNonDirectKey_enter()`() {
        assertFalse(KeyConstantMapper.isDirectServiceKey(66))
    }

    @Test
    fun `testNonDirectKey_delete()`() {
        assertFalse(KeyConstantMapper.isDirectServiceKey(-5))
    }

    @Test
    fun `testNonDirectKey_shift()`() {
        assertFalse(KeyConstantMapper.isDirectServiceKey(-100))
    }

    @Test
    fun `testNonDirectKey_a()`() {
        assertFalse(KeyConstantMapper.isDirectServiceKey('a'.code))
    }

    // ==================== 数字判定 ====================

    @Test
    fun `testNumericKey_0to9()`() {
        for (i in 48..57) {
            assertTrue(KeyConstantMapper.isNumericKey(i))
        }
    }

    @Test
    fun `testNonNumericKey()`() {
        assertFalse(KeyConstantMapper.isNumericKey(33))
        assertFalse(KeyConstantMapper.isNumericKey('a'.code))
    }

    // ==================== 記号判定 ====================

    @Test
    fun `testSymbolKey_symbols()`() {
        assertTrue(KeyConstantMapper.isSymbolKey(33))    // !
        assertTrue(KeyConstantMapper.isSymbolKey(40))    // (
        assertTrue(KeyConstantMapper.isSymbolKey(41))    // )
        assertTrue(KeyConstantMapper.isSymbolKey(44))    // ,
        assertTrue(KeyConstantMapper.isSymbolKey(46))    // .
        assertTrue(KeyConstantMapper.isSymbolKey(63))    // ?
        assertTrue(KeyConstantMapper.isSymbolKey(91))    // [
        assertTrue(KeyConstantMapper.isSymbolKey(93))    // ]
    }

    @Test
    fun `testNonSymbolKey_numbers()`() {
        assertFalse(KeyConstantMapper.isSymbolKey(48))   // 0
        assertFalse(KeyConstantMapper.isSymbolKey(57))   // 9
    }
}
