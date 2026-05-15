# バグ・クラッシュリスクまとめ

### 🔴 クラッシュ間違いなし / ほぼ確実

#### B1. SKKService.onCreate() での辞書オープン失敗後の NPE 連鎖

ファイル: SKKService.kt:303-320

```kotlin
  if (dict == null) {
      // notificationを出す
      super.onDestroy()  // ← ここでonDestroyが呼ばれるが
  }
  return dict!!  // ← ここでnullなのでNPE
```

辞書が開けなかった場合、super.onDestroy() を呼んでから dict!! で強制終了。これは設計ミスだが、実害としては辞書DBが初回に作れない場合に即座にクラッシュ。

#### B2. unzipFile() の zipスリッパリング脆弱性

ファイル: SKKUtils.kt:381-401

```kotlin
  var ze: ZipEntry? // ← ?: は不要、コンパイルは通るが意図不明
  ...
  val f = File(outDir, ze.name)
  if (!f.canonicalPath.startsWith(outDir.canonicalPath)) {
      throw IOException("zip path traversal")
  }
```

ze.name に ../ が含まれている場合、File.canonicalPath は .db を含まないパスになるので、startsWith(outDir.canonicalPath) が false になり IOException を投げるが、ze を消費せずに次のループに進んでしまう（ze が nextEntry  
 で上書きされる）。一応防御はしてあるが、意図しない動作の可能性。

#### B3. SKKService.onCreate() の mUserDict が null のまま mEngine が使われる

ファイル: SKKService.kt:326-327

```kotlin
  mUserDict = openUserDictionary(...)  // ← nullの可能性
  mAsciiDict = openUserDictionary(...) // ← nullの可能性
  ...
  mEngine = SKKEngine(this@SKKService, dictList, mUserDict, mAsciiDict, mHistoryDict)
```

openUserDictionary が null を返した場合、mEngine コンストラクタに null が渡されるが、SKKEngine の引数は lateinit でないので mUserDict が null のままになり、辞書操作で NPE。

#### B4. CandidatesViewContainer の mService が null のまま requestChooseCandidate を呼ぶ

ファイル: SKKService.kt:1383-1393

```kotlin
  mInputView!!.apply { ... }
  val right = mScreenWidth - leftOffset - mInputView!!.keyboard.width
```

mInputView!! が複数箇所。mInputView が null なら即クラッシュ。onCreateInputView で null になる経路がある。

#### B5. SKKService.onCreateInputView() で wasGodan/wasFlick が破棄された後の mFlickJPInputView 参照

ファイル: SKKService.kt:594-610

```kotlin
  val wasGodan = mInputView?.equals(mGodanInputView) == true
  val wasFlick = mInputView?.equals(mFlickJPInputView) == true
  createInputView()  // ← ここで mFlickJPInputView が new される
  // mFlickJPInputView は createInputView() で null → new される
```

createInputView() は mFlickJPInputView = FlickJPKeyboardView(...) を実行するので、wasGodan/wasFlick の判定は正しく古い値を保持している。ただし、createInputView() で mEmojiPickerView = null  
 になるので、mIsEmojiPickerShown の状態が失われる可能性あり。

#### B6. SKKEngine 内の hiragana2katakana() の !! 地獄

ファイル: SKKEngine.kt:179, 1189

```kotlin
  // line 179
  if (state === SKKHanKanaState) zenkaku2hankaku(mComposing.toString())!!

  // line 1189
  val hira = if (kanaState === SKKHiraganaState) s else katakana2hiragana(s)!!
```

zenkaku2hankaku と hiragana2katakana は null を返す可能性があるが、!! で強制参照。s が null の場合は即クラッシュ。

#### B7. SKKUserDictionary.getEntry() の !!

ファイル: SKKUserDictionary.kt:101

```kotlin
  userEntry!!.okuriganaBlocks.any { ... }
```

userEntry が null なのに !! で参照 → NPE。

────────────────────────────────────────────────────────────────────────────────

### 🟡 論理バグ / 予期せぬ動作

#### B8. 履歴辞書が SKKEngine で使われているが UI 表示とフォーマットが異なる

ファイル: SKKHistoryDictTool.kt と SKKEngine.kt の間

- SKKHistoryDictionary.addHistory() は /候補1/候補2/.../ 形式で保存
- SKKUserDictionary.getEntry() は /候補;注釈/[送り/候補/]/ 形式をパース
- UIツールは split("/") で表示しているが、内部DBフォーマットが混在していると正しく表示されない

#### B9. RomajiConverter.convertLastChar() で 2文字以上渡される可能性

ファイル: SKKEngine.kt:404-414

```kotlin
  fun changeLastChar(type: String) {
      ...
      // SKKKanjiState ブランチ
      val idx = s.length - 1
      val newLastChar = RomajiConverter.convertLastChar(s.substring(idx), type).second
      // この convertLastChar に 2 文字が渡ることはない
```

コメントに「2文字が渡ることはない」とあるが、mKanjiKey が空文字列の場合、substring(idx) が例外を投げる可能性。

#### B10. SKKEngine.processKey() が state で分岐するが、state が変数なのでスレッドセーフでない

ファイル: SKKEngine.kt:126

```kotlin
  fun processKey(keyCode: Int) = state.processKey(this, keyCode)
```

state は var で mutable。processKey 実行中に他スレッドから changeState() が呼ばれると、予期せぬ状態遷移になる（ただし現状はメインスレッド単一で使われているので実害は低い）。

#### B11. SKKEngine.updateSuggestions() のコルーチン競合

ファイル: SKKEngine.kt:929-970

```kotlin
  mUpdateSuggestionsJob.cancel()
  mUpdateSuggestionsJob.invokeOnCompletion {
      mUpdateSuggestionsJob = MainScope().launch(Dispatchers.Default) { ... }
      mUpdateSuggestionsJob.start()
  }
```

cancel() と invokeOnCompletion の間にタイポがある。invokeOnCompletion のブロックの中で mUpdateSuggestionsJob = MainScope().launch(...) を代入してから start() しているが、Job は launch() の結果として既に active なので  
 start() は不要（ただしエラーにはならない）。ただし、cancel() したJobがまだ完了していないうちに新しいJobを作ると、両方が走ってしまう可能性。

#### B12. SKKService.onStartInput() の keyboardType で "ignore" 以外の場合に例外

ファイル: SKKService.kt:735

```kotlin
  else -> throw Exception("invalid keyboardType: $keyboardType")
```

"ignore" 以外の値が来ると即 Exception。inputType が予期しない値の場合、クラッシュする。

#### B13. SKKService.restorePrevStates() で prev.keyboard が null なら it.keyboard = kb で落ちる

ファイル: SKKService.kt:700-712

```kotlin
  internal fun restorePrevStates() {
      mPrevStates?.let { prev ->
          ...
          prev.keyboard?.let { kb -> it.keyboard = kb }
          ...
      }
  }
```

これは let でガードしているのでセーフだが、prev.inputView が null の場合、it.keyboard = kb は実行されない。

#### B14. SKKUserDictionary.addEntry() の mOldKey/mOldValue のスレッドセーフ

ファイル: SKKUserDictionary.kt:92-96

```kotlin
  fun addEntry(key: String, value: String, okurigana: String) {
      ...
      safeRun {
          mOldKey = key
          mOldValue = oldVal.orEmpty()
          ...
      }
  }
```

safеRun は runBlocking(Dispatchers.IO) でMutexロックしているが、mOldKey と mOldValue は safeRun 外からも読み取られる（rollBack() から）。Mutexで保護されていない。

#### B15. SKKUserDictionary.removeEntry() の送り仮名ブロックの削除バグ

ファイル: SKKUserDictionary.kt:115-121

```kotlin
  if (okuriganaBlocks.isEmpty() ||
      !okuriganaBlocks.removeIf { pair ->
          pair.first == okurigana && pair.second.takeWhile { it != ';' } == rawVal
      }
  )
      candidates.removeIf { old -> old.takeWhile { it != ';' } == rawVal }
```

「送りブロックが残らない場合は丸ごと消す」とあるが、これは送りブロックを削除した後、かつ候補も削除した結果、エントリが空になる場合にのみ全削除すべき。現在は「送りブロックが1つでも削除されたら全消去」という誤った判定に
なっている。

────────────────────────────────────────────────────────────────────────────────

### 🟢 軽微だが気になるところ

#### B16. SKKService.isRunning() の NPE キャッチが NullPointerException をキャッチしている

```kotlin
  internal fun isRunning(): Boolean {
      return try {
          instance?.ping() ?: false
      } catch (_: NullPointerException) {
          false
      }
  }
```

instance が null の場合は ?. で安全。ping() が null を返すことはない。この catch は不要。

#### B17. SKKHistoryDictionary.close() で instance = null している

```kotlin
  fun close() {
      ...
      synchronized(SKKHistoryDictionary::class.java) {
          if (instance === this) {
              instance = null  // ← SKKService.onDestroy() から close() が呼ばれる
          }
      }
  }
```

SKKService.onDestroy() で mEngine.closeUserDict() → mHistoryDict?.close() → instance = null。しかし SKKHistoryDictTool からも getInstance() を使っているので、ツール起動時に singleton が null になっている可能性。

#### B18. SKKService.onUnbind() で stopSelf() を強制呼び出し

```kotlin
  override fun onUnbind(intent: Intent?): Boolean {
      MainScope().launch { stopSelf() }
      return false // rebind 不可能
  }
```

rebind 不可能にして stopSelf() するのは、InputMethodService として正しいかもしれないが、システムが onRebind() を呼ぶことを期待している場合に問題になる可能性がある。

#### B19. SKKDictManager.downloadDict() のキャンセルジョブ処理

```kotlin
  progressJob.let {
      it.cancelAndJoin()
      ...
  }
```

cancelAndJoin() の後、URL.openStream().use { ... } がまだ実行されていない。つまりキャンセルした後にネットワークアクセスを実行している。

#### B20. SKKUtils.hankaku2zenkaku() の H2Z マップが 0x10000 以上のコードポイントを使う

```kotlin
  H2Z[0x10000 + it.code]?.let { d -> skipNext = true; Char(d) }
```

0x10000 以上の値を Char() コンストラクタに渡すと、UTF-16のサロゲートペアにならないため、不正なCharになる。

────────────────────────────────────────────────────────────────────────────────

### 全体としての感想

このプロジェクトは非常に複雑な状態遷移マシンを1つの巨大な SKKEngine.kt (1430行) で実現している。主要な問題点は:

1.  null安全性の不足 — !! が多数散在し、特に SKKService と SKKEngine でNPEのリスクが高い
2.  状態管理のスレッドセーフでない — state や mComposing などが mutableで、コルーチンから非同期にアクセスされる箇所がある
3.  辞書DBの整合性リスク — ユーザー辞書と変換履歴の分離が部分的に実装されているが、整合性チェックが弱い
4.  UIとロジックの密結合 — SKKService が1498行もあり、UI操作と入力ロジックが混在している
5.  辞書フォーマットの混在 — ユーザー辞書と変換履歴で同じJDBM BTreeを使いつつ、異なるフォーマットを扱っている

---

# 対応ログ

修正サマリー:

┌─────┬─────────────────────────┬───────────────────────────────────────────────────────────────────────────────┬──────┐  
 │ Bug │ ファイル │ 内容 │ 状態 │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B1 │ SKKService.kt │ openUserDictionary() null 許容型化 + super.onDestroy() 削除 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B3 │ SKKService.kt │ mUserDict/mAsciiDict を var (nullable) に変更 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B4 │ SKKService.kt │ mInputView!! → ?., let で安全アクセス化 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B5 │ SKKService.kt │ 絵文字ピッカー表示状態を保持 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B6 │ SKKEngine.kt │ zenkaku2hankaku(s)!!, mCandidateList!!, katakana2hiragana(s)!! を null 安全化 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B7 │ SKKUserDictionary.kt │ getEntry() の null チェック明確化 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B8 │ SKKHistoryDictTool.kt │ split("/").filter → isNotBlank() │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B9 │ RomajiConverter.kt │ 空文字列バリデーション + hankaku2zenkaku()!! 除去 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B11 │ SKKEngine.kt │ updateSuggestions() のコルーチン競合修正 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B14 │ SKKUserDictionary.kt │ addEntry()/rollBack() の mOldKey/mOldValue を safeRun 内で処理 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B15 │ SKKUserDictionary.kt │ 送りブロック削除時に候補が残っていればエントリ維持 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B16 │ SKKService.kt │ isRunning() の不要な NPE catch 削除 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B17 │ SKKHistoryDictionary.kt │ close() で singleton = null しなく │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B18 │ SKKService.kt │ onUnbind() の stopSelf() をメインスレッドから呼 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B19 │ SKKDictManager.kt │ downloadDict() のキャンセル競合解消 │ ✅ │  
 ├─────┼─────────────────────────┼───────────────────────────────────────────────────────────────────────────────┼──────┤  
 │ B20 │ SKKUtils.kt │ Char(0x10000+) → String(charArrayOf()) でサロゲートペア対応 │ ✅ │  
 └─────┴─────────────────────────┴───────────────────────────────────────────────────────────────────────────────┴──────┘

---

追加

### Fix 1: SKKService.kt (+354〜+357) — 履歴辞書nullでIME起動不可

問題: mHistoryDict が null の場合 return で早期終了し、mEngine が lateinit  
 のまま未初期化で終了。その後のキー入力で UninitializedPropertyAccessException が発生。

修正: 履歴辞書が開けなくても return せず、null のまま SKKEngine に渡すように変更。SKKEngine は  
 mHistoryDict を安全参照（?.）で使うため、null でも正常に動作可能。

### Fix 2: SKKUserDictionary.kt (+116〜+136) — 候補リストの意図せぬ削除

問題: candidates.removeIf が送り仮名ブロックの有無に関わらず常に候補を削除。例:  
 /送る/[ら/送/][り/送/]/ で removeEntry("おく", "送る", "ら") を呼ぶと、基底候補 送る も削除され  
 [り/送/] のみ残ってしまう。

修正: 送り仮名ブロックを削除した後に、残りのブロックから rawVal  
 が参照されているか確認。参照されていれば候補を残す（送り仮名変換の候補として機能し続ける）。

修正内容 (SKKUtils.kt +37〜+57):

skipNext = true を if (d/h != null) ブロック内に移動しました。

- 修正前: skipNext = true が常に実行 → 有声バリアントがない文字 (ｦﾞ etc.) に続く ﾞ/ﾟ  
  が無視されて消える
- 修正後: skipNext = true は変換成功時のみセット → 有声バリアントがない場合は ﾞ/ﾟ  
  が次のイテレーションで通常処理される

### Fix 1: SKKService.kt (+346〜+353) — ユーザー辞書null時のmEngine未初期化

問題: mUserDict または mAsciiDict が null の場合、return で早期終了し mEngine (lateinit var)  
 が未初期化のまま。その後のキー入力で UninitializedPropertyAccessException 発生。

修正: return の代わりに stopSelf() を呼び、サービスを明示的に停止する。これにより onDestroy()  
 が呼ばれ、クリーンなシャットダウンが行われる。

### Fix 2: SKKEngine.kt (+733〜+754) — updateSuggestions()のデータ競合

問題: cancel() の直後に launch()  
 で新規Jobを起動するため、旧Jobがサスペンションポイントのないループ実行中に両Jobが並走し、mComplet
ionList/mCandidateList への代入が競合する。

修正: invokeOnCompletion  
 を使って旧Jobの完了を待ってから新規Jobを起動するパターンに変更。旧Jobが既に完了している場合は即時
起動、実行中の場合は invokeOnCompletion  
 コールバックで完了を待つ。これにより旧コードの直列化パターンを再現し、データ競合を防止する。

### 修正内容: SKKEngine.kt (+730〜+739) — invokeOnCompletion ハンドラ蓄積による競合

問題のシナリオ:

1.  updateSuggestions("あ") → invokeOnCompletion { launch("あ") } を job1 に登録
2.  すぐに updateSuggestions("い") → mUpdateSuggestionsJob はまだ job1 → invokeOnCompletion {  
    launch("い") } を job1 に追加（2つ目）
3.  さらに updateSuggestions("う") → job1 に3つ目の handler を追加
4.  job1 完了時 → 3つの launch が同時発火 → 古い結果が最新を上書き  


修正: invokeOnCompletion を使わず、Job(parent) でJobチェーンを構築する方式に変更。

- 各 updateSuggestions で Job(mUpdateSuggestionsJob) を新しい親として作成
- 各 Job は直前に作成した Job を親として持つ
- launch(context = Dispatchers.Default + job) で新規 Job の完了をトリガーとして自動的に次の Job  
  が起動
- これにより どの Job にも handler は常に1つだけ 蓄積せず、常に最後の str  
  の結果のみが最終的に残る
