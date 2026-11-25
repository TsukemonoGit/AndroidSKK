package jp.deadend.noname.skk.engine

import jp.deadend.noname.skk.R
import jp.deadend.noname.skk.hiragana2katakana
import jp.deadend.noname.skk.skkPrefs
import jp.deadend.noname.skk.zenkaku2hankaku

// 半角ｶﾀｶﾅモード
class SKKHanKanaState : SKKState {
    override val isTransient = false
    override val icon =
        R.drawable.ic_katakana

    override fun handleKanaKey(context: SKKEngine) {
        if (skkPrefs.toggleKanaKey) {
            context.changeState(context.asciiState, true)
        } else {
            context.changeState(context.hiraganaState)
        }
    }

    override fun processKey(context: SKKEngine, keyCode: Int) {
        if (context.changeInputMode(keyCode)) return
        context.hiraganaState.processKana(context, keyCode) { engine, hiraganaChar ->
            val str = zenkaku2hankaku(hiragana2katakana(hiraganaChar))
            if (str != null) engine.commitTextSKK(str)
            engine.mComposing.setLength(0)
        }
    }

    override fun afterBackspace(context: SKKEngine) {
        context.hiraganaState.afterBackspace(context)
    }

    override fun handleCancel(context: SKKEngine): Boolean {
        return context.hiraganaState.handleCancel(context)
    }

    override fun changeToFlick(context: SKKEngine): Boolean {
        return false
    }
}
