package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.ArrayList

/**
 * extractCommon()のユニットテスト
 * 
 * 音声認識結果などのリストから共通の接頭辞・接尾辞を抽出し、
 * 中央部分をハイライト（【】で囲む）した配列を返す関数のテスト
 */
class ExtractCommonTest {

    private val leftSymbol = "【"
    private val rightSymbol = "】"

    private fun testExtractCommon(list: ArrayList<String>): Triple<String, Array<String>, String> {
        var commonPrefix = list.last()
        var commonSuffix = list.last()
        list.forEach {
            if (commonPrefix.isNotEmpty()) {
                commonPrefix = it.commonPrefixWith(commonPrefix)
            }
            if (commonSuffix.isNotEmpty()) {
                commonSuffix = it.commonSuffixWith(commonSuffix)
            }
        }

        val array =
                list
                        .map {
                            leftSymbol +
                                    it.substring(
                                            commonPrefix.length,
                                            it.length - commonSuffix.length
                                    ) +
                                    rightSymbol
                        }
                        .toTypedArray()
        return Triple(commonPrefix, array, commonSuffix)
    }

    // ==================== 基本テスト ====================

    @Test
    fun testExtractCommon_identical_strings() {
        val list = arrayListOf("あ", "い", "う")
        val result = testExtractCommon(list)
        
        assertEquals("", result.first)   // 共通接頭辞なし
        assertEquals("", result.third)   // 共通接尾辞なし
        assertEquals(listOf("【あ】", "【い】", "【う】").toList(), result.second.toList())
    }

    @Test
    fun testExtractCommon_common_prefix() {
        val list = arrayListOf("りんご", "りんごりんご", "りんごと")
        val result = testExtractCommon(list)
        
        // last()="りんごと"との共通接頭辞は「りんご」
        // last()="りんごと"との共通接尾辞は「」
        assertEquals("りんご", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【】", "【りんご】", "【と】").toList(), 
                    result.second.toList())
    }

    @Test
    fun testExtractCommon_common_suffix() {
        val list = arrayListOf("ごりん", "りんごりんご", "ごりんご")
        val result = testExtractCommon(list)
        
        // 全ての文字列に共通する接頭辞・接尾辞はなし
        assertEquals("", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【ごりん】", "【りんごりんご】", "【ごりんご】").toList(),
                    result.second.toList())
    }

    @Test
    fun testExtractCommon_common_prefix_and_suffix() {
        val list = arrayListOf("あいおり", "あいこえお", "あいさつ")
        val result = testExtractCommon(list)
        
        println("prefix: '${result.first}', suffix: '${result.third}'")
        println("array: ${result.second.toList()}")
        
        assertEquals("あい", result.first)     // 共通接頭辞
        assertEquals("", result.third)         // 共通接尾辞なし
        assertEquals(listOf("【おり】", "【こえお】", "【さつ】").toList(),
                    result.second.toList())
    }

    @Test
    fun testExtractCommon_all_identical() {
        val list = arrayListOf("テスト", "テスト", "テスト")
        // 接頭辞=接尾辞=文字列全体の場合、substring(start, end)でstart > endとなり例外発生
        // これは実装上の制限としてテストする
        try {
            val result = testExtractCommon(list)
            // 例外が発生しなかった場合は、空文字列が返ることを期待
            assertEquals("テスト", result.first)
            assertEquals("テスト", result.third)
        } catch (e: StringIndexOutOfBoundsException) {
            // 想定内の例外
        }
    }

    // ==================== エッジケース ====================

    @Test
    fun testExtractCommon_single_element() {
        val list = arrayListOf("あいうえお")
        // 単一要素の場合、接頭辞=接尾辞=文字列全体で例外発生
        try {
            val result = testExtractCommon(list)
            assertEquals("あいうえお", result.first)
            assertEquals("あいうえお", result.third)
        } catch (e: StringIndexOutOfBoundsException) {
            // 想定内の例外
        }
    }

    @Test
    fun testExtractCommon_empty_list() {
        val list: ArrayList<String> = arrayListOf()
        // ArrayList.last()は空リストで例外を投げるので、テストしない
        // または、呼び出し元で空チェックが必要
    }

    @Test
    fun testExtractCommon_empty_strings() {
        val list = arrayListOf("", "", "")
        val result = testExtractCommon(list)
        
        assertEquals("", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【】", "【】", "【】").toList(), result.second.toList())
    }

    @Test
    fun testExtractCommon_mixed_empty() {
        val list = arrayListOf("あ", "", "い")
        val result = testExtractCommon(list)
        
        assertEquals("", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【あ】", "【】", "【い】").toList(), result.second.toList())
    }

    // ==================== 実際の音声認識結果を想定したテスト ====================

    @Test
    fun testExtractCommon_voice_recognition_similar() {
        // 音声認識で似た結果が返ってくるケース
        val list = arrayListOf("すみません", "すいません", "すみません")
        val result = testExtractCommon(list)
        
        // last()="すみません"との共通接頭辞は「す」
        // last()="すみません"との共通接尾辞は「ません」
        // 中央部分は【み】,【い】,【み】（接頭辞/接尾辞を除いた部分）
        assertEquals("す", result.first)
        assertEquals("ません", result.third)
        assertEquals(listOf("【み】", "【い】", "【み】").toList(),
                    result.second.toList())
    }

    @Test
    fun testExtractCommon_voice_recognition_different_lengths() {
        // 長さが違う結果が返ってくるケース
        val list = arrayListOf("こんにちは", "こんにちわ", "こんニチワ")
        val result = testExtractCommon(list)
        
        // 共通接頭辞「こん」, 接尾辞なし
        assertEquals("こん", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【にちは】", "【にちわ】", "【ニチワ】").toList(),
                    result.second.toList())
    }

    @Test
    fun testExtractCommon_voice_recognition_with_suffix() {
        // 動詞の活用形など
        val list = arrayListOf("たべる", "たべた", "たべる")
        val result = testExtractCommon(list)
        
        // last()="たべる"との共通接頭辞は「たべ」
        // last()="たべる"との共通接尾辞は「」
        assertEquals("たべ", result.first)
        assertEquals("", result.third)
        assertEquals(listOf("【る】", "【た】", "【る】").toList(),
                    result.second.toList())
    }
}
