package jp.deadend.noname.skk.engine

import jp.deadend.noname.skk.R
import jp.deadend.noname.skk.hankaku2zenkaku

// 全角英数モード
class SKKZenkakuState : SKKState {
    override val isTransient = false
    override val icon =
        R.drawable.ic_full_alphabet

    override fun handleKanaKey(context: SKKEngine) {
        context.changeState(context.hiraganaState)
    }

    override fun processKey(context: SKKEngine, keyCode: Int) {
        hankaku2zenkaku(Char(keyCode).toString())?.let {
            context.commitTextSKK(it)
        }
    }

    override fun afterBackspace(context: SKKEngine) {}

    override fun handleCancel(context: SKKEngine): Boolean {
        return context.hiraganaState.handleCancel(context)
    }

    override fun changeToFlick(context: SKKEngine): Boolean {
        context.changeState(context.kanaState, true) // こちらはキーボードも変更
        return true
    }
}
