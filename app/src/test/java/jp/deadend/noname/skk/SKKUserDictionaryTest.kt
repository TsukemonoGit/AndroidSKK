package jp.deadend.noname.skk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SKKUserDictionaryTest {

    private lateinit var dir: File
    private lateinit var dict: SKKUserDictionary

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("skk-userdict").toFile()
        val (recMan, btree) = SKKUserDictionary.openDB(File(dir, "userdict").path, "user")
        dict = SKKUserDictionary.newInstanceFromOpenDB(
            recMan, btree, File(dir, "userdict").path, "user"
        )
    }

    @After
    fun tearDown() {
        dict.close()
        dir.deleteRecursively()
    }

    @Test
    fun testAddAndGetEntry() {
        dict.addEntry("かんじ", "漢字", "")
        val entry = dict.getEntry("かんじ")
        assertNotNull(entry)
        assertEquals(listOf("漢字"), entry!!.candidates)
    }

    @Test
    fun testAddEntryMergesCandidates() {
        dict.addEntry("かんじ", "漢字", "")
        dict.addEntry("かんじ", "感じ", "")
        assertEquals(listOf("感じ", "漢字"), dict.getEntry("かんじ")!!.candidates)
    }

    @Test
    fun testRemoveEntry() {
        dict.addEntry("かんじ", "漢字", "")
        dict.removeEntry("かんじ", "漢字", "")
        assertNull(dict.getEntry("かんじ"))
    }

    @Test
    fun testRemoveEntryKeepsOtherCandidates() {
        dict.addEntry("かんじ", "漢字", "")
        dict.addEntry("かんじ", "感じ", "")
        dict.removeEntry("かんじ", "漢字", "")
        assertEquals(listOf("感じ"), dict.getEntry("かんじ")!!.candidates)
    }

    @Test
    fun testRemoveEntryUsingNumberPlaceholderKey() {
        // 「だい4かい」に対応する実キーが「だい#かい」のとき、
        // 読みで解決したキーを削除し、元の読みでゴミエントリを作らないこと。
        dict.addEntry("だい#かい", "第4回", "")
        assertNull("読みでは直接引けない", dict.getEntry("だい4かい"))

        dict.removeEntry("だい4かい", "第4回", "")

        assertNull("解決後の実キーが削除される", dict.getEntry("だい#かい"))
        assertNull("元の読みでゴミエントリを作らない", dict.getEntry("だい4かい"))
    }

    @Test
    fun testReplaceEntryWithOnlySlashesRemovesEntry() {
        dict.addEntry("かんじ", "漢字", "")
        dict.replaceEntry("かんじ", "///")
        assertNull(dict.getEntry("かんじ"))
    }

    @Test
    fun testRemoveEntryWithOkuriganaBlock() {
        dict.addEntry("たべる", "食べる", "たべ")
        val before = dict.getEntry("たべる")!!
        assertTrue(before.candidates.contains("食べる"))

        dict.removeEntry("たべる", "食べる", "たべ")
        assertNull(dict.getEntry("たべる"))
    }
}
