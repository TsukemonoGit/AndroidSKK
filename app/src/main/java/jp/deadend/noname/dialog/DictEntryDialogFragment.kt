package jp.deadend.noname.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DictEntryDialogFragment : DialogFragment() {
    private var mListener: Listener? = null

    interface Listener {
        fun onPositiveClick(key: String, value: String)
        fun onNegativeClick()
    }

    fun setListener(listener: Listener) {
        this.mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val padding = (16 * context.resources.displayMetrics.density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val keyLabel = TextView(context).apply { text = "読み（ひらがな）" }
        val keyEdit = EditText(context).apply {
            setSingleLine()
            hint = "例: かおもじ"
        }

        val valueLabel = TextView(context).apply { text = "単語" }
        val valueEdit = EditText(context).apply {
            setSingleLine()
            hint = "例: ₍ ･ᴗ･ ₎"
        }

        layout.addView(keyLabel)
        layout.addView(keyEdit)
        layout.addView(valueLabel)
        layout.addView(valueEdit)

        return AlertDialog.Builder(context)
            .setTitle(arguments?.getString("title") ?: "エントリの追加")
            .setView(layout)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val key = keyEdit.text.toString().trim()
                val value = valueEdit.text.toString().trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    mListener?.onPositiveClick(key, value)
                }
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                mListener?.onNegativeClick()
                dismiss()
            }
            .create()
    }

    companion object {
        fun newInstance(title: String): DictEntryDialogFragment {
            val frag = DictEntryDialogFragment()
            val args = Bundle()
            args.putString("title", title)
            frag.arguments = args
            return frag
        }
    }
}
