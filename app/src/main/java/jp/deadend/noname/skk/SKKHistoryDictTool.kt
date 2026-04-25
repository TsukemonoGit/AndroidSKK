package jp.deadend.noname.skk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.deadend.noname.skk.databinding.ActivityUserDictToolBinding

/** 変換履歴管理専用のUI（履歴のみを表示・編集） */
class SKKHistoryDictTool : AppCompatActivity() {
    private lateinit var binding: ActivityUserDictToolBinding
    private lateinit var historyDict: SKKHistoryDictionary
    private val entryList = mutableListOf<Pair<String, String>>()
    private val filteredList = mutableListOf<Pair<String, String>>()
    private lateinit var adapter: android.widget.ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDictToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DB初期化
        historyDict =
                SKKHistoryDictionary.newInstance(
                        filesDir.absolutePath + "/skk_historydict",
                        "skk_historydict"
                )
                        ?: return

        // 履歴全件取得
        loadHistoryList()
        filteredList.addAll(entryList)

        // ListViewに表示
        adapter =
                android.widget.ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        filteredList.map { "${it.first}  ${it.second}" }
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
                            historyDict.removeHistory(item.first)
                            loadHistoryList()
                            filterList(binding.userDictToolSearch.query?.toString())
                        }
                        override fun onNegativeClick() {}
                    }
            )
            dialog.show(supportFragmentManager, "dialog")
        }
    }

    private fun loadHistoryList() {
        entryList.clear()
        val btree = historyDict.mBTree ?: return
        val tuple = jdbm.helper.Tuple<String, String>()
        val browser = btree.browse()
        while (browser.getNext(tuple)) {
            entryList.add(tuple.key to tuple.value)
        }
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
        adapter.addAll(filteredList.map { "${it.first}  ${it.second}" })
        adapter.notifyDataSetChanged()
    }
}
