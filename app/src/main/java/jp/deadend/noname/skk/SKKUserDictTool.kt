package jp.deadend.noname.skk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import jdbm.helper.Tuple
import jp.deadend.noname.dialog.ConfirmationDialogFragment
import jp.deadend.noname.dialog.DictEntryDialogFragment
import jp.deadend.noname.dialog.SimpleMessageDialogFragment
import jp.deadend.noname.dialog.showDialogSafely
import jp.deadend.noname.skk.databinding.ActivityUserDictToolBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.zip.GZIPInputStream
import kotlin.math.floor
import kotlin.math.sqrt

class SKKUserDictTool : AppCompatActivity() {
    private lateinit var mDictName: String
    private var mUserDict: SKKUserDictionary? = null
    private var mEntryList = mutableListOf<Tuple<String, String>>()
    private var mFoundList = mutableListOf<Tuple<String, String>>()
    private lateinit var mAdapter: EntryAdapter
    private lateinit var mSearchView: SearchView
    private lateinit var mSearchAdapter: EntryAdapter
    private lateinit var mMenu: Menu
    private var mSearchJob: Job = Job()
    private var mDatabaseJob: Job = Job()
    private var mInFileLauncher = false
    private val mDelay = 500L

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            mInFileLauncher = false
            return@registerForActivityResult
        }
        dLog("importing $uri")
        mSearchJob.cancel()
        mDatabaseJob.cancel()
        mDatabaseJob.invokeOnCompletion {
            mDatabaseJob = lifecycleScope.launch(Dispatchers.IO) {
                if (!openUserDict()) {
                    mInFileLauncher = false
                    throw CancellationException()
                }
                val dict = mUserDict ?: throw CancellationException()
                withContext(Dispatchers.Main) {
                    mAdapter.clear()
                    mMenu.setGroupEnabled(0, false)
                }
                val name = getFileNameFromUri(this@SKKUserDictTool, uri)!!
                val isGzip = name.endsWith(".gz")
                val isWordList = name.endsWith("combined.gz")

                try {
                    withContext(Dispatchers.Main) { mSearchView.queryHint = "文字セット識別中" }
                    val charset = if (!isWordList &&
                        contentResolver.openInputStream(uri)!!.use { inputStream ->
                            val processedInputStream =
                                if (isGzip) GZIPInputStream(inputStream) else inputStream
                            isTextDictInEucJp(processedInputStream)
                        }
                    ) "EUC-JP"
                    else "UTF-8"

                    withContext(Dispatchers.Main) { mSearchView.queryHint = "インポート中" }
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val processedInputStream =
                            if (isGzip) GZIPInputStream(inputStream) else inputStream
                        val completed =
                            dict.importFromText(processedInputStream, charset, isWordList) { count ->
                                if (floor(sqrt(count.toFloat())) % 50 == 0f) { // 味わい進捗
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        mSearchView.queryHint = "インポート中 ${count}行目"
                                    }
                                }
                            }
                        if (!completed) {
                            dLog("import aborted: user dictionary was recreated")
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        val message =
                            if (e is CharacterCodingException) {
                                getString(R.string.error_text_dict_coding)
                            } else {
                                getString(
                                    R.string.error_file_load,
                                    getFileNameFromUri(this@SKKUserDictTool, uri)
                                )
                            }
                        showDialogSafely(
                            SimpleMessageDialogFragment.newInstance(message),
                            "dialog"
                        )
                    }
                }
                closeUserDict()
                mDatabaseJob.invokeOnCompletion {
                    mInFileLauncher = false
                    lifecycleScope.launch(Dispatchers.Main) {
                        mMenu.setGroupEnabled(0, true)
                        updateListItems()
                    }
                }
            }
            mDatabaseJob.start()
        }
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) {
            mInFileLauncher = false
            return@registerForActivityResult
        }
        dLog("exporting $uri")
        if (mDatabaseJob.isActive) {
            Toast.makeText(
                applicationContext,
                "読み込みが終了してからエクスポートします",
                Toast.LENGTH_SHORT
            ).show()
        }
        mDatabaseJob.invokeOnCompletion {
            mDatabaseJob = lifecycleScope.launch(Dispatchers.IO) {
                if (!openUserDict()) {
                    mInFileLauncher = false
                    throw CancellationException()
                }
                var errorMessage = ""
                try {
                    val entries = mUserDict?.getAllEntries() ?: emptyList()
                    contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                        entries.forEach { (key, value) ->
                            writer.write("$key $value\n")
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "(null)"
                }
                closeUserDict()

                mDatabaseJob.invokeOnCompletion {
                    mInFileLauncher = false
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateListItems()
                        showDialogSafely(
                            SimpleMessageDialogFragment.newInstance(
                                if (errorMessage.isEmpty()) {
                                    getString(
                                        R.string.message_tools_written_to_external_storage,
                                        getFileNameFromUri(this@SKKUserDictTool, uri)
                                    )
                                } else {
                                    getString(
                                        R.string.error_write_to_external_storage,
                                        errorMessage
                                    )
                                }
                            ),
                            "dialog"
                        )
                    }
                }
            }
            mDatabaseJob.start()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        startServiceCommand(SKKService.COMMAND_RELOAD_DICT) // commit させる
        super.onCreate(savedInstanceState)
        mDictName = intent.dataString!!
        val binding = ActivityUserDictToolBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        setSupportActionBar(binding.userDictToolToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.Default) {
            delay(mDelay) // 無駄に一瞬 emptyView が出るのを防ぐための遅延
            withContext(Dispatchers.Main) {
                binding.userDictToolList.emptyView = binding.EmptyListItem
            }
        }
        binding.userDictToolList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                if (!mSearchView.isEnabled || mDatabaseJob.isActive) { // 読み込み中
                    return@OnItemClickListener
                }
                val dialog =
                    ConfirmationDialogFragment.newInstance(getString(R.string.message_tools_confirm_remove_entry))
                dialog.setListener(
                    object : ConfirmationDialogFragment.Listener {
                        override fun onPositiveClick() {
                            val adapter = parent.adapter as EntryAdapter
                            val item = adapter.getItem(position) ?: return
                            dLog("remove $item from $mDictName")
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    if (openUserDict()) {
                                        mUserDict?.removeKey(item.key)
                                        withContext(Dispatchers.Main) {
                                            mEntryList.remove(item)
                                            mFoundList.remove(item)
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                    closeUserDict()
                                } catch (e: Exception) {
                                    Log.e(
                                        "SKK",
                                        "UserDictTool error removing ${item.key}: ${e.message}"
                                    )
                                }
                            }
                        }

                        override fun onNegativeClick() {}
                    })
                showDialogSafely(dialog, "dialog")
            }

        mSearchView = binding.userDictToolSearch
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                mSearchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!mSearchView.isEnabled) {
                    if (mSearchView.query.isNotEmpty()) {
                        mSearchView.setQuery("", false)
                    }
                    return true
                }

                mSearchJob.cancel()
                if (newText == null) {
                    binding.userDictToolList.adapter = mAdapter
                    return true
                }
                val regex: Regex? by lazy {
                    try {
                        Regex(newText)
                    } catch (_: Exception) {
                        null
                    }
                }
                mFoundList.clear()
                mSearchJob.invokeOnCompletion {
                    mSearchJob = lifecycleScope.launch(Dispatchers.Default) {
                        mFoundList.addAll(mEntryList.filter {
                            ensureActive() // ここでキャンセルされる
                            if (regex != null) {
                                regex!!.containsMatchIn(it.key) || regex!!.containsMatchIn(it.value)
                            } else {
                                newText in it.key || newText in it.value
                            }
                        })
                        withContext(Dispatchers.Main) {
                            binding.userDictToolList.adapter = mSearchAdapter
                        }
                    }
                    mSearchJob.start()
                }
                return true
            }
        })

        mAdapter = EntryAdapter(this, mEntryList)
        binding.userDictToolList.adapter = mAdapter
        mSearchAdapter = EntryAdapter(this, mFoundList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        menuInflater.inflate(R.menu.menu_user_dict_tool, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_user_dict_tool_add -> {
                val dialog = DictEntryDialogFragment.newInstance("エントリの追加")
                dialog.setListener(object : DictEntryDialogFragment.Listener {
                    override fun onPositiveClick(key: String, value: String) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                if (openUserDict()) {
                                    // addEntry は新しい候補を先頭に追加し、重複を除く。
                                    // ツールからの編集で IME の再変換記録を上書きしない。
                                    mUserDict?.addEntry(
                                        key,
                                        value,
                                        "",
                                        updateRollback = false
                                    )
                                }
                                closeUserDict()
                            } catch (e: Exception) {
                                Log.e("SKK", "UserDictTool error adding entry: ${e.message}")
                            }
                            withContext(Dispatchers.Main) { updateListItems() }
                        }
                    }
                    override fun onNegativeClick() {}
                })
                showDialogSafely(dialog, "add_entry")
                return true
            }

            R.id.menu_user_dict_tool_import -> {
                mInFileLauncher = true
                importFileLauncher.launch(arrayOf("*/*"))
                return true
            }

            R.id.menu_user_dict_tool_export -> {
                mInFileLauncher = true
                exportFileLauncher.launch("$mDictName.txt")
                return true
            }

            R.id.menu_user_dict_tool_clear -> {
                val cfDialog = ConfirmationDialogFragment.newInstance(
                    getString(R.string.message_tools_confirm_clear)
                )
                cfDialog.setListener(
                    object : ConfirmationDialogFragment.Listener {
                        override fun onPositiveClick() {
                            recreateUserDict(extract = false)
                        }

                        override fun onNegativeClick() {}
                    })
                showDialogSafely(cfDialog, "dialog")
                return true
            }

            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        // FileLauncher 系統の ActivityResult の後にもここを通る
        if (!mInFileLauncher && mAdapter.count == 0) {
            when (val commandChar = mDictName.first()) {
                '*', '+' -> {
                    mDictName = mDictName.drop(1)
                    val cfDialog = ConfirmationDialogFragment.newInstance(
                        getString(R.string.message_tools_confirm_clear)
                    )
                    cfDialog.setListener(
                        object : ConfirmationDialogFragment.Listener {
                            override fun onPositiveClick() {
                                recreateUserDict(commandChar == '+')
                            }

                            override fun onNegativeClick() {
                                updateListItems()
                            }
                        })
                    showDialogSafely(cfDialog, "dialog")
                }

                else -> updateListItems()
            }
        }
    }

    public override fun onPause() {
        closeUserDict()
        mAdapter.clear()
        startServiceCommand(SKKService.COMMAND_RELOAD_DICT)

        super.onPause()
    }

    private fun recreateUserDict(extract: Boolean) {
        mAdapter.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            if (!openUserDict()) return@launch
            try {
                mUserDict?.recreate(
                    if (extract) {
                        {
                            try {
                                unzipFile(resources.assets.open("$mDictName.zip"), filesDir)
                                dLog("$mDictName.zip extracted")
                            } catch (e: IOException) {
                                Log.e("SKK", "I/O error in extracting $mDictName.zip: $e")
                            }
                        }
                    } else null
                )
            } catch (e: Exception) {
                Log.e("SKK", "UserDictTool error recreating $mDictName: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                if (extract) {
                    updateListItems()
                } else {
                    mAdapter.clear() // どうせ空なので updateListItems() 不要
                }
            }
        }
    }

    private fun onFailToOpenUserDict() {
        mUserDict = null
        lifecycleScope.launch(Dispatchers.Main) {
            ConfirmationDialogFragment.newInstance(getString(R.string.error_tools_open_user_dict))
                .let {
                    it.setListener(
                        object : ConfirmationDialogFragment.Listener {
                            override fun onPositiveClick() {
                                recreateUserDict(extract = false)
                            }

                            override fun onNegativeClick() {
                                startServiceCommand(SKKService.COMMAND_RELOAD_DICT)
                                finish()
                            }
                        })
                    showDialogSafely(it, "dialog")
                }
        }
    }

    private fun openUserDict(): Boolean {
        mUserDict?.let {
            return true
        }
        val isASCII = mDictName == getString(R.string.dict_name_ascii)
        val dict =
            try {
                SKKUserDictionary.getInstance(
                    filesDir.absolutePath + "/" + mDictName,
                    getString(R.string.btree_name),
                    isASCII
                ) {
                    try {
                        unzipFile(resources.assets.open("$mDictName.zip"), filesDir)
                    } catch (e: IOException) {
                        Log.e("SKK", "I/O error in extracting $mDictName.zip: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e("SKK", "openUserDict error: ${e.message}")
                null
            }
        if (dict == null) {
            Log.e("SKK", "openUserDict error: failed to open $mDictName")
            onFailToOpenUserDict()
            return false
        }
        mUserDict = dict
        dLog("UserDictTool: opened")
        return true
    }

    /**
     * シングルトンへの参照を落とす。DB 自体は IME と共有したまま閉じない。
     * 書き込みは各 API が commit 済みなので、ここで flush はしない
     * （インポート中にメインスレッドがロック待ちになるのを避ける）。
     */
    private fun closeUserDict() {
        mUserDict = null
        dLog("UserDictTool: detached")
    }

    private fun updateListItems() {
        dLog("updateListItems")
        mSearchJob.cancel()
        mDatabaseJob.cancel()
        mDatabaseJob.invokeOnCompletion {
            mSearchView.isEnabled = false
            mDatabaseJob = lifecycleScope.launch(Dispatchers.IO) {
                if (!openUserDict()) {
                    withContext(Dispatchers.Main) { mSearchView.isEnabled = true }
                    return@launch
                }
                try {
                    val items =
                        mUserDict?.getAllEntries()?.map { Tuple(it.first, it.second) }
                            ?: emptyList()
                    withContext(Dispatchers.Main) {
                        mAdapter.clear()
                        mAdapter.addAll(items)
                        mSearchView.isEnabled = true
                        mSearchView.queryHint = ""
                    }
                } catch (e: CancellationException) {
                    dLog("UserDictTool updateListItems: canceled")
                    closeUserDict()
                    throw e
                } catch (e: Exception) {
                    Log.e("SKK", "UserDictTool updateListItems: ${e.message}")
                    withContext(Dispatchers.Main) { onFailToOpenUserDict() }
                }
                closeUserDict()
                dLog("UserDictTool updateListItems: finished")
            }
            mDatabaseJob.start()
        }
    }

    private fun startServiceCommand(command: String) {
        if (SKKService.isRunning()) {
            val intent = Intent(this@SKKUserDictTool, SKKService::class.java)
            intent.putExtra(SKKService.KEY_COMMAND, command)
            startService(intent)
        }
    }

    private class EntryAdapter(
        context: Context,
        items: List<Tuple<String, String>>
    ) : ArrayAdapter<Tuple<String, String>>(context, 0, items) {
        private val mLayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = convertView
                ?: mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)

            val item = getItem(position) ?: Tuple("", "")
            (tv as TextView).text =
                context.getString(R.string.item_tools_entry, item.key, item.value)

            return tv
        }
    }
}
