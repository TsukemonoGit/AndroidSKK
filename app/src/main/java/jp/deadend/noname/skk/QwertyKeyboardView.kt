package jp.deadend.noname.skk

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.SparseArray
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.TextView
import jp.deadend.noname.skk.databinding.PopupFlickguideBinding
import jp.deadend.noname.skk.engine.SKKASCIIState
import jp.deadend.noname.skk.engine.SKKHiraganaState
import jp.deadend.noname.skk.engine.SKKKatakanaState
import jp.deadend.noname.skk.engine.SKKState
import jp.deadend.noname.skk.engine.SKKZenkakuState

class QwertyKeyboardView : KeyboardView, KeyboardView.OnKeyboardActionListener {
    val mLatinKeyboard: Keyboard by lazy {
        Keyboard(context, R.xml.qwerty, mService.mScreenWidth, mService.mScreenHeight)
    }
    val mSymbolsKeyboard: Keyboard by lazy {
        Keyboard(context, R.xml.symbols, mService.mScreenWidth, mService.mScreenHeight)
    }

    private var mSpacePressed = false
    private var mSpaceFlicked = false

    // カナキー押下中のフリック検知用
    private var mKanaKeyPressed = false

    // フリックガイド表示用
    private var mUsePopup = true
    private var mFixedPopup = false
    private var mPopup: PopupWindow? = null
    private var mPopupTextView: Array<TextView>? = null
    private val mPopupSize = 120
    private val mPopupOffset = intArrayOf(0, 0)
    private val mFixedPopupPos = intArrayOf(0, 0)
    private var mFixedPopupPosDirty = true

    // フリック方向ラベル (左, 上, 下)
    private val mFlickGuideLabelList = SparseArray<Array<String>>()

    private var mCurrentPopupLabels = arrayOf("", "", "", "", "", "", "")

    init {
        isPreviewEnabled = false
        val a = mFlickGuideLabelList
        // xml popup_flickguide.xml の TextView インデックスに合わせる
        // [0]=中央=かな, [1]=左=絵☻, [2]=上=貼り付け
        a.append(KEYCODE_QWERTY_TO_JP, arrayOf("かな", "絵☻", "貼り付け", "", "", "", ""))
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun setService(service: SKKService) {
        super.setService(service)
        keyboard = mLatinKeyboard
        onKeyboardActionListener = this
        isPreviewEnabled = false
        readPrefs(context)
    }

    override fun onDetachedFromWindow() {
        if (mPopup?.isShowing == true) mPopup!!.dismiss()
        super.onDetachedFromWindow()
        isShifted = false
        isCapsLocked = false
    }

    private fun readPrefs(context: Context) {
        mUsePopup = skkPrefs.usePopup
        if (mUsePopup) {
            mFixedPopup = skkPrefs.useFixedPopup
            if (mPopup == null) {
                val popup = createPopupGuide(context)
                mPopup = popup
                val binding = PopupFlickguideBinding.bind(popup.contentView)
                mPopupTextView = arrayOf(
                    binding.labelA,
                    binding.labelI,
                    binding.labelU,
                    binding.labelE,
                    binding.labelO,
                    binding.labelLeftA,
                    binding.labelRightA,
                    binding.labelLeftI,
                    binding.labelRightI,
                    binding.labelLeftU,
                    binding.labelRightU,
                    binding.labelLeftE,
                    binding.labelRightE,
                    binding.labelLeftO,
                    binding.labelRightO
                )
            }
        }
    }

    private fun createPopupGuide(context: Context): PopupWindow {
        val view = inflate(context, R.layout.popup_flickguide, null)

        val scale = context.resources.displayMetrics.density
        val size = (mPopupSize * scale + 0.5f).toInt()

        val popup = PopupWindow(view, size, size)
        popup.animationStyle = 0

        return popup
    }

    private fun setupPopupTextView() {
        if (!mUsePopup || mPopupTextView == null) return

        val labels = mPopupTextView!!
        labels.forEach { it.text = ""; it.setBackgroundResource(R.drawable.popup_label) }

        // xml popup_flickguide.xml の TextView インデックスに直接マッピング
        // [0]=labelA(中央), [1]=labelI(左), [2]=labelU(上), [3]=labelE(右), [4]=labelO(下)
        labels[0].text = mCurrentPopupLabels[0]
        labels[1].text = mCurrentPopupLabels[1]
        labels[2].text = mCurrentPopupLabels[2]
        labels[3].text = mCurrentPopupLabels[3]
        labels[4].text = mCurrentPopupLabels[4]

        // 現在操作中の方向をハイライト
        val flickIndex = when (isFlicked) {
            FLICK_LEFT -> 1
            FLICK_UP -> 2
            FLICK_DOWN -> 4
            else -> 0
        }
        labels[flickIndex].setBackgroundResource(R.drawable.popup_label_highlighted)
    }

    override fun handleBack(): Boolean {
        mService.clearCandidatesView()
        return super.handleBack()
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        if (key.codes[0] == KEYCODE_QWERTY_ENTER) {
            mService.pressSearch()
            return true
        }

        return super.onLongPress(key)
    }

    override fun onModifiedTouchEvent(me: MotionEvent, possiblePoly: Boolean): Boolean {
        when (me.action) {
            MotionEvent.ACTION_DOWN -> {
                flickStartX = me.x
                flickStartY = me.y
                isFlicked = FLICK_NONE
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = me.x - flickStartX
                val dy = me.y - flickStartY
                val dx2 = dx * dx
                val dy2 = dy * dy
                if (dx2 + dy2 > mFlickSensitivitySquared) {
                    when {
                        mSpacePressed -> {
                            if (dx2 > dy2 && dx2 > mFlickSensitivitySquared) {
                                if (dx < 0) {
                                    mService.keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT)
                                } else {
                                    mService.keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT)
                                }
                                mSpaceFlicked = true
                                flickStartX = me.x
                                flickStartY = me.y
                            } else if (dx2 < dy2 && dy2 > mFlickSensitivitySquared) {
                                if (dy < 0) {
                                    mService.keyDownUp(KeyEvent.KEYCODE_DPAD_UP)
                                } else {
                                    mService.keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN)
                                }
                                mSpaceFlicked = true
                                flickStartY = me.y
                                flickStartX = me.x
                            }
                            return true
                        }

                        mKanaKeyPressed -> {
                            // カナキー: 全方向をフリック検知
                            val prevFlicked = isFlicked
                            when {
                                dx2 > dy2 && dx2 > mFlickSensitivitySquared -> {
                                    isFlicked = if (dx < 0) FLICK_LEFT else FLICK_NONE
                                }
                                dy2 > mFlickSensitivitySquared -> {
                                    isFlicked = if (dy < 0) FLICK_UP else FLICK_DOWN
                                }
                                else -> {
                                    isFlicked = FLICK_NONE
                                }
                            }
                            // フリック方向変更時にガイドのハイライトを更新
                            if (isFlicked != prevFlicked && mUsePopup) {
                                setupPopupTextView()
                            }
                            return true
                        }

                        dy < 0 && dx2 < dy2 -> {
                            isFlicked = FLICK_UP
                            return true
                        }

                        dy > 0 && dx2 < dy2 -> {
                            isFlicked = FLICK_DOWN
                            return true
                        }

                        dx < 0 && dx2 > dy2 -> {
                            isFlicked = FLICK_LEFT
                            return true
                        }

                        else -> {
                            isFlicked = FLICK_NONE
                        }
                    }
                } else {
                    isFlicked = FLICK_NONE
                }
            }
        }
        return super.onModifiedTouchEvent(me, possiblePoly)
    }

    override fun onKey(primaryCode: Int) {
        when (primaryCode) {
            // repeatable
            Keyboard.KEYCODE_DELETE -> {
                if (!isCapsLocked) isShifted = false
                if (!mService.handleBackspace()) mService.pressDel()
            }
            // codes[0] 以外
            Keyboard.KEYCODE_CAPSLOCK -> {
                isShifted = true
                isCapsLocked = true
            }
        }
    }

    override fun onRelease(primaryCode: Int) {
        mSpacePressed = false
        mKanaKeyPressed = false
        mService.resumeSuggestions()

        // カナキーのフリックガイドポップアップを閉じる
        if (mUsePopup) {
            val popup = mPopup
            if (popup != null && popup.isShowing) {
                popup.dismiss()
            }
        }

        // シフトで up と none が交換される
        val flickNone = if (isShifted) FLICK_UP else FLICK_NONE
        val flickUp = if (isShifted) FLICK_NONE else FLICK_UP

        if (!mMiniKeyboardOnScreen) when (primaryCode) {
            // onKey で消費済み
            Keyboard.KEYCODE_DELETE, Keyboard.KEYCODE_CAPSLOCK -> {}
            // repeatable 以外
            Keyboard.KEYCODE_SHIFT -> {
                when (isFlicked) {
                    FLICK_NONE -> {
                        isShifted = !isShifted
                        isCapsLocked = false
                    }

                    else -> {
                        isShifted = true
                        isCapsLocked = true
                    }
                }
            }

            KEYCODE_QWERTY_ENTER -> {
                if (!isCapsLocked) isShifted = false
                if (!mService.handleEnter()) mService.pressEnter()
            }

            KEYCODE_QWERTY_TO_JP -> {
                // preferFlick 設定で通常タップ/下フリックの役割を切り替え
                when (isFlicked) {
                    if (skkPrefs.preferFlick) flickNone else FLICK_DOWN -> mService.changeToFlick()
                    if (skkPrefs.preferFlick) FLICK_DOWN else flickNone -> mService.handleKanaKey()
                    flickUp -> mService.pasteClip()
                    FLICK_LEFT -> mService.showEmojiPicker() // 絵文字
                    else -> {}
                }
            }

            KEYCODE_QWERTY_TO_SYM -> {
                if (!isCapsLocked) isShifted = false
                when (isFlicked) {
                    flickNone -> {
                        keyboard = mSymbolsKeyboard
                        isShifted = keyboard.isShifted
                        isCapsLocked = keyboard.isCapsLocked
                        // 記号は capslock にならない気がするが一応
                    }

                    flickUp -> mService.googleTransliterate()
                    FLICK_DOWN -> mService.handleCancel()
                }
            }

            KEYCODE_QWERTY_TO_LATIN -> {
                when (isFlicked) {
                    FLICK_NONE -> {
                        mService.mEngine.changeState(SKKASCIIState)
                        mService.changeSoftKeyboard(SKKASCIIState)
                    }
                    FLICK_UP -> {
                        mService.mEngine.changeState(SKKASCIIState)
                        mService.changeSoftKeyboard(SKKASCIIState)
                    }
                    FLICK_DOWN -> {
                        mService.handleCancel()
                    }
                    else -> {}
                }
            }

                else -> {
                    if (primaryCode == ' '.code && mSpaceFlicked) {
                        mService.updateSuggestionsASCII()
                        return
                    }

                    val shiftedCode = keyboard.shiftedCodes[primaryCode] ?: 0
                    val downCode = keyboard.downCodes[primaryCode] ?: 0
                    val code = when (isFlicked) {
                        FLICK_DOWN ->
                            if (downCode > 0) downCode else primaryCode

                        flickUp ->
                            if (shiftedCode > 0) shiftedCode else primaryCode

                        else -> primaryCode
                    }

                    // qwerty配列では通常ASCII入力、全角モードのみ全角を維持
                    if (mService.engineState === SKKZenkakuState) {
                        mService.processKeyIn(SKKZenkakuState, code)
                    } else {
                        mService.mEngine.changeState(SKKASCIIState)
                        mService.processKeyIn(SKKASCIIState, code)
                    }
                }
        }
        when (primaryCode) {
            Keyboard.KEYCODE_SHIFT, KEYCODE_QWERTY_TO_SYM, KEYCODE_QWERTY_TO_LATIN -> {}
            else -> if (keyboard === mLatinKeyboard && !isCapsLocked) isShifted = false
            // 記号モードでは普通の shift も capslock として扱う (対応する { と } 等に便利だろうから)
        }
        setKeyState(mService.engineState)
    }

    private fun findKeyByCode(code: Int) =
        keyboard.keys.find { it.codes[0] == code }

    override fun setKeyState(state: SKKState): QwertyKeyboardView {
        // カナキー: 状態に応じた表示
        val kanaKey = findKeyByCode(KEYCODE_QWERTY_TO_JP)
        // 一時状態 (候補選択等) は「確定」、ひらがな状態は「かな」、他は「貼付」
        val kanaLabel = if (state.isTransient) "確定" else "かな"
        val flickLabel = if (skkPrefs.preferFlick) "Flick" else "かな"
        val showKana = state !in listOf(SKKASCIIState, SKKZenkakuState) && !mService.isHiragana
        kanaKey?.on = showKana
        kanaKey?.label = if (state.isTransient) kanaLabel else if (showKana) flickLabel else "貼付\n☻ $flickLabel \n "
        val qKey = findKeyByCode('q'.code)
        qKey?.on = (state !in listOf(SKKASCIIState, SKKZenkakuState) && !mService.isHiragana)

        val lKey = findKeyByCode('l'.code)
        lKey?.on = (state === SKKASCIIState)

        isZenkaku = (state === SKKZenkakuState)

        invalidateAllKeys()
        return this
    }

    override fun onPress(primaryCode: Int) {
        mSpacePressed = (primaryCode == ' '.code)
        mSpaceFlicked = false
        if (mSpacePressed) {
            mService.suspendSuggestions()
        }

        // カナキー押下時のフリックガイド表示
        mKanaKeyPressed = (primaryCode == KEYCODE_QWERTY_TO_JP)
        if (mUsePopup && mKanaKeyPressed) {
            // フリックガイドラベルを設定（xmlのTextViewインデックスに合わせる）
            val labels = mFlickGuideLabelList.get(KEYCODE_QWERTY_TO_JP)
            if (labels != null) {
                for (i in labels.indices) {
                    if (i < mCurrentPopupLabels.size) {
                        mCurrentPopupLabels[i] = labels[i]
                    }
                }
            }
            setupPopupTextView()

            // ポップアップ位置を再計算
            mFixedPopupPosDirty = true
            if (mFixedPopupPosDirty) {
                calculatePopupPos()
                mFixedPopupPosDirty = false
            }

            val popup = mPopup
            if (popup != null) {
                if (mFixedPopup) {
                    popup.showAtLocation(
                        this,
                        Gravity.NO_GRAVITY,
                        mFixedPopupPos[0],
                        mFixedPopupPos[1]
                    )
                } else {
                    popup.showAtLocation(
                        this,
                        Gravity.NO_GRAVITY,
                        flickStartX.toInt() + mPopupOffset[0],
                        flickStartY.toInt() + mPopupOffset[1]
                    )
                }
            }
        }
    }

    override fun onText(text: CharSequence) {}

    override fun swipeRight() {}

    override fun swipeLeft() {}

    override fun swipeDown() {}

    override fun swipeUp() {}

    private fun calculatePopupPos() {
        val scale = context.resources.displayMetrics.density
        val size = (mPopupSize * scale + 0.5f).toInt()

        val offsetInWindow = IntArray(2)
        getLocationInWindow(offsetInWindow)
        val windowLocation = IntArray(2)
        getLocationOnScreen(windowLocation)
        mPopupOffset[0] = -size / 2
        mPopupOffset[1] = -windowLocation[1] + offsetInWindow[1] - size / 2
        mFixedPopupPos[0] = windowLocation[0] + this.width / 2 + mPopupOffset[0]
        mFixedPopupPos[1] = windowLocation[1] - size / 2 + mPopupOffset[1]
    }



    companion object {
        private const val KEYCODE_QWERTY_TO_JP = -1008
        private const val KEYCODE_QWERTY_TO_SYM = -1009
        private const val KEYCODE_QWERTY_TO_LATIN = -1010
        private const val KEYCODE_QWERTY_ENTER = -1011
        private const val FLICK_UP = 1
        private const val FLICK_NONE = 0
        private const val FLICK_DOWN = -1
        private const val FLICK_LEFT = -2
    }

}