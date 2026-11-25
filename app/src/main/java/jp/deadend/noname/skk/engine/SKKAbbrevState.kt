package jp.deadend.noname.skk.engine

import jp.deadend.noname.skk.R
import jp.deadend.noname.skk.hankaku2zenkaku

// Abbrevモード(▽モード)
class SKKAbbrevState : SKKState {
    override val isTransient = true
    override val icon =
        R.drawable.ic_abbrev

    override fun handleKanaKey(context: SKKEngine) {
        context.changeState(context.hiraganaState)
    }

    override fun processKey(context: SKKEngine, keyCode: Int) {
        context.apply {
            // スペースで変換するかそのままComposingに積む
            when (keyCode) {
                ' '.code -> if (mKanjiKey.isNotEmpty()) conversionStart(mKanjiKey)
                17 -> {
                    // 全角変換
                    hankaku2zenkaku(mKanjiKey.toString())?.let { zen ->
                        commitTextSKK(zen)
                    }
                    handleKanaKey(context)
                }

                -1010 -> {
                    changeState(context.kanjiState)
                }

                else -> {
                    mKanjiKey.append(keyCode.toChar())
                    setComposingTextSKK(mKanjiKey)
                    updateSuggestions(mKanjiKey.toString())
                }
            }
        }
    }

    override fun afterBackspace(context: SKKEngine) {
        context.apply {
            setComposingTextSKK(mKanjiKey)
            updateSuggestions(mKanjiKey.toString())
        }
    }

    override fun handleCancel(context: SKKEngine): Boolean {
        context.mKanjiKey.setLength(0) // 確定させない
        context.changeState(context.hiraganaState)
        return true
    }

    override fun changeToFlick(context: SKKEngine): Boolean {
        context.changeState(context.kanaState, true)
        return true
    }
}
