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

/** 変換履歴専用のユーザー辞書DB 明示登録用とは別ファイル・別BTreeで管理する */
class SKKHistoryDictionary
private constructor(
        var mRecMan: RecordManager?,
        var mBTree: BTree<String, String>?,
        private val mDictFile: String,
        private val mBtreeName: String
) {
    val mMutex = Mutex()

    fun addHistory(key: String, value: String) {
        safeRun {
            val existing = mBTree?.find(key)
            val newList = if (existing != null) {
                // スラッシュ区切りで保持し、最新のものを先頭にする（重複排除、上限20件）
                val currentList = existing.split("/").filter { it.isNotEmpty() && it != value }
                val updatedList = listOf(value) + currentList
                "/" + updatedList.take(20).joinToString("/") + "/"
            } else {
                "/$value/"
            }
            mBTree?.insert(key, newList, true)
            mRecMan?.commit()
        }
    }

    fun getHistory(key: String): String? = mBTree?.find(key)

    fun removeHistory(key: String) {
        safeRun {
            mBTree?.remove(key)
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

    fun close() {
        safeRun { mRecMan?.commit() }
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
        Log.e("SKK", "HistoryDict force recreate called.")
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
            if (recID == 0L) {
                val btree = BTree<String, String>(recMan, StringComparator())
                recMan.setNamedObject(btreeName, btree.recordId)
                recMan.commit()
                dLog("New history dictionary created")
                return recMan to btree
            }
            return try {
                recMan to BTree<String, String>().load(recMan, recID)
            } catch (e: Exception) {
                // 破損時はファイル削除して再生成
                Log.e("SKK", "HistoryDict DB corrupted, recreating: $e")
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
                Log.e("SKK", "HistoryDict open failed, retrying: $e")
                File("$mDictFile.db").delete()
                try {
                    val (recMan, btree) = openDB(mDictFile, btreeName)
                    SKKHistoryDictionary(recMan, btree, mDictFile, btreeName)
                } catch (e2: Exception) {
                    Log.e("SKK", "HistoryDict open failed after retry: $e2")
                    null
                }
            }
        }
    }
}
