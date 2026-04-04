package jp.deadend.noname.skk

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class EmojiPickerKeyboardView(context: Context) : FrameLayout(context) {
    private var mService: SKKService? = null
    private val mEmojiPickerView: EmojiPickerView
    private val mSymbolContainer: LinearLayout
    private val mSymbolGrid: RecyclerView
    private val mSymbolCategoryTabs: TabLayout
    private val mSearchEdit: EditText
    private var mCurrentSymbolCategory = 0
    private var mAllSymbols = SymbolData.categories
    private var mIsSearching = false

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_emoji_picker, this, true)

        mEmojiPickerView = view.findViewById(R.id.emoji_picker_view)
        mSymbolContainer = view.findViewById(R.id.symbol_container)
        mSymbolGrid = view.findViewById(R.id.symbol_grid)
        mSymbolCategoryTabs = view.findViewById(R.id.symbol_category_tabs)
        mSearchEdit = view.findViewById(R.id.emoji_search_edit)

        val backButton = view.findViewById<ImageButton>(R.id.emoji_back_button)
        backButton.setOnClickListener { mService?.hideEmojiPicker() }

        // Main tabs: Emoji / Symbols
        val tabLayout = view.findViewById<TabLayout>(R.id.emoji_tab_layout)
        tabLayout.addTab(tabLayout.newTab().setText("絵文字"))
        tabLayout.addTab(tabLayout.newTab().setText("記号"))
        tabLayout.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        when (tab.position) {
                            0 -> showEmoji()
                            1 -> showSymbols()
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab) {}
                    override fun onTabReselected(tab: TabLayout.Tab) {}
                }
        )

        // Emoji picker callback
        mEmojiPickerView.setOnEmojiPickedListener { emojiViewItem ->
            mService?.commitTextSKK(emojiViewItem.emoji)
        }

        // Symbol category tabs
        setupSymbolCategoryTabs()

        // Symbol grid
        mSymbolGrid.layoutManager = GridLayoutManager(context, spanCountForCategory(0))
        mSymbolGrid.adapter =
                SymbolAdapter(mAllSymbols[0].symbols, isHalfWidth = isHalfWidthCategory(0)) { symbol
                    ->
                    mService?.commitTextSKK(symbol)
                }

        // Search functionality for symbols
        mSearchEdit.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {}
                    override fun afterTextChanged(s: Editable?) {
                        val query = s?.toString()?.trim() ?: ""
                        if (mSymbolContainer.visibility == View.VISIBLE) {
                            filterSymbols(query)
                        }
                    }
                }
        )
    }

    fun setService(service: SKKService) {
        mService = service
    }

    private fun showEmoji() {
        mEmojiPickerView.visibility = View.VISIBLE
        mSymbolContainer.visibility = View.GONE
        mSearchEdit.hint = context.getString(R.string.emoji_picker_search_hint)
    }

    private fun showSymbols() {
        mEmojiPickerView.visibility = View.GONE
        mSymbolContainer.visibility = View.VISIBLE
        mSearchEdit.hint = context.getString(R.string.symbol_picker_search_hint)
        val query = mSearchEdit.text?.toString()?.trim() ?: ""
        if (query.isNotEmpty()) {
            filterSymbols(query)
        }
    }

    private fun setupSymbolCategoryTabs() {
        mSymbolCategoryTabs.removeAllTabs()
        for (category in mAllSymbols) {
            mSymbolCategoryTabs.addTab(mSymbolCategoryTabs.newTab().setText(category.name))
        }
        mSymbolCategoryTabs.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        mCurrentSymbolCategory = tab.position
                        (mSymbolGrid.layoutManager as? GridLayoutManager)?.spanCount =
                                spanCountForCategory(tab.position)
                        (mSymbolGrid.adapter as? SymbolAdapter)?.isHalfWidth =
                                isHalfWidthCategory(tab.position)
                        val query = mSearchEdit.text?.toString()?.trim() ?: ""
                        if (query.isEmpty()) {
                            (mSymbolGrid.adapter as? SymbolAdapter)?.updateData(
                                    mAllSymbols[tab.position].symbols
                            )
                        } else {
                            filterSymbols(query)
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab) {}
                    override fun onTabReselected(tab: TabLayout.Tab) {}
                }
        )
    }

    private fun filterSymbols(query: String) {
        if (query.isEmpty()) {
            (mSymbolGrid.adapter as? SymbolAdapter)?.updateData(
                    mAllSymbols[mCurrentSymbolCategory].symbols
            )
            return
        }

        // Search across all categories
        val results =
                mAllSymbols
                        .flatMap { category ->
                            category.symbols.filter { symbol ->
                                symbol.contains(query) || category.name.contains(query)
                            }
                        }
                        .distinct()
        (mSymbolGrid.adapter as? SymbolAdapter)?.updateData(results)
    }

    private fun isHalfWidthCategory(index: Int): Boolean {
        return index < mAllSymbols.size && mAllSymbols[index].name == "半角"
    }

    private fun spanCountForCategory(index: Int): Int {
        return if (isHalfWidthCategory(index)) 10 else 6
    }

    private class SymbolAdapter(
            private var symbols: List<String>,
            var isHalfWidth: Boolean = false,
            private val onSymbolClick: (String) -> Unit
    ) : RecyclerView.Adapter<SymbolAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view as TextView
        }

        fun updateData(newSymbols: List<String>) {
            symbols = newSymbols
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv =
                    TextView(parent.context).apply {
                        layoutParams =
                                ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        (48 * resources.displayMetrics.density).toInt()
                                )
                        textSize = 22f
                        gravity = android.view.Gravity.CENTER
                        setBackgroundResource(android.R.drawable.list_selector_background)
                        isClickable = true
                        isFocusable = true
                    }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val symbol = symbols[position]
            holder.textView.text = symbol
            val cellHeight = if (isHalfWidth) 38 else 48
            val textSz = if (isHalfWidth) 18f else 22f
            holder.textView.layoutParams.height =
                    (cellHeight * holder.textView.resources.displayMetrics.density).toInt()
            holder.textView.textSize = textSz
            holder.textView.setOnClickListener { onSymbolClick(symbol) }
        }

        override fun getItemCount() = symbols.size
    }
}
