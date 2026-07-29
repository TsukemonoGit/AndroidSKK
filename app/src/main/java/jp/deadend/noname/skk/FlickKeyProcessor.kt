package jp.deadend.noname.skk

import java.util.EnumSet
import jp.deadend.noname.skk.engine.SKKEngine
import jp.deadend.noname.skk.FlickState

/**
 * フリックキーの入力ロジックを処理するクラス
 * 
 * このクラスはServiceの依存を持たず、入力（キーコード+フリック状態）から
 * 出力する文字コードのシーケンスを計算するだけの純粋な関数を保持する。
 * これによりユニットテストが容易になる。
 */
class FlickKeyProcessor {

    /** フリック方向から母音を取得 */
    fun getVowelForFlick(flick: EnumSet<FlickState>): Int =
            when {
                flick.contains(FlickState.LEFT) -> 'i'.code
                flick.contains(FlickState.UP) -> 'u'.code
                flick.contains(FlickState.RIGHT) -> 'e'.code
                flick.contains(FlickState.DOWN) -> 'o'.code
                else -> 'a'.code
            }

    /** Aキーの出力シーケンス */
    fun processAKey(vowel: Int, flick: EnumSet<FlickState>, isLeftCurve: Boolean, isShifted: Boolean): Sequence<Int> = sequence {
        if (isLeftCurve) {
            yield('x'.code)
            yield(vowel)
        } else if (!isShifted) {
            yield(vowel)
        } else {
            yield(Character.toUpperCase(vowel.toChar()).code)
        }
    }

    /** カナキーの出力シーケンス */
    fun processKanaKey(
        consonant: Int,
        flick: EnumSet<FlickState>,
        isLeftCurve: Boolean,
        isShifted: Boolean
    ): Sequence<Any> = sequence {
        yield(if (isShifted) Character.toUpperCase(consonant.toChar()).code else consonant)

        if (isLeftCurve) {
            // t+u→小つ, y+a/u/o→小文字
            if ((consonant == 't'.code) || consonant == 'y'.code) {
                yield(getVowelForFlick(flick))
                yield(SKKEngine.LAST_CONVERSION_SMALL)
                return@sequence
            }
        }
        yield(getVowelForFlick(flick))
    }

    /** YAキーの出力シーケンス（記号入力含む） */
    fun processYaKey(flick: EnumSet<FlickState>, isCurve: Boolean, isRightCurve: Boolean): Sequence<Int> = sequence {
        val symbol = when {
            flick.contains(FlickState.LEFT) -> if (isCurve) '[' else '('
            flick.contains(FlickState.RIGHT) -> if (isCurve) ']' else ')'
            else -> null
        }

        if (symbol != null) {
            if (isRightCurve) yield('z'.code)
            yield(symbol.code)
            return@sequence
        }
        // 通常入力はy+vowel
        yield('y'.code)
        yield(getVowelForFlick(flick))
    }

    /** WAキーの出力シーケンス */
    fun processWaKey(
        flick: EnumSet<FlickState>,
        isShifted: Boolean
    ): Sequence<Int> = sequence {
        when (flick) {
            EnumSet.of(FlickState.NONE) -> {
                yield(if (isShifted) 'W'.code else 'w'.code)
                yield('a'.code)
            }
            EnumSet.of(FlickState.NONE, FlickState.CURVE_LEFT) -> {
                yield(if (isShifted) 'X'.code else 'x'.code)
                yield('w'.code)
                yield('a'.code)
            }
            EnumSet.of(FlickState.LEFT) -> {
                yield('w'.code)
                yield('o'.code)
            }
            EnumSet.of(FlickState.UP) -> {
                yield(if (isShifted) 'N'.code else 'n'.code)
                yield('n'.code)
            }
            EnumSet.of(FlickState.RIGHT) -> yield('-'.code)
            EnumSet.of(FlickState.DOWN) -> yield('~'.code)
        }
    }

    /** 句読点キーの出力シーケンス */
    fun processTenKey(flick: EnumSet<FlickState>): Sequence<Int> = sequence {
        val keyMap = mapOf(
            EnumSet.of(FlickState.NONE) to '、'.code,
            EnumSet.of(FlickState.LEFT) to '。'.code,
            EnumSet.of(FlickState.UP) to '？'.code,
            EnumSet.of(FlickState.RIGHT) to '！'.code,
        )
        when {
            flick in keyMap -> yield(keyMap[flick]!!)
            flick == EnumSet.of(FlickState.DOWN) -> {
                yield('z'.code)
                yield('。'.code)
            }
        }
    }

    /** 全角句読点キーの出力シーケンス */
    fun processTenShiftedKey(flick: EnumSet<FlickState>): Sequence<Int> = sequence {
        val keyMap = mapOf(
            EnumSet.of(FlickState.NONE) to ' '.code,
            EnumSet.of(FlickState.LEFT) to '（'.code,
            EnumSet.of(FlickState.UP) to '「'.code,
            EnumSet.of(FlickState.RIGHT) to '）'.code,
            EnumSet.of(FlickState.DOWN) to '」'.code,
        )
        keyMap[flick]?.let { yield(it) }
    }

    /** 記号数字キーの出力シーケンス */
    fun processTenNumKey(flick: EnumSet<FlickState>): Sequence<String> = sequence {
        val keyMap = mapOf(
            EnumSet.of(FlickState.NONE) to "，",
            EnumSet.of(FlickState.LEFT) to "．",
            EnumSet.of(FlickState.UP) to "－",
            EnumSet.of(FlickState.RIGHT) to "：",
            EnumSet.of(FlickState.DOWN) to "／",
        )
        keyMap[flick]?.let { yield(it) }
    }

    /** 記号数字キーLEFTの出力シーケンス */
    fun processTenNumLeftKey(flick: EnumSet<FlickState>): Sequence<String> = sequence {
        val keyMap = mapOf(
            EnumSet.of(FlickState.NONE) to "＃",
            EnumSet.of(FlickState.LEFT) to "￥",
            EnumSet.of(FlickState.UP) to "＋",
            EnumSet.of(FlickState.RIGHT) to "＄",
            EnumSet.of(FlickState.DOWN) to "＊",
        )
        keyMap[flick]?.let { yield(it) }
    }
}
