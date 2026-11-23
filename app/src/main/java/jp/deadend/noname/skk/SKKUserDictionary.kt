package jp.deadend.noname.skk

import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
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

    /** 辞書を開き直すたびに増える。進行中のインポートが古い BTree を使い続けないために使う。 */
    @Volatile private var mGeneration = 0

    class Entry(val candidates: List<String>, val okuriganaBlocks: List<Pair<String, String>>)

    fun getEntry(rawKey: String, rawValue: String? = null): Entry? {
        val key = katakana2hiragana(rawKey) ?: return null
        // rawValue が渡された場合は DB を読まない。
        // 呼び出し元がすでにロックを持っている経路（addEntry など）でも安全。
        val value: String = rawValue ?: safeRun { mBTree?.find(key) } ?: return null
        return parseEntry(key, value)
    }

    /** BTree を読まずに値を解析する。呼び出し側でロックを取っていること。 */
    private fun parseEntry(key: String, value: String): Entry? {
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

    /** ロック保持中に呼ぶ読み取り。 */
    private fun findEntryLocked(key: String): Entry? {
        val value =
                try {
                    mBTree?.find(key)
                } catch (e: IOException) {
                    Log.e("SKK", "Error in getEntry(): $e")
                    null
                } ?: return null
        return parseEntry(key, value)
    }

    override fun getCandidates(rawKey: String): List<String>? =
            getEntry(rawKey)?.candidates?.distinct()

    // B14修正: mOldKey/mOldValue の読み書きを safeRun 内に統合
    // updateRollback=false は辞書ツール用。IME の再変換記録を上書きしない。
    fun addEntry(
            key: String,
            value: String,
            okurigana: String,
            updateRollback: Boolean = true
    ) {
        // 読み取り(find)も同じロック内で行う。
        // 外で読むと、直後に他スレッドの insert/commit が入った場合に
        // 古い値を元に新しい値を組み立ててしまう。
        safeRun {
            val oldVal = mBTree?.find(key)

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

            if (updateRollback) {
                mOldKey = key
                mOldValue = oldVal.orEmpty()
            }
            mBTree?.insert(key, newVal, true)
            mRecMan?.commit()
        }
    }

    // B15修正: 送りブロックを削除しても候補が残っている場合はエントリを維持
    fun removeEntry(key: String, value: String, okurigana: String) {
        // キー解決から書き込みまでを 1 つのロックで行う。
        // 途中で別スレッドの insert が入ると、古い内容を元に上書きしてしまう。
        safeRun {
            // 実際に存在するキーを 1 度だけ解決し、読み書きで同じキーを使う。
            // (以前は「#」版を読んで元のキーへ書いていたため、削除されずゴミが増えていた)
            val resolvedKey =
                    if (findEntryLocked(key) != null) key
                    else
                            key.replace(Regex("\\d+(\\.\\d+)?"), "#").takeIf {
                                findEntryLocked(it) != null
                            }
                                    ?: return@safeRun
            val entry = findEntryLocked(resolvedKey) ?: return@safeRun
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
                mBTree?.remove(resolvedKey)
            } else {
                val newVal =
                        candidates.fold("/") { acc, str -> "$acc$str/" } +
                                okuriganaBlocks.fold("") { acc, pair ->
                                    "$acc[${pair.first}/${pair.second}/]/"
                                }
                mBTree?.insert(resolvedKey, newVal, true)
            }
            mRecMan?.commit()
        }
    }

    fun replaceEntry(key: String, value: String) {
        safeRun {
            // ここは再変換と関係ないので mOldKey / mOldValue を更新しない
            if (value.all { it == '/' }) {
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

    /** 全エントリを取得する。件数が多い場合は IO スレッドから呼ぶこと。 */
    fun getAllEntries(): List<Pair<String, String>> =
            safeRun {
                val entries = mutableListOf<Pair<String, String>>()
                val btree = mBTree ?: return@safeRun entries
                val tuple = jdbm.helper.Tuple<String, String>()
                val browser = btree.browse()
                while (browser.getNext(tuple)) {
                    entries.add(tuple.key to tuple.value)
                }
                entries
            }

    /** 件数を返す。 */
    fun size(): Int = safeRun { mBTree?.size() ?: 0 }

    /** キーごと削除する。 */
    fun removeKey(key: String) {
        safeRun {
            mBTree?.remove(key)
            mRecMan?.commit()
        }
    }

    /**
     * テキスト辞書を読み込む。
     * ファイルの読み取りはロックの外で行い、1000 行ずつ BTree へ反映する。
     * これにより、インポート中も IME の読み書きが長時間ブロックされない。
     */
    fun importFromText(
            inputStream: InputStream,
            charset: String,
            isWordList: Boolean,
            onProgress: (count: Int) -> Unit = {}
    ): Boolean {
        var generation = -1
        val initial =
                safeRun {
                    generation = mGeneration
                    mBTree
                } ?: return false
        val loader = TextDictLineLoader(isWordList, false, initial)
        createTextDictReader(inputStream, charset).use { reader ->
            val batch = ArrayList<String>(IMPORT_BATCH_SIZE)
            var count = 0

            fun applyBatch(): Boolean =
                    safeRun {
                        // 初期化（recreate）で BTree が差し替わっていたら、古い辞書には書かない。
                        if (mGeneration != generation) return@safeRun false
                        batch.forEach(loader::loadLine)
                        mRecMan?.commit()
                        true
                    }

            while (true) {
                val line = reader.readLine() ?: break
                batch.add(line)
                if (batch.size >= IMPORT_BATCH_SIZE) {
                    if (!applyBatch()) return false
                    count += batch.size
                    batch.clear()
                    onProgress(count)
                }
            }
            if (batch.isNotEmpty()) {
                if (!applyBatch()) return false
                count += batch.size
                onProgress(count)
            }
        }
        return true
    }

    override fun close() {
        safeRun {
            try {
                mRecMan?.commit()
                mRecMan?.close()
            } catch (e: IOException) {
                Log.e("SKK", "Error closing user dictionary: $e")
            }
            mOldKey = ""
            mOldValue = ""
            mRecMan = null
            mBTree = null
            mGeneration++
        }
    }

    /**
     * 閉じたインスタンスを同じ参照のまま再オープンする。
     * シングルトンを参照している IME / ツールを無効にしないために必要。
     */
    private fun reopenIfClosed() {
        if (mRecMan != null && mBTree != null) return
        safeRun {
            if (mRecMan != null && mBTree != null) return@safeRun
            try {
                val (recMan, btree) = openDB(mDictFile, mBtreeName)
                mRecMan = recMan
                mBTree = btree
            } catch (e: Exception) {
                Log.e("SKK", "Failed to reopen user dictionary: $e")
            }
        }
    }

    /** 辞書を開き直す。シングルトンなので、既に開いていれば何もしない。 */
    fun reopen() {
        reopenIfClosed()
    }

    /**
     * 辞書を作り直す。
     * @param extract ファイル削除後に呼ばれる処理（アセットからの再展開など）。
     */
    fun recreate(extract: (() -> Unit)? = null) {
        Log.e("SKK", "UserDict force recreate called.")
        safeRun {
            try {
                mRecMan?.commit()
                mRecMan?.close()
            } catch (e: IOException) {
                Log.e("SKK", "Error closing user dictionary before recreate: $e")
            }
            mOldKey = ""
            mOldValue = ""
            mRecMan = null
            mBTree = null
            File("$mDictFile.db").delete()
            File("$mDictFile.lg").delete()
            extract?.invoke()
            val (recMan, btree) = openDB(mDictFile, mBtreeName)
            mRecMan = recMan
            mBTree = btree
        }
    }

    private inline fun <T> safeRun(crossinline block: () -> T): T =
            runBlocking(Dispatchers.IO) { mMutex.withLock { block() } }

    companion object {
        private const val IMPORT_BATCH_SIZE = 1000

        // プロセス内で同じ DB を 1 インスタンスに共有する。
        // IME と辞書ツールが別々に open すると JDBM が壊れるため、所有者を 1 つにする。
        private val sInstances = HashMap<String, SKKUserDictionary>()

        /**
         * 辞書の唯一の入口。同じファイルパスに対しては常に同じインスタンスを返す。
         * @param extract DB が無いときにアセットから展開する処理（ASCII 辞書用）。
         */
        @Synchronized
        fun getInstance(
                dictFile: String,
                btreeName: String,
                isASCII: Boolean = false,
                extract: (() -> Unit)? = null
        ): SKKUserDictionary? {
            val key = File(dictFile).absolutePath
            sInstances[key]?.let { instance ->
                instance.reopenIfClosed()
                return instance
            }
            val dbFile = File("$dictFile.db")
            if (isASCII && !dbFile.exists()) {
                extract?.invoke()
            }
            val instance = openInstance(dictFile, btreeName, isASCII) ?: return null
            sInstances[key] = instance
            return instance
        }

        private fun openInstance(
                dictFile: String,
                btreeName: String,
                isASCII: Boolean
        ): SKKUserDictionary? =
                try {
                    val (recMan, btree) = openDB(dictFile, btreeName)
                    SKKUserDictionary(recMan, btree, isASCII, dictFile, btreeName)
                } catch (e: Exception) {
                    // 破損時はファイル削除して再生成（1回だけリトライ）
                    Log.e("SKK", "UserDict open failed, retrying: $e")
                    File("$dictFile.db").delete()
                    try {
                        val (recMan, btree) = openDB(dictFile, btreeName)
                        SKKUserDictionary(recMan, btree, isASCII, dictFile, btreeName)
                    } catch (e2: Exception) {
                        Log.e("SKK", "UserDict open failed after retry: $e2")
                        null
                    }
                }

        /** テスト専用。プロセス内シングルトンを破棄する。 */
        @Synchronized
        internal fun resetInstancesForTest() {
            sInstances.values.forEach { runCatching { it.close() } }
            sInstances.clear()
        }

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

        /** テスト専用。既に開いた DB をラップする（シングルトンには登録しない）。 */
        internal fun newInstanceFromOpenDB(
                recMan: RecordManager,
                btree: BTree<String, String>,
                dictFile: String,
                btreeName: String
        ): SKKUserDictionary =
                SKKUserDictionary(recMan, btree, false, dictFile, btreeName)
    }
}
