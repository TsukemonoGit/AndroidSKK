package jp.deadend.noname.skk

import java.io.File
import jdbm.RecordManager
import jdbm.RecordManagerFactory
import jdbm.btree.BTree
import jdbm.helper.StringComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 変換履歴専用のユーザー辞書DB 明示登録用とは別ファイル・別BTreeで管理する */
class SKKHistoryDictionary private constructor(
        var mRecMan: RecordManager?,
        var mBTree: BTree<String, String>?,
        private val mDictFile: String,
        private val mBtreeName: String
) {
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
        logDbFileState("addHistory:before")
        safeRun {
            val existing = mBTree?.find(key)
            val newList =
                    if (existing != null) {
                        val currentList =
                                existing.split("/").filter { it.isNotEmpty() && it != value }
                        val updatedList = listOf(value) + currentList
                        "/" + updatedList.take(20).joinToString("/") + "/"
                    } else {
                        "/$value/"
                    }
            try {
                mBTree?.insert(key, newList, true)
                mRecMan?.commit()
                logDbFileState("addHistory:afterCommit")
            } catch (e: Exception) {
                try {
                    mRecMan?.rollback()
                } catch (_: Exception) {}
                logDbFileState("addHistory:exception")
                throw e
            }
        }
    }

    fun getHistory(key: String): String? = safeRun { mBTree?.find(key) }

    fun getAll(): List<Pair<String, String>> = safeRun {
        val list = mutableListOf<Pair<String, String>>()
        val btree = mBTree ?: return@safeRun list
        try {
            val tuple = jdbm.helper.Tuple<String, String>()
            val browser = btree.browse()
            while (browser.getNext(tuple)) {
                list.add(tuple.key to tuple.value)
            }
        } catch (e: Exception) {
            android.util.Log.e("SKKHistoryDictionary", "getAll error: ${e.message}")
        }
        list
    }

    fun removeHistory(key: String) {
        logDbFileState("removeHistory:before")
        safeRun {
            try {
                mBTree?.remove(key)
                mRecMan?.commit()
                logDbFileState("removeHistory:afterCommit")
            } catch (e: Exception) {
                try {
                    mRecMan?.rollback()
                } catch (_: Exception) {}
                logDbFileState("removeHistory:exception")
                throw e
            }
        }
    }

    fun clear() {
        logDbFileState("clear:before")
        safeRun {
            val tuple = jdbm.helper.Tuple<String, String>()
            val browser = mBTree?.browse() ?: return@safeRun
            val keys = mutableListOf<String>()
            while (browser.getNext(tuple)) {
                keys.add(tuple.key)
            }
            try {
                for (key in keys) {
                    mBTree?.remove(key)
                }
                mRecMan?.commit()
                logDbFileState("clear:afterCommit")
            } catch (e: Exception) {
                try {
                    mRecMan?.rollback()
                } catch (_: Exception) {}
                logDbFileState("clear:exception")
                throw e
            }
        }
    }

    // B17修正: close() で singleton instance = null しない。reopen() で再オープン可能にする
    fun close() {
        logDbFileState("close:before")
        safeRun { 
            mRecMan?.commit()
            mRecMan?.close()
        }
        logDbFileState("close:afterCommit")
        mRecMan = null
        mBTree = null
        instance = null // クローズ後はgetInstance()が新しいインスタンスを生成する
    }

    fun reopen() {
        logDbFileState("reopen:before")
        close()
        openDB(mDictFile, mBtreeName).let {
            mRecMan = it.first
            mBTree = it.second
        }
        logDbFileState("reopen:afterOpen")
        // close()でinstance = nullになるので復元
        instance = this
    }

    fun recreate() {
        logDbFileState("recreate:before")
        close()
        java.io.File("$mDictFile.db").delete()
        openDB(mDictFile, mBtreeName).let {
            mRecMan = it.first
            mBTree = it.second
        }
        logDbFileState("recreate:afterOpen")
        // close()でinstance = nullになるので復元（reopen() と同様に）
        // instance を復元しないとgetInstance()が新しいインスタンスを生成し、
        // 同一DBファイルへの二重オープンに繋がる
        instance = this
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
                SKKHistoryDictionary(recMan, btree, mDictFile, btreeName)
            } catch (e: Exception) {
                // 破損時はファイル削除して再生成（1回だけリトライ）
                File("$mDictFile.db").delete()
                try {
                    val (recMan, btree) = openDB(mDictFile, btreeName)
                    SKKHistoryDictionary(recMan, btree, mDictFile, btreeName)
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }
}
