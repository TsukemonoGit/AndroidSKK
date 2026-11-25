package jp.deadend.noname.skk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SKKUtilsTest {

    @Test
    fun testHankaku2Zenkaku() {
        assertEquals("ａｂｃ", hankaku2zenkaku("abc"))
        assertEquals("１２３", hankaku2zenkaku("123"))
        assertEquals("アイウ", hankaku2zenkaku("ｱｲｳ"))
        assertEquals("カキクケコ", hankaku2zenkaku("ｶｷｸｹｺ"))
        assertEquals("ワオン", hankaku2zenkaku("ﾜｵﾝ"))
        assertEquals("ガギグゲゴ", hankaku2zenkaku("ｶﾞｷﾞｸﾞｹﾞｺﾞ"))
        assertEquals("パピプペポ", hankaku2zenkaku("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ"))
        assertEquals("ヴ", hankaku2zenkaku("ｳﾞ"))
        assertEquals("－", hankaku2zenkaku("-")) // マイナス
        assertEquals("ー", hankaku2zenkaku("ｰ")) // 長音
        assertEquals(null, hankaku2zenkaku(null))
        // ヲ と 中黒 は別の文字（回帰テスト）
        assertEquals("ヲ", hankaku2zenkaku("ｦ"))
        assertEquals("・", hankaku2zenkaku("･"))
        // 単独の濁点・半濁点
        assertEquals("゛", hankaku2zenkaku("ﾞ"))
        assertEquals("゜", hankaku2zenkaku("ﾟ"))
    }

    @Test
    fun testZenkaku2Hankaku() {
        assertEquals("abc xyz ", zenkaku2hankaku("ａｂｃ　xyz "))
        assertEquals("123890", zenkaku2hankaku("１２３890"))
        assertEquals("ｱｲｳあいう", zenkaku2hankaku("アイウあいう"))
        assertEquals("ｶｷｸｹｺ", zenkaku2hankaku("カキクケコ"))
        assertEquals("ﾜｵﾝ", zenkaku2hankaku("ワオン"))
        assertEquals("ﾜｹ", zenkaku2hankaku("ヮヶ"))
        assertEquals("ｶﾞｷﾞｸﾞｹﾞｺﾞ", zenkaku2hankaku("ガギグゲゴ"))
        assertEquals("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ", zenkaku2hankaku("パピプペポ"))
        assertEquals("ｳﾞ", zenkaku2hankaku("ヴ"))
        assertEquals("ｰ", zenkaku2hankaku("ー")) // 長音
        assertEquals("-", zenkaku2hankaku("－")) // マイナス
        assertEquals(null, zenkaku2hankaku(null))
        // ヲ と 中黒 は別の文字（回帰テスト）
        assertEquals("ｦ", zenkaku2hankaku("ヲ"))
        assertEquals("･", zenkaku2hankaku("・"))
        // 単独の濁点・半濁点（合成用 U+3099/U+309A と間隔用 U+309B/U+309C の両方）
        assertEquals("ﾞ", zenkaku2hankaku("\u3099"))
        assertEquals("ﾟ", zenkaku2hankaku("\u309A"))
        assertEquals("ﾞ", zenkaku2hankaku("\u309B"))
        assertEquals("ﾟ", zenkaku2hankaku("\u309C"))
    }

    @Test
    fun testHankakuZenkakuRoundTrip() {
        // 代表的な半角・全角文字列で往復して元に戻ること。
        val hankaku = listOf(
            "abc", "123", "ｱｲｳ", "ｶｷｸｹｺ", "ｶﾞｷﾞｸﾞ", "ﾊﾟﾋﾟﾌﾟ", "ｦ", "･", "ｰ", "-"
        )
        hankaku.forEach { input ->
            assertEquals(input, zenkaku2hankaku(hankaku2zenkaku(input)))
        }
        val zenkaku = listOf(
            "ａｂｃ", "１２３", "アイウ", "ガギグゲゴ", "パピプペポ", "ヲ", "・", "ー", "－"
        )
        zenkaku.forEach { input ->
            assertEquals(input, hankaku2zenkaku(zenkaku2hankaku(input)))
        }
    }

    @Test
    fun testParseDictOrder() {
        assertEquals(
            listOf(DictOrderEntry("ユーザー辞書", "skk_userdict")),
            parseDictOrder("ユーザー辞書/skk_userdict/")
        )
        assertEquals(
            listOf(
                DictOrderEntry("SKK ada", "skk_dict_ada"),
                DictOrderEntry("SKK base", "skk_dict_base"),
            ),
            parseDictOrder("SKK ada/skk_dict_ada/SKK base/skk_dict_base/")
        )
        // 末尾にラベルだけ残った壊れた設定でも落ちない
        assertEquals(
            listOf(DictOrderEntry("SKK ada", "skk_dict_ada")),
            parseDictOrder("SKK ada/skk_dict_ada/orphan/")
        )
        assertEquals(emptyList<DictOrderEntry>(), parseDictOrder(""))
        assertEquals(emptyList<DictOrderEntry>(), parseDictOrder(null))
        assertEquals(emptyList<DictOrderEntry>(), parseDictOrder("/"))
    }

    @Test
    fun testUnzipFile() {
        val outDir = Files.createTempDirectory("skk-unzip").toFile()
        try {
            unzipFile(ByteArrayInputStream(zipOf("a/b.txt" to "hello")), outDir)
            assertEquals("hello", File(outDir, "a/b.txt").readText())
        } finally {
            outDir.deleteRecursively()
        }
    }

    @Test
    fun testUnzipRejectsSiblingPrefixEscape() {
        // outDir の名前を接頭辞に持つ兄弟ディレクトリへ脱出するケース。
        // 文字列の startsWith では防げないため、パス要素単位の比較が必要。
        val outDir = Files.createTempDirectory("skk-unzip").toFile()
        val evilName = "../${outDir.name}-evil/evil.txt"
        try {
            unzipFile(ByteArrayInputStream(zipOf(evilName to "evil")), outDir)
            fail("path traversal should be rejected: $evilName")
        } catch (expected: IOException) {
            // ok
        } finally {
            File(outDir.parentFile, "${outDir.name}-evil").deleteRecursively()
            outDir.deleteRecursively()
        }
    }

    @Test
    fun testUnzipRejectsParentEscape() {
        val outDir = Files.createTempDirectory("skk-unzip").toFile()
        try {
            unzipFile(ByteArrayInputStream(zipOf("../evil.txt" to "evil")), outDir)
            fail("path traversal should be rejected")
        } catch (expected: IOException) {
            // ok
        } finally {
            outDir.deleteRecursively()
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            entries.forEach { (name, body) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(body.toByteArray())
                zos.closeEntry()
            }
        }
        assertTrue(bos.size() > 0)
        return bos.toByteArray()
    }

    @Test
    fun testHiragana2Katakana() {
        assertEquals("アイウアイウ", hiragana2katakana("あいうアイウ"))
        assertEquals("ヴヴ", hiragana2katakana("う゛ヴ"))
        assertEquals("アイウあいう", hiragana2katakana("あいうアイウ", true))
        assertEquals("ヴゔ", hiragana2katakana("う゛ヴ", true))
        assertEquals("ー", hiragana2katakana("ー")) // かなカナの区別なし
        assertEquals(null, hiragana2katakana(null))
    }

    @Test
    fun testKatakana2Hiragana() {
        assertEquals("あいうあいう", katakana2hiragana("アイウあいう"))
        assertEquals("ゔ", katakana2hiragana("ヴ"))
        assertEquals(null, katakana2hiragana(null))
    }

    @Test
    fun testIsAlphabet() {
        assertEquals(true, isAlphabet('a'.code))
        assertEquals(true, isAlphabet('Z'.code))
        assertEquals(false, isAlphabet('1'.code))
    }

    @Test
    fun testIsVowel() {
        assertEquals(true, isVowel('a'.code))
        assertEquals(true, isVowel('u'.code))
        assertEquals(false, isVowel('b'.code))
    }

    @Test
    fun testRemoveAnnotation() {
        assertEquals("変換", removeAnnotation("変換;注釈"))
        assertEquals("単品", removeAnnotation("単品"))
    }

    @Test
    fun testProcessConcatAndMore() {
        assertEquals(
            "第123回45/",
            processConcatAndMore("(concat \"第#0回#0\\057\")", "だい123かい45すらっしゅ")
        )
        assertEquals(
            "第１２３回45;",
            processConcatAndMore("(concat \"第#1回#0\\073\")", "だい123かい45せみころん")
        )
        assertEquals("第一二三回#", processConcatAndMore("第#2回#0", "だい123かい"))
        assertEquals("第百二十三回", processConcatAndMore("第#3回", "だい123かい"))
        assertEquals(
            "第123回45",
            processConcatAndMore("(concat \"第#回\" \"#0\")", "だい123かい45")
        )
        assertEquals(
            "0/0/0/0",
            processConcatAndMore("(concat \"0\\0570\\0570\\0570\")", "1みりもかんけいないけど")
        )
    }
}