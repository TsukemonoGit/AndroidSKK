package jp.deadend.noname.skk

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SKKHistoryDictionaryTest {

    private lateinit var dir: File
    private var dict: SKKHistoryDictionary? = null

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("skk-history").toFile()
    }

    @After
    fun tearDown() {
        dict?.close()
        dir.deleteRecursively()
    }

    @Test
    fun testMergeDiskSnapshotKeepsEntriesAddedDuringRebuild() {
        val disk = mapOf("a" to "/old/")
        val current = mapOf("a" to "/new/", "b" to "/added/")

        val merged = mergeHistoryDiskSnapshot(disk, current, emptySet(), false)

        assertEquals("/new/", merged["a"]) // メモリ側が新しい
        assertEquals("/added/", merged["b"]) // ディスクに無い追加分を残す
    }

    @Test
    fun testMergeDiskSnapshotDoesNotResurrectPendingRemoval() {
        val disk = mapOf("a" to "/a/", "b" to "/b/")
        val current = mapOf("a" to "/a/")

        val merged = mergeHistoryDiskSnapshot(disk, current, setOf("b"), false)

        assertEquals(setOf("a"), merged.keys)
    }

    @Test
    fun testMergeDiskSnapshotRespectsPendingClear() {
        val disk = mapOf("a" to "/a/")
        val merged = mergeHistoryDiskSnapshot(disk, emptyMap(), emptySet(), true)
        assertEquals(0, merged.size)
    }

    @Test
    fun testAddHistoryIsPersisted() {
        dict = newDict()
        dict!!.addHistory("かんじ", "漢字")
        runBlocking { dict!!.awaitBackground() }
        assertNotNull(dict!!.getHistory("かんじ"))
        dict!!.close()

        dict = newDict()
        runBlocking { dict!!.awaitBackground() }
        assertEquals("/漢字/", dict!!.getHistory("かんじ"))
    }

    @Test
    fun testRemoveHistoryIsPersisted() {
        dict = newDict()
        dict!!.addHistory("かんじ", "漢字")
        dict!!.removeHistory("かんじ")
        runBlocking { dict!!.awaitBackground() }
        assertNull(dict!!.getHistory("かんじ"))
        dict!!.close()

        dict = newDict()
        runBlocking { dict!!.awaitBackground() }
        assertNull(dict!!.getHistory("かんじ"))
    }

    @Test
    fun testNewHistoryIsPrepended() {
        dict = newDict()
        dict!!.addHistory("かんじ", "漢字")
        dict!!.addHistory("かんじ", "感じ")
        assertEquals("/感じ/漢字/", dict!!.getHistory("かんじ"))
    }

    @Test
    fun testGetInstanceReturnsSameInstanceAfterClose() {
        SKKHistoryDictionary.resetInstanceForTest()
        try {
            val first = SKKHistoryDictionary.getInstance(File(dir, "history").path, "history")!!
            first.close()
            val second = SKKHistoryDictionary.getInstance(File(dir, "history").path, "history")!!
            // close() で instance = null にすると、同じ DB を二重オープンしてしまう
            assertSame(first, second)
            second.close()
        } finally {
            SKKHistoryDictionary.resetInstanceForTest()
        }
    }

    private fun newDict(): SKKHistoryDictionary =
        SKKHistoryDictionary.newInstance(File(dir, "history").path, "history")!!
}
