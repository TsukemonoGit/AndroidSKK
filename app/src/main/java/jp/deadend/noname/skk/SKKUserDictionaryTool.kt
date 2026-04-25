package jp.deadend.noname.skk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import jdbm.helper.Tuple
import jp.deadend.noname.dialog.ConfirmationDialogFragment
import jp.deadend.noname.dialog.SimpleMessageDialogFragment
import jp.deadend.noname.skk.databinding.ActivityUserDictToolBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ユーザー辞書管理専用のUI（明示登録単語のみを表示・削除・エクスポートできる） */
class SKKUserDictionaryTool : AppCompatActivity() {
    private lateinit var binding: ActivityUserDictToolBinding
    private var mUserDict: SKKUserDictionary? = null
    private val mEntryList = mutableListOf<Pair<String, String>>()
    private val mFilteredList = mutableListOf<Pair<String, String>>()
    private lateinit var mAdapter: android.widget.ArrayAdapter<String>

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        MainScope().launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    binding.userDictToolSearch.queryHint = "インポート中..."
                    // ActionBar からはアクセスできないため、ここは一旦飛ばすか Activity の invalidateOptionsMenu などを使う
                    // binding.userDictToolToolbar.menu.setGroupEnabled(0, false)
                }
                val name = getFileNameFromUri(this@SKKUserDictionaryTool, uri) ?: "unknown"
                val isGzip = name.endsWith(".gz")
                val isWordList = name.endsWith("combined.gz")
                val charset = if (!isWordList && contentResolver.openInputStream(uri)?.use { stream ->
                        val processedStream = if (isGzip) java.util.zip.GZIPInputStream(stream) else stream
                        isTextDictInEucJp(processedStream)
                    } == true) "EUC-JP" else "UTF-8"

                contentResolver.openInputStream(uri)?.use { stream ->
                    val processedStream = if (isGzip) java.util.zip.GZIPInputStream(stream) else stream
                    val recMan = mUserDict?.mRecMan ?: return@use
                    val btree = mUserDict?.mBTree ?: return@use
                    loadFromTextDict(processedStream, charset, isWordList, recMan, btree, false) {}
                }
            } catch (e: Exception) {
                Log.e("SKK", "UserDictionaryTool import error: $e")
                withContext(Dispatchers.Main) {
                    SimpleMessageDialogFragment.newInstance(
                        getString(R.string.error_file_load, e.message ?: "(null)")
                    ).show(supportFragmentManager, "dialog")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.userDictToolSearch.queryHint = ""
                    // binding.userDictToolToolbar.menu.setGroupEnabled(0, true)
                    loadEntryList()
                    filterList(binding.userDictToolSearch.query?.toString())
                }
            }
        }
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        MainScope().launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                    mEntryList.forEach { (key, value) ->
                        writer.write("$key $value\n")
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.message_tools_written_to_external_storage,
                            getFileNameFromUri(this@SKKUserDictionaryTool, uri)),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("SKK", "UserDictionaryTool export error: $e")
                withContext(Dispatchers.Main) {
                    SimpleMessageDialogFragment.newInstance(
                        getString(R.string.error_write_to_external_storage, e.message ?: "(null)")
                    ).show(supportFragmentManager, "dialog")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDictToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = bars.left, top = bars.top,
                right = bars.right, bottom = bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        setSupportActionBar(binding.userDictToolToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // DB初期化（直接 openDB を使う。isASCII=false なので extractDictionary は不要）
        val dictFile = filesDir.absolutePath + "/" + getString(R.string.dict_name_user)
        val btreeName = getString(R.string.btree_name)
        mUserDict = try {
            val (recMan, btree) = SKKUserDictionary.openDB(dictFile, btreeName)
            SKKUserDictionary.newInstanceFromOpenDB(recMan, btree, dictFile, btreeName)
        } catch (e: Exception) {
            Log.e("SKK", "UserDictionaryTool: failed to open userdict: $e")
            null
        }
        if (mUserDict == null) {
            SimpleMessageDialogFragment.newInstance(
                getString(R.string.error_tools_open_user_dict)
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 全件取得
        loadEntryList()
        mFilteredList.addAll(mEntryList)

        // リスト表示
        mAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mFilteredList.map { (k, v) -> "$k  ${formatDictEntry(k, v)}" }.toMutableList()
        )
        binding.userDictToolList.adapter = mAdapter
        binding.userDictToolList.emptyView = binding.EmptyListItem

        // 検索
        binding.userDictToolSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    filterList(newText)
                    return true
                }
            }
        )

        // 削除（タップでダイアログ）
        binding.userDictToolList.setOnItemClickListener { _, _, position, _ ->
            if (position >= mFilteredList.size) return@setOnItemClickListener
            val (key, value) = mFilteredList[position]
            val entry = mUserDict?.getEntry(key, value) ?: return@setOnItemClickListener
            val dialog = ConfirmationDialogFragment.newInstance(
                getString(R.string.message_tools_confirm_remove_entry) + "\n$key  $value"
            )
            dialog.setListener(object : ConfirmationDialogFragment.Listener {
                override fun onPositiveClick() {
                    // 候補ごとに削除（送り仮名なし前提でシンプルに）
                    entry.candidates.forEach { candidate ->
                        mUserDict?.removeEntry(key, candidate, "")
                    }
                    loadEntryList()
                    filterList(binding.userDictToolSearch.query?.toString())
                }
                override fun onNegativeClick() {}
            })
            dialog.show(supportFragmentManager, "dialog")
        }

        // (Moved importFileLauncher to class property)

    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_userdict_tool, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_userdict_tool_add -> {
                val dialog = jp.deadend.noname.dialog.DictEntryDialogFragment.newInstance("エントリの追加")
                dialog.setListener(object : jp.deadend.noname.dialog.DictEntryDialogFragment.Listener {
                    override fun onPositiveClick(key: String, value: String) {
                        try {
                            mUserDict?.let { dict ->
                                dict.addEntry(key, value, "")
                                loadEntryList()
                                filterList(binding.userDictToolSearch.query?.toString())
                            }
                        } catch (e: Exception) {
                            Log.e("SKK", "UserDictionaryTool error adding entry: ${e.message}")
                        }
                    }
                    override fun onNegativeClick() {}
                })
                dialog.show(supportFragmentManager, "add_entry")
                return true
            }
            R.id.menu_userdict_tool_import -> {
                // To avoid "mMenu.setGroupEnabled(0, false)" error in importFileLauncher, you can access the toolbar menu:
                // binding.userDictToolToolbar.menu.setGroupEnabled(0, false)
                importFileLauncher.launch(arrayOf("*/*"))
                return true
            }
            R.id.menu_userdict_tool_export -> {
                exportFileLauncher.launch("skk_userdict_export.txt")
                return true
            }
            R.id.menu_userdict_tool_clear -> {
                val dialog = jp.deadend.noname.dialog.ConfirmationDialogFragment.newInstance(
                        getString(R.string.message_tools_confirm_clear)
                )
                dialog.setListener(
                        object : jp.deadend.noname.dialog.ConfirmationDialogFragment.Listener {
                            override fun onPositiveClick() {
                                mUserDict?.clear()
                                loadEntryList()
                                filterList(binding.userDictToolSearch.query?.toString())
                            }
                            override fun onNegativeClick() {}
                        }
                )
                dialog.show(supportFragmentManager, "clear_dialog")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        mUserDict?.close()
        mUserDict = null
        // サービスに辞書の再オープンを通知
        if (SKKService.isRunning()) {
            val intent = Intent(this, SKKService::class.java)
            intent.putExtra(SKKService.KEY_COMMAND, SKKService.COMMAND_RELOAD_DICT)
            startService(intent)
        }
    }

    private fun loadEntryList() {
        mEntryList.clear()
        val dict = mUserDict ?: return
        val btree = dict.mBTree ?: return
        try {
            val tuple = jdbm.helper.Tuple<String, String>()
            val browser = btree.browse() ?: return
            while (browser.getNext(tuple)) {
                mEntryList.add(tuple.key to tuple.value)
            }
        } catch (e: Exception) {
            Log.e("SKK", "loadEntryList error: $e. Recreating UserDict.")
            dict.recreate()
        }
    }

    private fun formatDictEntry(key: String, rawValue: String): String {
        val entry = mUserDict?.getEntry(key, rawValue)
        return if (entry == null) {
            // パース失敗した場合は生の値をそのまま出す（フォールバック）
            rawValue
        } else {
            val normalCands = entry.candidates.joinToString(", ")
            val okuriganaStr = if (entry.okuriganaBlocks.isNotEmpty()) {
                val blocks = entry.okuriganaBlocks.joinToString(", ") { "${it.first}→${it.second}" }
                " [送り: $blocks]"
            } else ""
            normalCands + okuriganaStr
        }
    }

    private fun filterList(query: String?) {
        mFilteredList.clear()
        if (query.isNullOrEmpty()) {
            mFilteredList.addAll(mEntryList)
        } else {
            mFilteredList.addAll(
                mEntryList.filter { (k, v) -> k.contains(query) || v.contains(query) }
            )
        }
        mAdapter.clear()
        mAdapter.addAll(mFilteredList.map { (k, v) -> "$k  ${formatDictEntry(k, v)}" })
        mAdapter.notifyDataSetChanged()
    }
}
