package jp.deadend.noname.skk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        SKKUserDictionary.resetInstancesForTest()
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

    @Test
    fun testFindKeysReturnsEntries() = runBlocking {
        dict.addEntry("かんじ", "漢字", "")
        dict.addEntry("かんじ", "感じ", "")
        dict.addEntry("かんじる", "感じる", "")

        val keys = dict.findKeys(this, "かんじ")

        assertEquals(listOf("かんじ" to "かんじ", "かんじる" to "かんじる"), keys)
    }

    @Test
    fun testFindKeysWhileWritingDoesNotThrow() = runBlocking {
        dict.addEntry("かんじ", "漢字", "")
        // findKeys は書き込みと同じ mutex を使う。同時実行で壊れないこと。
        val writer =
                launch(Dispatchers.IO) {
                    repeat(30) { i -> dict.addEntry("かんじ", "候補$i", "") }
                }
        repeat(30) { dict.findKeys(this, "かんじ") }
        writer.join()

        assertTrue(dict.getEntry("かんじ")!!.candidates.isNotEmpty())
    }

    @Test
    fun testGetInstanceReturnsSameInstanceForSameFile() {
        // IME と辞書ツールが同じ DB を別々に開くと JDBM が壊れる。
        // 同じパスに対しては必ず同じインスタンスを返すこと。
        val path = File(dir, "shared").path
        val first = SKKUserDictionary.getInstance(path, "user")
        val second = SKKUserDictionary.getInstance(path, "user")

        assertNotNull(first)
        assertSame(first, second)
    }

    @Test
    fun testGetInstanceReopensClosedInstance() {
        val path = File(dir, "shared").path
        val first = SKKUserDictionary.getInstance(path, "user")!!
        first.addEntry("かんじ", "漢字", "")
        first.close()

        val second = SKKUserDictionary.getInstance(path, "user")!!

        assertSame("同じ参照のまま再オープンする", first, second)
        assertEquals(listOf("漢字"), second.getEntry("かんじ")!!.candidates)
    }

    @Test
    fun testRecreateKeepsSameInstanceAndClearsEntries() {
        val path = File(dir, "shared").path
        val first = SKKUserDictionary.getInstance(path, "user")!!
        first.addEntry("かんじ", "漢字", "")

        first.recreate()

        assertSame(first, SKKUserDictionary.getInstance(path, "user"))
        assertNull(first.getEntry("かんじ"))
    }

    @Test
    fun testGetAllEntries() {
        dict.addEntry("かんじ", "漢字", "")
        dict.addEntry("かんじる", "感じる", "")

        val entries = dict.getAllEntries().toMap()

        assertEquals("/漢字/", entries["かんじ"])
        assertEquals("/感じる/", entries["かんじる"])
    }

    @Test
    fun testRemoveKeyRemovesWholeEntry() {
        dict.addEntry("かんじ", "漢字", "")
        dict.addEntry("かんじ", "感じ", "")

        dict.removeKey("かんじ")

        assertNull(dict.getEntry("かんじ"))
    }

    @Test
    fun testImportFromText() {
        dict.importFromText(
            "かんじ /漢字/\nかんじる /感じる/\n".byteInputStream(),
            "UTF-8",
            isWordList = false
        ) {}

        assertEquals(listOf("漢字"), dict.getEntry("かんじ")!!.candidates)
        assertEquals(listOf("感じる"), dict.getEntry("かんじる")!!.candidates)
    }

    @Test
    fun testImportFromTextInBatches() {
        // バッチ境界（1000 行）をまたいでも全件登録されること。
        val text = buildString {
            repeat(2500) { i -> append("よみ$i /単語$i/\n") }
        }
        dict.importFromText(text.byteInputStream(), "UTF-8", isWordList = false) {}

        assertEquals(2500, dict.size())
        assertEquals(listOf("単語2499"), dict.getEntry("よみ2499")!!.candidates)
    }

    @Test
    fun testImportAbortsWhenDictionaryIsRecreated() {
        // インポート中に辞書が作り直されたら、古い BTree へ書き続けないこと。
        val text = buildString {
            repeat(2500) { i -> append("よみ$i /単語$i/\n") }
        }
        val source = ByteArrayInputStream(text.toByteArray())
        var recreated = false
        val stream =
            object : InputStream() {
                private fun maybeRecreate() {
                    if (!recreated) {
                        recreated = true
                        dict.recreate()
                    }
                }

                override fun read(): Int {
                    maybeRecreate()
                    return source.read()
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    maybeRecreate()
                    return source.read(b, off, len)
                }
            }

        val completed = dict.importFromText(stream, "UTF-8", false)

        assertFalse("辞書が差し替わったら中断する", completed)
        assertEquals(0, dict.size())
    }

    @Test
    fun testToolAddDoesNotOverwriteRollbackState() {
        // IME の変換で追加したエントリの rollback 情報を、ツールからの追加が
        // 上書きしないこと。上書きされると rollBack() がツールの単語を消してしまう。
        dict.addEntry("ほん", "本", "")
        dict.addEntry("つーる", "ツール", "", updateRollback = false)

        dict.rollBack()

        assertNull("IME の追加が rollback される", dict.getEntry("ほん"))
        assertEquals("ツールの追加は残る", listOf("ツール"), dict.getEntry("つーる")!!.candidates)
    }
}
