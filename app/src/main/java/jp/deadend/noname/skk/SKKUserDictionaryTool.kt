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
            mFilteredList.map { (k, v) -> "$k  $v" }.toMutableList()
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

        // ツールバーメニュー（エクスポートのみ提供）
        binding.userDictToolToolbar.inflateMenu(R.menu.menu_userdict_tool)
        binding.userDictToolToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_userdict_tool_export -> {
                    exportFileLauncher.launch("skk_userdict_export.txt")
                    true
                }
                android.R.id.home -> {
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
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
        val tuple = Tuple<String, String>()
        val browser = btree.browse() ?: return
        while (browser.getNext(tuple)) {
            mEntryList.add(tuple.key to tuple.value)
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
        mAdapter.addAll(mFilteredList.map { (k, v) -> "$k  $v" })
        mAdapter.notifyDataSetChanged()
    }
}
