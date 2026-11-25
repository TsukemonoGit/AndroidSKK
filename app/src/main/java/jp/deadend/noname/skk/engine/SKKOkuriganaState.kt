package jp.deadend.noname.skk.engine

import jp.deadend.noname.skk.createTrimmedBuilder
import jp.deadend.noname.skk.skkPrefs

// 送り仮名入力中(▽モード，*つき)
class SKKOkuriganaState : SKKConfirmingState {
    override val isTransient = true
    override val icon = 0
    override var pendingLambda: (() -> Unit)? = null
    override var oldComposingText: String = ""

    override fun handleKanaKey(context: SKKEngine) {
        context.apply {
            if (skkPrefs.toggleKanaKey) {
                changeState(context.asciiState, true)
            } else {
                mComposing.setLength(0)
                changeState(context.hiraganaState)
            }
        }
    }

    override fun processKey(context: SKKEngine, keyCode: Int) {
        // シフトキーの状態をチェック
        val isUpper = Character.isUpperCase(keyCode)
        // 大文字なら，ローマ字変換のために小文字に戻す
        val codeLower = if (isUpper) Character.toLowerCase(keyCode) else keyCode

        context.apply {
            // l, L, q, / による暗黙の確定
            if (changeInputMode(keyCode)) return

            // 全角句読点: 送り仮名モードを終了し、mKanjiKeyに追加
            when (codeLower) {
                '！'.code, '？'.code, '。'.code, '、'.code -> {
                    mKanjiKey.append(codeLower.toChar())
                    mOkurigana = ""
                    mComposing.setLength(0)
                    setComposingTextSKK(mKanjiKey)
                    context.changeState(context.kanjiState)
                    updateSuggestions(mKanjiKey.toString())
                    return
                }
            }

            if (mComposing.length == 1 && mOkurigana.isEmpty()) {
                // 「ん」か「っ」を処理したらここで終わり
                val hiraganaChar = RomajiConverter.checkSpecialConsonants(mComposing[0], codeLower)
                if (hiraganaChar != null) {
                    mOkurigana = hiraganaChar
                    setComposingTextSKK(
                        createTrimmedBuilder(mKanjiKey)
                            .append('*').append(hiraganaChar).append(codeLower.toChar())
                    )
                    mComposing.setLength(0)
                    mComposing.append(codeLower.toChar())
                    return
                }
            }
            // 送りがなが確定すれば変換，そうでなければComposingに積む
            mComposing.append(codeLower.toChar())
            val hiraganaChar = RomajiConverter.convert(mComposing.toString())
            if (mOkurigana.isNotEmpty()) { //「ん」か「っ」がある場合
                if (hiraganaChar.isNotEmpty()) {
                    mComposing.setLength(0)
                    mOkurigana += hiraganaChar
                    conversionStart(mKanjiKey)
                } else {
                    setComposingTextSKK(
                        createTrimmedBuilder(mKanjiKey)
                            .append('*').append(mOkurigana).append(mComposing)
                    )
                }
            } else {
                if (hiraganaChar.isNotEmpty()) {
                    mComposing.setLength(0)
                    mOkurigana = hiraganaChar
                    conversionStart(mKanjiKey)
                } else {
                    if (!RomajiConverter.isIntermediateRomaji(mComposing.toString())) {
                        mComposing.setLength(0) // これまでの composing は typo とみなしてやり直す
                        if (mKanjiKey.isNotEmpty()) mKanjiKey.deleteCharAt(mKanjiKey.lastIndex)
                        changeState(context.kanjiState)
                        context.kanjiState.processKey(context, Character.toUpperCase(keyCode))
                        return
                    }
                    setComposingTextSKK(
                        createTrimmedBuilder(mKanjiKey).append('*').append(mComposing)
                    )
                }
            }
        }
    }

    override fun afterBackspace(context: SKKEngine) {
        context.apply {
            mComposing.setLength(0) // 元から空のはず
            if (mKanjiKey.isNotEmpty()) mKanjiKey.deleteCharAt(mKanjiKey.lastIndex)
            if (mOkurigana.isNotEmpty()) mKanjiKey.append(mOkurigana) // 「っ」とか
            mOkurigana = ""
            setComposingTextSKK(mKanjiKey)
            changeState(context.kanjiState)
        }
    }

    override fun handleCancel(context: SKKEngine): Boolean {
        context.apply {
            if (skkPrefs.preferFlick) mComposing.setLength(0) // Flickでアルファベットが残っても困る
            mOkurigana = ""
            if (mKanjiKey.isNotEmpty()) mKanjiKey.deleteCharAt(mKanjiKey.lastIndex) // composing と同じ子音アルファベットのはず
            changeState(context.kanjiState)
            setComposingTextSKK("${mKanjiKey}${mComposing}")
        }

        return true
    }

    override fun changeToFlick(context: SKKEngine): Boolean {
        return false
    }
}
