package jp.deadend.noname.skk

import android.util.Log
import java.io.File
import jdbm.RecordManager
import jdbm.RecordManagerFactory
import jdbm.btree.BTree
import jdbm.helper.StringComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SKKUserDictionary
private constructor(
        override var mRecMan: RecordManager?,
        override var mBTree: BTree<String, String>?,
        override val mIsASCII: Boolean,
        private val mDictFile: String,
        private val mBtreeName: String
) : SKKDictionaryInterface {
    override val mMutex = Mutex()
    private var mOldKey: String = ""
    private var mOldValue: String = ""

    class Entry(val candidates: List<String>, val okuriganaBlocks: List<Pair<String, String>>)

    fun getEntry(rawKey: String, rawValue: String? = null): Entry? {
        val key = katakana2hiragana(rawKey) ?: return null
        val value: String = rawValue ?: mBTree?.find(key) ?: return null

        // 正規表現で "/送/" と "/[り/送/]/" を拾う
        val (candidates, okuriganaStrings) =
                Regex("""((?<=/)[^\[\]/;][^/]*?(?=/)(?!]/))|((?<=/\[).+?(?=/]/))""")
                        .findAll(value)
                        .map { it.value }
                        .partition { !it.contains("/") }

        if (candidates.isEmpty()) {
            Log.e("SKK", "Invalid value found: Key=$key value=$value")
            return null
        }

        val okuriganaBlocks =
                okuriganaStrings.mapNotNull { block ->
                    block.split('/').let { pair ->
                        if (pair.size == 2) pair[0] to pair[1]
                        else
                                null.also {
                                    Log.e(
                                            "SKK",
                                            "Invalid: Key=$key okuriganaBlock=$block in $value"
                                    )
                                }
                    }
                }

        return Entry(candidates, okuriganaBlocks)
    }

    override fun getCandidates(rawKey: String): List<String>? =
            getEntry(rawKey)?.candidates?.distinct()

    // B14修正: mOldKey/mOldValue の読み書きを safeRun 内に統合
    fun addEntry(key: String, value: String, okurigana: String) {
        val oldVal = mBTree?.let { it.find(key) }

        val newVal =
                oldVal?.let { getEntry(key, it) }.let { entry ->
                    if (entry == null)
                            "/$value/" +
                                    if (okurigana.isNotEmpty()) "[$okurigana/$value/]/" else ""
                    else {
                        val candidates =
                                listOf(value).plus(entry.candidates).distinctBy { candidate ->
                                    candidate.takeWhile { it != ';' } // 注釈は無視して一致判定する
                                }

                        val okuriganaBlocks =
                                (if (okurigana.isEmpty()) listOf() else listOf(okurigana to value))
                                        .plus(entry.okuriganaBlocks)
                                        .distinctBy { pair ->
                                            pair.first to pair.second.takeWhile { it != ';' }
                                        }

                        candidates.fold("/") { acc, str -> "$acc$str/" } +
                                okuriganaBlocks.fold("") { acc, pair ->
                                    "$acc[${pair.first}/${pair.second}/]/"
                                }
                    }
                }

        safeRun {
            val oldKey = key
            val oldValue = oldVal.orEmpty()
            mOldKey = oldKey
            mOldValue = oldValue
            mBTree?.insert(key, newVal, true)
            mRecMan?.commit()
        }
    }

    // B15修正: 送りブロックを削除しても候補が残っている場合はエントリを維持
    fun removeEntry(key: String, value: String, okurigana: String) {
        val entry =
                getEntry(key)
                        ?: getEntry(
                                // 「だい4かい」がなければ「だい#かい」を削除する
                                key.replace(Regex("\\d+(\\.\\d+)?"), "#")
                        )
                                ?: return
        val candidates = entry.candidates.toMutableList()
        val okuriganaBlocks = entry.okuriganaBlocks.toMutableList()
        val rawVal = value.takeWhile { it != ';' } // 注釈を無視して探す

        // 送り仮名ブロックを削除
        okuriganaBlocks.removeIf { pair ->
            pair.first == okurigana && pair.second.takeWhile { it != ';' } == rawVal
        }

        // 候補を削除: 残り(okuriganaRemoved)の送りブロックからrawValが参照されている場合は候補を残す
        if (okuriganaBlocks.none { it.second.takeWhile { it2 -> it2 != ';' } == rawVal }) {
            candidates.removeIf { old ->
                old.takeWhile { it != ';' } == rawVal
            }
        }

        // B15修正: 候補も送りブロックも空になった場合のみ削除
        if (candidates.isEmpty() && okuriganaBlocks.isEmpty()) {
            replaceEntry(key, "")
        } else {
            val newVal =
                    candidates.fold("/") { acc, str -> "$acc$str/" } +
                            okuriganaBlocks.fold("") { acc, pair ->
                                "$acc[${pair.first}/${pair.second}/]/"
                            }
            replaceEntry(key, newVal)
        }
    }

    fun replaceEntry(key: String, value: String) {
        safeRun {
            // ここは再変換と関係ないので mOldKey / mOldValue を更新しない
            if (value.isEmpty() || Regex("/*").matchEntire(value) != null) {
                mBTree?.remove(key)
            } else {
                mBTree?.insert(key, value, true)
            }
            mRecMan?.commit()
        }
    }

    // B14修正: mOldKey/mOldValue の読み書きをすべて safeRun 内に行う
    fun rollBack() {
        if (mOldKey.isEmpty()) return

        safeRun {
            val oldKey = mOldKey
            val oldValue = mOldValue
            mOldKey = ""
            mOldValue = ""
            if (oldValue.isEmpty()) {
                mBTree?.remove(oldKey)
            } else {
                mBTree?.insert(oldKey, oldValue, true)
            }
            mRecMan?.commit()
        }
    }

    fun clear() {
        safeRun {
            val tuple = jdbm.helper.Tuple<String, String>()
            val browser = mBTree?.browse() ?: return@safeRun
            val keys = mutableListOf<String>()
            while (browser.getNext(tuple)) {
                keys.add(tuple.key)
            }
            for (key in keys) {
                mBTree?.remove(key)
            }
            mRecMan?.commit()
        }
    }

    override fun close() {
        safeRun { mRecMan?.commit() }
        super.close()
        mOldKey = ""
        mOldValue = ""
        mRecMan = null
        mBTree = null
    }

    fun reopen() {
        close()
        openDB(mDictFile, mBtreeName).let {
            mRecMan = it.first
            mBTree = it.second
        }
    }

    fun recreate() {
        Log.e("SKK", "UserDict force recreate called.")
        close()
        java.io.File("$mDictFile.db").delete()
        openDB(mDictFile, mBtreeName).let {
            mRecMan = it.first
            mBTree = it.second
        }
    }

    private inline fun <T> safeRun(crossinline block: () -> T): T =
            runBlocking(Dispatchers.IO) { mMutex.withLock { block() } }

    companion object {
        fun openDB(
                filename: String,
                btreeName: String
        ): Pair<RecordManager, BTree<String, String>> {
            val recMan = RecordManagerFactory.createRecordManager(filename)
            val recID = recMan.getNamedObject(btreeName)
            return if (recID == 0L) {
                val btree = BTree<String, String>(recMan, StringComparator())
                recMan.setNamedObject(btreeName, btree.recordId)
                recMan.commit()
                dLog("New user dictionary created")
                recMan to btree
            } else {
                try {
                    recMan to BTree<String, String>().load(recMan, recID)
                } catch (e: Exception) {
                    // 破損時はファイル削除して再生成
                    Log.e("SKK", "UserDict DB corrupted, recreating: $e")
                    recMan.close()
                    File(filename + ".db").delete()
                    val recMan2 = RecordManagerFactory.createRecordManager(filename)
                    val btree2 = BTree<String, String>(recMan2, StringComparator())
                    recMan2.setNamedObject(btreeName, btree2.recordId)
                    recMan2.commit()
                    dLog("User dictionary recreated after corruption")
                    recMan2 to btree2
                }
            }
        }

        fun newInstance(
                context: SKKService,
                mDictFile: String,
                btreeName: String,
                isASCII: Boolean
        ): SKKUserDictionary? {
            val dbFile = File("$mDictFile.db")
            if (isASCII && !dbFile.exists()) {
                context.extractDictionary(dbFile.nameWithoutExtension)
            }
            try {
                val (recMan, btree) = openDB(mDictFile, btreeName)
                return SKKUserDictionary(recMan, btree, isASCII, mDictFile, btreeName)
            } catch (e: Exception) {
                // 破損時はファイル削除して再生成（1回だけリトライ）
                Log.e("SKK", "UserDict open failed, retrying: $e")
                File("$mDictFile.db").delete()
                return try {
                    val (recMan, btree) = openDB(mDictFile, btreeName)
                    SKKUserDictionary(recMan, btree, isASCII, mDictFile, btreeName)
                } catch (e2: Exception) {
                    Log.e("SKK", "UserDict open failed after retry: $e2")
                    null
                }
            }
        }
        fun newInstanceFromOpenDB(
                recMan: RecordManager,
                btree: BTree<String, String>,
                dictFile: String,
                btreeName: String
        ): SKKUserDictionary =
                SKKUserDictionary(recMan, btree, false, dictFile, btreeName)
    }
}
