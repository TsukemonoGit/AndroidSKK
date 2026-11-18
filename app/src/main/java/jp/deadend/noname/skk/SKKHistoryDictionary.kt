package jp.deadend.noname.skk

import java.io.File
import java.io.IOException
import jdbm.RecordManager
import jdbm.RecordManagerFactory
import jdbm.btree.BTree
import jdbm.helper.StringComparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 変換履歴専用のユーザー辞書DB 明示登録用とは別ファイル・別BTreeで管理する */
class SKKHistoryDictionary private constructor(
        @Volatile var mRecMan: RecordManager?,
        @Volatile var mBTree: BTree<String, String>?,
        private val mDictFile: String,
        private val mBtreeName: String
) {
    // メインスレッドがJDBM（BTree）を待たないための読み取り専用キャッシュ。
    // JDBM構造が破損して find/browse が暴走しても、入力処理はブロックされない。
    private val mLock = Any()
    @Volatile
    private var mCache = HashMap<String, String>()
    // 非同期の削除がまだディスクへ反映されていないキー。
    // rebuildCache() がディスクの内容でキャッシュを作り直すとき、
    // 削除済みのキーを復活させないために使う。
    private val mPendingRemovals = HashSet<String>()
    @Volatile
    private var mPendingClear = false

    // JDBM永続化を非同期実行するスコープ。破損時の暴走を背景スレッドに閉じ込める。
    // サービス破棄時も最後のコミットを逃さないよう明示的にはキャンセルしない
    // （プロセス停止時はOSが後始末するため、リークは実害にならない）。
    private val mBgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun logDbFileState(tag: String) {
        try {
            val dbFile = File("$mDictFile.db")
            val exists = dbFile.exists()
            val size = if (exists) dbFile.length() else -1L
            val lastMod = if (exists) dbFile.lastModified() else -1L
            val procId = android.os.Process.myPid()
            val instHash = System.identityHashCode(this)
            android.util.Log.d(
                "SKKHistoryDictionary",
                "$tag: pid=$procId, inst=${instHash}, file=$mDictFile.db, exists=$exists, size=$size, lastMod=$lastMod"
            )
        } catch (e: Exception) {
            android.util.Log.e("SKKHistoryDictionary", "$tag: logDbFileState error: ${e.message}")
        }
    }

    val mMutex = Mutex()

    fun addHistory(key: String, value: String) {
        // キャッシュへの反映は主スレッドですぐ完了させる（候補確定時の応答を阻害しない）
        val newList: String
        synchronized(mLock) {
            val existing = mCache[key]
            newList =
                    if (existing != null) {
                        val currentList =
                                existing.split("/").filter { it.isNotEmpty() && it != value }
                        val updatedList = listOf(value) + currentList
                        "/" + updatedList.take(20).joinToString("/") + "/"
                    } else {
                        "/$value/"
                    }
            mCache[key] = newList
            mPendingRemovals.remove(key)
        }
        logDbFileState("addHistory:before")
        // JDBMへの永続化は非同期実行。破損時も主スレッドはブロックされない
        mBgScope.launch {
            try {
                safeRun {
                    mBTree?.insert(key, newList, true)
                    mRecMan?.commit()
                }
                logDbFileState("addHistory:afterCommit")
            } catch (e: Exception) {
                try {
                    mRecMan?.rollback()
                } catch (_: Exception) {}
                logDbFileState("addHistory:exception")
                android.util.Log.e("SKKHistoryDictionary", "addHistory persist error: ${e.message}")
            }
        }
    }

    fun getHistory(key: String): String? = synchronized(mLock) { mCache[key] }

    fun getAll(): List<Pair<String, String>> = synchronized(mLock) {
        mCache.map { it.key to it.value }
    }

    fun removeHistory(key: String) {
        synchronized(mLock) {
            mCache.remove(key)
            mPendingRemovals.add(key)
        }
        logDbFileState("removeHistory:before")
        mBgScope.launch {
            try {
                safeRun {
                    mBTree?.remove(key)
                    mRecMan?.commit()
                }
                synchronized(mLock) { mPendingRemovals.remove(key) }
                logDbFileState("removeHistory:afterCommit")
            } catch (e: Exception) {
                try {
                    mRecMan?.rollback()
                } catch (_: Exception) {}
                logDbFileState("removeHistory:exception")
                android.util.Log.e("SKKHistoryDictionary", "removeHistory persist error: ${e.message}")
            }
        }
    }

    fun clear() {
        synchronized(mLock) {
            mCache = HashMap()
            mPendingClear = true
            mPendingRemovals.clear()
        }
        logDbFileState("clear:before")
        mBgScope.launch {
            try {
                safeRun {
                    // JDBM BTreeのbrowse中にremoveすると構造が壊れるため、
                    // ファイルを削除して再生成する（SKKUserDictToolのrecreateUserDictと同様の方式）
                    mRecMan?.commit()
                    mRecMan?.close()

                    val dbFile = File("$mDictFile.db")
                    if (dbFile.exists()) {
                        if (!dbFile.delete()) {
                            throw IOException("Failed to delete $mDictFile.db")
                        }
                    }
                    // ファイルlg（ログ）も削除
                    val lgFile = File("$mDictFile.lg")
                    if (lgFile.exists()) {
                        if (!lgFile.delete()) {
                            throw IOException("Failed to delete $mDictFile.lg")
                        }
                    }
                    // 新しいBTreeを再生成
                    val (newRecMan, newBTree) = openDB(mDictFile, mBtreeName)
                    mRecMan = newRecMan
                    mBTree = newBTree
                    synchronized(mLock) { mPendingClear = false }
                    logDbFileState("clear:afterRecreate")
                }
            } catch (e: Exception) {
                logDbFileState("clear:exception")
                android.util.Log.e("SKKHistoryDictionary", "clear error: ${e.message}")
            }
        }
    }

    // B17修正元からの継承: close() 後も getInstance() は同じインスタンスを返す。
    // ここで instance = null にすると、次に getInstance() を呼んだ人が
    // 同じ DB ファイルを二重オープンしてしまう。
    fun close() {
        logDbFileState("close:before")
        // 非同期の add/remove が終わってから commit/close する（取りこぼし防止）
        drainBackground()
        val rm = mRecMan
        // 参照を先に外すことで、クローズ済みRMへの非同期書き込み競合を防ぐ
        mRecMan = null
        mBTree = null
        synchronized(mLock) {
            mCache = HashMap()
            mPendingRemovals.clear()
            mPendingClear = false
        }
        rm?.let {
            safeRun {
                it.commit()
                it.close()
            }
        }
        logDbFileState("close:afterCommit")
    }

    /** テスト・close() 前の待ち合わせ用。実行中のバックグラウンド永続化を完了させる。 */
    internal suspend fun awaitBackground() {
        val job = mBgScope.coroutineContext[Job] ?: return
        job.children.toList().forEach { it.join() }
    }

    private fun drainBackground() = runBlocking { awaitBackground() }

    fun reopen() {
        logDbFileState("reopen:before")
        close()
        // JDBM再オープンとキャッシュ再構築は非同期。破損時の暴走を主スレッドに持ち込まない
        mBgScope.launch {
            try {
                safeRun {
                    openDB(mDictFile, mBtreeName).let {
                        mRecMan = it.first
                        mBTree = it.second
                    }
                }
                rebuildCache()
                logDbFileState("reopen:afterOpen")
            } catch (e: Exception) {
                android.util.Log.e("SKKHistoryDictionary", "reopen error: ${e.message}")
            }
        }
    }

    fun recreate() {
        logDbFileState("recreate:before")
        close()
        java.io.File("$mDictFile.db").delete()
        // 再オープンとキャッシュ再構築は非同期
        mBgScope.launch {
            try {
                safeRun {
                    openDB(mDictFile, mBtreeName).let {
                        mRecMan = it.first
                        mBTree = it.second
                    }
                }
                rebuildCache()
                logDbFileState("recreate:afterOpen")
            } catch (e: Exception) {
                android.util.Log.e("SKKHistoryDictionary", "recreate error: ${e.message}")
            }
        }
    }

    // JDBMの全エントリをメモリキャッシュへ読み込む（背景スレッド専用）
    private fun rebuildCache() {
        val map = safeRun {
            val m = HashMap<String, String>()
            val btree = mBTree ?: return@safeRun m
            try {
                val tuple = jdbm.helper.Tuple<String, String>()
                val browser = btree.browse()
                while (browser.getNext(tuple)) {
                    m[tuple.key] = tuple.value
                }
            } catch (e: Exception) {
                android.util.Log.e("SKKHistoryDictionary", "rebuildCache error: ${e.message}")
            }
            m
        }
        synchronized(mLock) {
            // ディスクのスナップショットで丸ごと置き換えると、
            // 読み込み中に addHistory() された分が消える。
            // メモリ側（新しい）を優先してマージする。
            mCache = mergeHistoryDiskSnapshot(map, mCache, mPendingRemovals, mPendingClear)
        }
    }

    // 初期ロード時に背景スレッドでキャッシュを構築する
    private fun refreshCacheAsync() {
        mBgScope.launch {
            runCatching { rebuildCache() }
                    .onFailure {
                        android.util.Log.e("SKKHistoryDictionary", "refreshCache error: ${it.message}")
                    }
        }
    }

    private inline fun <T> safeRun(crossinline block: () -> T): T =
            runBlocking(Dispatchers.IO) { mMutex.withLock { block() } }

    companion object {
        @Volatile
        private var instance: SKKHistoryDictionary? = null

        fun getInstance(mDictFile: String, btreeName: String): SKKHistoryDictionary? {
            return instance ?: synchronized(this) {
                instance ?: newInstance(mDictFile, btreeName)?.also { instance = it }
            }
        }

        /** テスト専用。プロセス内シングルトンを破棄する。 */
        internal fun resetInstanceForTest() {
            synchronized(this) { instance = null }
        }

        fun openDB(
                filename: String,
                btreeName: String
        ): Pair<RecordManager, BTree<String, String>> {
            val recMan = RecordManagerFactory.createRecordManager(filename)
            val recID = recMan.getNamedObject(btreeName)
            if (recID == 0L) {
                val btree = BTree<String, String>(recMan, StringComparator())
                recMan.setNamedObject(btreeName, btree.recordId)
                recMan.commit()
                dLog("New history dictionary created")
                return recMan to btree
            }
            return try {
                val pair = recMan to BTree<String, String>().load(recMan, recID)
                // 正常に読み込めた場合のみバックアップを1つだけ作成
                try {
                    val dbFile = File("$filename.db")
                    val bakFile = File("$filename.db.bak")
                    if (dbFile.exists()) {
                        dbFile.copyTo(bakFile, overwrite = true)
                    }
                } catch (e: Exception) {}
                pair
            } catch (e: Exception) {
                // 破損時はファイル削除して再生成
                recMan.close()
                File("$filename.db").delete()
                val recMan2 = RecordManagerFactory.createRecordManager(filename)
                val btree2 = BTree<String, String>(recMan2, StringComparator())
                recMan2.setNamedObject(btreeName, btree2.recordId)
                recMan2.commit()
                dLog("History dictionary recreated after corruption")
                recMan2 to btree2
            }
        }

        fun newInstance(mDictFile: String, btreeName: String): SKKHistoryDictionary? {
            return try {
                val (recMan, btree) = openDB(mDictFile, btreeName)
                SKKHistoryDictionary(recMan, btree, mDictFile, btreeName).also { it.refreshCacheAsync() }
            } catch (e: Exception) {
                // 破損時はファイル削除して再生成（1回だけリトライ）
                File("$mDictFile.db").delete()
                try {
                    val (recMan, btree) = openDB(mDictFile, btreeName)
                    SKKHistoryDictionary(recMan, btree, mDictFile, btreeName).also { it.refreshCacheAsync() }
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * ディスクのスナップショットとメモリキャッシュをマージする。
 * - メモリにある値は「より新しい」として優先する
 * - 非同期削除待ちのキーはディスクに残っていても復活させない
 * - clear() の永続化が終わるまではディスクの内容を無視する
 */
internal fun mergeHistoryDiskSnapshot(
        disk: Map<String, String>,
        current: Map<String, String>,
        pendingRemovals: Set<String>,
        pendingClear: Boolean,
): HashMap<String, String> {
    if (pendingClear) return HashMap()
    val merged = HashMap(disk)
    merged.putAll(current)
    pendingRemovals.forEach { merged.remove(it) }
    return merged
}
