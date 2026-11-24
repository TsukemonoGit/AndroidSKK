package jp.deadend.noname.dialog

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import jp.deadend.noname.skk.dLog

/**
 * 非同期処理の完了後にダイアログを出すための安全な show。
 *
 * Activity が終了中・破棄済み・状態保存済みのときに
 * `DialogFragment.show()` を呼ぶと
 * `IllegalStateException: Can not perform this action after onSaveInstanceState`
 * で落ちるため、その手前で何もしない。
 */
fun FragmentActivity.showDialogSafely(fragment: DialogFragment, tag: String) {
    val stateSaved = supportFragmentManager.isStateSaved
    if (isFinishing || isDestroyed || stateSaved) {
        dLog(
            "skip showing dialog ($tag): " +
                "finishing=$isFinishing, destroyed=$isDestroyed, stateSaved=$stateSaved"
        )
        return
    }
    fragment.show(supportFragmentManager, tag)
}
