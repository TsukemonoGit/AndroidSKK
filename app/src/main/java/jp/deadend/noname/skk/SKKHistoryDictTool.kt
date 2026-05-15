package jp.deadend.noname.skk

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import jp.deadend.noname.skk.databinding.ActivityUserDictToolBinding

/** 変換履歴管理専用のUI（履歴のみを表示・編集） */
class SKKHistoryDictTool : AppCompatActivity() {
    private lateinit var binding: ActivityUserDictToolBinding
    private var historyDict: SKKHistoryDictionary? = null
    private val entryList = mutableListOf<Pair<String, String>>()
    private val filteredList = mutableListOf<Pair<String, String>>()
    private lateinit var adapter: android.widget.ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDictToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars =
                    windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() or
                                    WindowInsetsCompat.Type.displayCutout()
                    )
            view.updatePadding(
                    left = bars.left,
                    top = bars.top,
                    right = bars.right,
                    bottom = bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        // DB初期化
        historyDict =
                SKKHistoryDictionary.getInstance(
                        filesDir.absolutePath + "/skk_historydict",
                        "skk_historydict"
                )
        if (historyDict == null) {
            return
        }

        // ListView初期化
        adapter =
                android.widget.ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        mutableListOf<String>()
                )
        binding.userDictToolList.adapter = adapter
        binding.userDictToolList.emptyView = binding.EmptyListItem

        // 検索機能
        binding.userDictToolSearch.setOnQueryTextListener(
                object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = true
                    override fun onQueryTextChange(newText: String?): Boolean {
                        filterList(newText)
                        return true
                    }
                }
        )

        // 削除機能（タップで削除ダイアログ）
        binding.userDictToolList.setOnItemClickListener { _, _, position, _ ->
            val item = filteredList[position]
            val dialog =
                    jp.deadend.noname.dialog.ConfirmationDialogFragment.newInstance(
                            "この履歴を削除しますか？\n${item.first}  ${item.second}"
                    )
            dialog.setListener(
                    object : jp.deadend.noname.dialog.ConfirmationDialogFragment.Listener {
                        override fun onPositiveClick() {
                            historyDict?.removeHistory(item.first)
                            loadHistoryList()
                            filterList(binding.userDictToolSearch.query?.toString())
                        }
                        override fun onNegativeClick() {}
                    }
            )
            dialog.show(supportFragmentManager, "dialog")
        }

        // Toolbar
        setSupportActionBar(binding.userDictToolToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        // 画面に戻るたびに最新の履歴を読み込む
        loadHistoryList()
        filterList(binding.userDictToolSearch.query?.toString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_historydict_tool, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_historydict_tool_clear -> {
                val dialog =
                        jp.deadend.noname.dialog.ConfirmationDialogFragment.newInstance(
                                getString(R.string.message_tools_confirm_clear_history)
                        )
                dialog.setListener(
                        object : jp.deadend.noname.dialog.ConfirmationDialogFragment.Listener {
                            override fun onPositiveClick() {
                                historyDict?.clear()
                                loadHistoryList()
                                filterList(binding.userDictToolSearch.query?.toString())
                            }
                            override fun onNegativeClick() {}
                        }
                )
                dialog.show(supportFragmentManager, "dialog")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadHistoryList() {
        entryList.clear()
        entryList.addAll(historyDict?.getAll() ?: emptyList())
    }

    private fun filterList(query: String?) {
        filteredList.clear()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(entryList)
        } else {
            filteredList.addAll(
                    entryList.filter { it.first.contains(query) || it.second.contains(query) }
            )
        }
        adapter.clear()
        adapter.addAll(
                filteredList.map {
                    // B8修正: empty → blank で空白のみの文字列もフィルタ
                    val formattedValue =
                            it.second.split("/").filter { s -> s.isNotBlank() }.joinToString(", ")
                    "${it.first}  $formattedValue"
                }
        )
        adapter.notifyDataSetChanged()
    }
}
