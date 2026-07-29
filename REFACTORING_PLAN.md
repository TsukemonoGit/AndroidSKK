# リファクタリング計画書

## 目的

Qwertyキーボードの「かな」キー（フリックボタン）と、フリックキーボードの「小」キー（濁点小文字ボタン）のバグ修正およびコードのリファクタリング。

---

## 判明したバグ

### バグ1: Qwertyキーボードの「かな」キー（Flickボタン）

**場所**: `app/src/main/java/jp/deadend/noname/skk/QwertyKeyboardView.kt` の `onRelease()` メソッド（約296-307行目）

**症状**:
- かなキーをタップしてもローマ字入力が確定せず、ただの「a」のまま
- かなキーを下フリックしてもフリック键盘に切替わらない
- 上フリック（貼り付け）と左フリック（絵文字）も機能しない

**原因**:
```kotlin
when (isFlicked) {
    if (skkPrefs.preferFlick) FLICK_NONE else FLICK_DOWN -> mService.changeToFlick()
    if (skkPrefs.preferFlick) FLICK_DOWN else FLICK_NONE -> mService.handleKanaKey()
    FLICK_UP -> mService.pasteClip()
    FLICK_LEFT -> mService.showEmojiPicker()
    else -> {}
}
```

`when (isFlicked)` の条件式で `if (skkPrefs.preferFlick) FLICK_NONE else FLICK_DOWN` を使用しているが、Kotlinの `when` は**等値チェック**として評価されるため、この式は `true` または `false` の Boolean を返す。つまり:
- `isFlicked == true` をチェックしている（あり得ない）
- `isFlicked == false` をチェックしている（あり得ない）

結果として、どの条件も一致せず `else -> {}` が実行される。

**設定 `preferFlick` の意味**:
- 設定名: 「フリック入力の使用」
- `preferFlick=true`（デフォルト）: フリック键盘を主に使う
  - かなキーをタップ → フリック键盘に切替
  - かなキーを下フリック → かな入力確定
- `preferFlick=false`: qwertyでローマ字入力する
  - かなキーをタップ → かな入力確定
  - かなキーを下フリック → フリック键盘に切替

**修正内容**:
```kotlin
when (isFlicked) {
    FLICK_LEFT -> mService.showEmojiPicker()
    FLICK_NONE -> if (skkPrefs.preferFlick) mService.changeToFlick() else mService.handleKanaKey()
    FLICK_UP -> mService.pasteClip()
    FLICK_DOWN -> if (skkPrefs.preferFlick) mService.handleKanaKey() else mService.changeToFlick()
    else -> {}
}
```

**状態**: ✅ 修正済み、テスト通過

---

### バグ2: フリックキーボードの「小」キー（こもじボタン）

**場所**: `app/src/main/java/jp/deadend/noname/skk/FlickJPKeyboardView.kt`

**症状**:
- 「ま」の下の「小」キー（こもじボタン）をクリックしても反応しない
- 濁点がつかない、小文字にならない

**分析中**:
- `KEYCODE_FLICK_JP_KOMOJI` の処理ロジックを調査中
- `release()` メソッド内で `mFlickState` の状態によって処理が分岐している
- `mLastPressedKey` と `mFlickState` の初期化タイミングが怪しい

**関連コード**（`release()`内）:
```kotlin
KEYCODE_FLICK_JP_KOMOJI -> {
    val smallState = if (skkPrefs.useSoftCancelKey) FlickState.UP else FlickState.NONE
    val cancelState = if (skkPrefs.useSoftCancelKey) FlickState.NONE else FlickState.UP
    when (mFlickState) {
        EnumSet.of(smallState) -> ...
        EnumSet.of(FlickState.LEFT) -> mService.changeLastChar(SKKEngine.LAST_CONVERSION_DAKUTEN)
        EnumSet.of(cancelState) -> mService.handleCancel()
        EnumSet.of(FlickState.RIGHT) -> mService.changeLastChar(SKKEngine.LAST_CONVERSION_HANDAKUTEN)
        EnumSet.of(FlickState.DOWN) -> mService.changeLastChar(SKKEngine.LAST_CONVERSION_SHIFT)
    }
}
```

**修正内容**:
```kotlin
MotionEvent.ACTION_DOWN -> {
    mFlickStartX = me.x
    mFlickStartY = me.y
    mArrowStartX = me.x
    mArrowStartY = me.y
    mFlickState = EnumSet.of(FlickState.NONE) // ← 追加
}
```

**原因**: `ACTION_DOWN` で `mFlickState` が初期化されていないため、前の操作のフリック状態が引き継がれていた。タップ（NONE）を期待しても、前の操作（例: 上フリック）の状態が残っていると、誤った判定になる。

**状態**: ✅ 修正済み、テスト通過

---

## 進捗

| ステップ | タスク | 状態 |
|----------|--------|------|
| 1 | テンプレートテスト作成 | ✅ 完了 |
| 2 | Qwertyかなキーのテスト作成 | ✅ 完了 |
| 3 | Qwertyかなキーのバグ修正 | ✅ 完了 |
| 4 | Qwertyかなキーのテスト実行 | ✅ 通過 |
| 5 | フリック小文字キーのバグ修正 | ✅ 完了 |
| 6 | AbbrevKeyboardViewの修正 | ✅ 完了 |
| 7 | 句読点キーpopup/入力不一致修正 | ✅ 完了 |
| 8 | 最終テスト実行 | ✅ 通過 |

---

## テスト関連

### 作成したテストファイル
- `app/src/test/java/jp/deadend/noname/skk/QwertyKanaKeyLogicTest.kt`
  - Qwertyかなキーの動作ロジックをテスト
  - `preferFlick=true/false` の各パターンを網羅

### テストの実行方法
```bash
./gradlew testDebugUnitTest --tests "jp.deadend.noname.skk.QwertyKanaKeyLogicTest"
```

### 既知の既存テスト失敗
- `RomajiConverterTest.testConvertLastChar` が失敗している（私の修正とは無関係）

---

## 注意事項

1. **`when` の条件式に `if` を書かない**: Kotlinの `when` は等値チェックとして評価される。条件分岐は `when` の外で `if-else` を使う。

2. **`isShifted` の影響**: Qwertyかなキーの処理では、`isShifted=true` の場合に `none↔up` が交換される仕様。修正では `isShifted` に関係なく直接 `isFlicked` で判定する方針（元のコードのコメントにもあり）。

3. **`flickUp`/`flickNone` 変数**: QwertyKeyboardView内で定義されているが、一般キーの入力処理（354行目）で使われている。かなキーの修正には影響しない。

4. **FlickJPKeyboardViewの複雑さ**: `processFlickForLetter()` が長大で、popupラベルと実際の入力の対応関係が見えにくい。リファクタリングの候補。

5. **popupラベルと入力の不一致**（追加バグ候補）:
   - 句読点キーのタップ時: popupラベルは `、`（句点）、実際の入力は `,`（カンマ）
   - 設定で表示と入力内容が異なる可能性がある

---

## 追加修正: 句読点キー popup/入力不一致

**場所**: `FlickJPKeyboardView.kt`

**不一致**:
| キー | popupラベル | 実際の入力 |
|------|-------------|------------|
| CHAR_TEN (NONE) | `、` | `,` (カンマ) |
| CHAR_TEN (LEFT) | `。` | `.` (ピリオド) |
| CHAR_TEN (UP) | `？` | `?` |
| CHAR_TEN (RIGHT) | `！` | `!` |
| CHAR_TEN_SHIFTED (LEFT) | `（` | `(` |
| CHAR_TEN_SHIFTED (UP) | `「` | `[` |
| CHAR_TEN_SHIFTED (RIGHT) | `）` | `)` |
| CHAR_TEN_SHIFTED (DOWN) | `」` | `]` |
| CHAR_TEN_NUM (全て) | 全角記号 | ASCII記号 |
| CHAR_TEN_NUM_LEFT (一部) | 全角記号 | ASCII記号 |

**修正内容**: popupラベルと一致させるために、ASCII記号を日本語全角記号に変更

**状態**: ✅ 修正済み

## 次のセッションでやるべきこと

1. ~~FlickJPKeyboardViewの `processFlickForLetter()` 関数のリファクタリング~~ ✅ 完了（13関数に分割）
2. ~~FlickJPKeyboardViewの `setupPopupTextView()` 関数のリファクタリング~~ ✅ 完了（6関数に分割）
3. ~~FlickJPKeyboardViewの `onSetShifted()` 関数のリファクタリング~~ ✅ 完了（6関数に分割）
4. ~~FlickJPKeyboardViewの `release()` 関数のリファクタリング~~ ✅ 完了（10関数に分割）
5. `suspendSuggestions()`/`resumeSuggestions()`呼び出しのパターン統一（try-finally化）
6. キー配置とpopupラベルの自動生成ロジックの見直し
7. 既存テスト `RomajiConverterTest.testConvertLastChar` の修正
8. バグ再調査: Shift→YA/YU/YO入力での変換モードキャンセル

---

## 2026-07-29 リファクタリング計画（第2弾）

### 分析：FlickJPKeyboardViewの構造問題

**問題点1: `processFlickForLetter()`の巨大化（約150行）**
- 15個のキーコードをwhen文で分岐
- 各caseが20〜40行に及ぶ
- `suspendSuggestions()`/`resumeSuggestions()`のパターンが重複
- Service直接呼び出しが埋め込まれていてテスト不可能

**問題点2: `setupPopupTextView()`の巨大化（約80行）**
- フリック状態（NONE/LEFT/UP/RIGHT/DOWN）ごとのpopupラベル表示ロジック
- 各caseで`isCurve()`判定と特殊ケース（YA/Ta/Wa）が埋め込み
- 15個のTextView操作が直列に並ぶ

**問題点3: `onSetShifted()`の巨大化（約50行）**
- シフトON/OFFで多数のキーのlabel/codesを変更
- 条件分岐が2分支のif-else
- キー変更ロジックが散在

**問題点4: `release()`の巨大化（約80行）**
- 15個以上のキーコードをwhen文で分岐
- Processor委譲が不完全（一部のみ）

### リファクタリング計画

#### Step 1: `processFlickForLetter()`の分割
- `processSingleLetterKey()` - 1母音キー共通処理（Aを除く）
- `processAKey()` - Aキー特別処理（カーブ・シフト対応）
- `processYAKey()` - YAキー（記号入力含む）
- `processWAKey()` - WAキー（複数パターン）
- `processTenKey()` - 句読点キー
- `processTenShiftedKey()` - 全角句読点キー
- `processTenNumKey()` - 記号数字キー
- `processTenNumLeftKey()` - 記号数字キーLEFT

#### Step 2: `setupPopupTextView()`の分割
- `setupPopupForFlickState()` - 各フリック状態ごとのラベル設定
- `getActiveLabelIndex()` - アクティブラベルのインデックス計算
- `highlightActiveLabel()` - 強調表示処理

#### Step 3: `onSetShifted()`の分割
- `updateTenKeyOnShift()` - 句読点キー変更
- `updateMojiKeyOnShift()` - モジキー変更
- `updateArrowKeysOnShift()` - 矢印キー変更

#### Step 4: `release()`の完全委譲
- 全ての分岐をProcessorに委譲

### 進捗

- [x] Step 1: processFlickForLetter()分割（13関数に分割）
- [x] Step 2: setupPopupTextView()分割（6関数に分割）
- [x] Step 3: onSetShifted()分割（6関数に分割）
- [x] Step 4: release()完全委譲（10関数に分割）
