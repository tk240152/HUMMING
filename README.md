# HUMMING

JavaFX で作るゲームのひな型です。チームのルールは [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。

## 起動方法

**チームの標準：VS Code で `HUMMING` フォルダを開き、`app/src/main/java/org/example/App.java` の「Run」を押す。**

- リポジトリ全体（`intern-2026-output`）ではなく、**`HUMMING` フォルダを開く**こと
- ステージの設定は `app/src/main/resources/stages/stageN/stage.json`、プレイヤーの設定は HUMMING 直下の `player_config.properties` が使われる

コマンドで起動する場合は次のとおりです（PR の前のテストはこちらで行う）。

```bash
gradlew run
```

### pull したあとに動かない・おかしいとき

1. コマンドパレット（Ctrl+Shift+P）→ **「Java: Clean Java Language Server Workspace」** → 再起動（VS Code が持っている古いビルドを消す）
2. それでも直らなければ `gradlew clean build` を実行して、エラーメッセージを確認する

### 最初に表示する画面を選ぶ（コマンドで起動するとき）

```bash
gradlew run --args=game
```

| 引数 | 最初に表示する画面 |
|---|---|
| なし / `title` | タイトル |
| `game` | ゲーム |
| `result` | リザルト |

## 操作（ひな型の状態）

| キー | 動作 |
|---|---|
| F12 / 最大化ボタン | フルスクリーンにする（縦横比を保って拡大。余白は黒）。フルスクリーン中に F12 で元のウィンドウに戻る（Esc はゲーム用に空けてある） |
| P | 次の画面へ進む（開発用。どの画面でも、その画面の `next()` の画面へ進む） |
| W A S D | プレイヤー（四角形）を動かす |
| F3 | デバッグ表示の切り替え（ゲーム画面） |
| O | 効果音 `debug` を鳴らす（ゲーム画面。確認用） |
| 1 / 2 | 全体の音量を下げる / 上げる（デバッグ表示中） |
| 3 / 4 | BGM の音量を下げる / 上げる（デバッグ表示中） |
| 5 / 6 | 効果音の音量を下げる / 上げる（デバッグ表示中） |
| M | ミュートの切り替え（デバッグ表示中） |
| R | 設定ファイル（`stages/stageN/stage.json`・`player_config.properties`・`effect_config.properties`）を読み直す（ゲーム画面） |

**P・F3・O・1〜6・M・R と、ステージ選択の F2・リザルトの R は開発用のキーです。** ふだんは効きません。
使うときは `app/src/main/java/org/example/core/DevMode.java` の `ENABLED` を `true` にします（配布するときは `false` に戻す）。

## ステージの設定（`stages/stage1` 〜 `stages/stage5`）

ステージごとに `app/src/main/resources/stages/stageN/stage.json` に「BGM・背景・ランクの基準・花の設定（`flower`）・イベント（`events`）」を書きます。**ステージの作り方とルールは [stages/README.md](app/src/main/resources/stages/README.md) を参照**してください。

**ステージを作るときの大事なルール**

- 共通のイベントや、ほかのステージのイベント（ステージ1の `BreezeEvent` など）の動きを少し変えたくなったら、**元のクラスは直さずに、自分のステージのフォルダにコピーして別の名前で登録する**（ほかのステージに影響するのを防ぐ。やり方は stages/README.md）
- **PR の前に `gradlew build` を実行する**（`stage.json` の書き間違いや、上げ忘れたファイルがテストで見つかる）

花の設定はゲーム画面で R キーを押すとすぐに反映されます。イベントは次にステージを始めたときに反映されます。
ファイルが無いステージは、**ファイルを作らずに**初期値（イベントなし）で動き、警告を出します。新しいステージを足すときは、ほかのステージのフォルダをコピーして作り、git に上げてください（足りないと `gradlew build` のテストが失敗して気づけます）。

```json
{
  "name": "STAGE 2",
  "flower": { "swayWidth": 216.0, "hitRadius": 52.0, ... },
  "events": [ ... ]
}
```

### 花の設定（`flower` の中身）

| 項目 | 意味 | 初期値 |
|---|---|---|
| `rootX` | 茎の根元の x 座標（根元は画面の下端。画面の幅は 1920） | 1560 |
| `stemLength` | 茎の長さ（px）。揺れても変わらない（画面の高さは 1080） | 540 |
| `stemWidth` | 茎の太さ（px） | 17 |
| `flowerSize` | 花の大きさ（px） | 130 |
| `swayWidth` | 揺れの大きさ。花が左右に動く幅（左端〜右端、px） | 86（プレイヤーの横幅） |
| `swayPeriod` | 揺れの1往復の秒数。0 より大きいと一定のリズムで揺れ、0 だと不規則な風で揺れる | 0（ステージ1は 4） |
| `stiffness` | 茎の硬さ（ばねの強さ）。大きいほどすぐ戻り細かく揺れる、小さいほどゆったりしなる | 25 |
| `damping` | 揺れの収まりやすさ（空気抵抗）。小さいほど長く弾む | 2.5 |
| `swaySpeed` | 不規則な風で揺れるとき（`swayPeriod` が 0）の揺れの速さ（1.0 が標準） | 1.0 |
| `hitRadius` | くちばしが重なったとみなす半径（px）。花の周りのリングの大きさも同じ | 52 |
| `nectarRate` | 1ステップ（1/60秒）で吸える蜜の量（満腹度の増え方） | 0.45 |
| `clearNectar` | この満腹度に達したらゲームクリアになり、リザルトへ進む | 100 |
| `guideDistance` | 花の周りに外側の点線リングが出始める距離（px）。状態が APPROACH になる距離も同じ | 756 |
| `texture` | 花の画像の名前（拡張子なし。例: `"petunia"`）。`images/` に置いた**正面向き**の花の画像を、真ん中を蜜の入り口に合わせて `flowerSize` の大きさで描き、茎の傾きに合わせて回す。無い・空なら図形の花 | 空（ステージ1は `petunia`、2は `anemone`、3は `trumpetvine`、4は `cornflower`、5は `golden_columbine`） |
| `textureSpin` | 花の画像の回りの上乗せ。花の真ん中を軸に、茎の傾きより多めに回る（0 なら茎と同じだけ傾き、0.4 なら茎の傾きの 1.4 倍）。茎が傾くほど多く回る、小さな風車のような動き | 0（ステージ1〜5は 0.4） |
| `textureCenterX`・`textureCenterY` | 花の画像の中で、花芯（蜜の入り口）が画像の真ん中からどれだけずれているか（画像の幅・高さに対する %。右・下がプラス）。花芯が真ん中からずれた画像でも、花芯をリングの中心に合わせて描ける | 0・0（ステージ2は -0.7・-3.5、3は 0.5・3.5、5は 2.7・-0.3） |

### ステージのイベント（`events` の中身）

イベントを1つずつ `{ }` で書いて並べます。始まったとき・終わったときはコンソールに `[Event] gust 開始` のように出ます。

```json
"events": [
  { "type": "gust", "when": { "time": 5 },    "duration": 3, "strength": 0.8 },
  { "type": "fog",  "when": { "nectar": 50 }, "duration": 6, "density": 0.6 }
]
```

**共通の項目**

| 項目 | 意味 |
|---|---|
| `type` | イベントの種類（下の表） |
| `when` | いつ始まるか。`{"time": 秒}`（開始から○秒後に1回）、`{"nectar": %}`（満腹度が○%を超えたら1回）、`{"every": 秒}`（始まりから○秒ごとにくり返す）、`{"rest": 秒}`（終わってから○秒休んでくり返す。最初も○秒後）。くり返すものは `"from": 秒` で最初の時刻を変えられる。省略するとすぐに始まる |
| `duration` | 続く秒数。省略や 0 だとステージが終わるまで続く |

**イベントの種類**

**どのステージでも使えるイベント**（`EventTypes.common()` に登録済み）

| `type` | 内容 | 項目 |
|---|---|---|
| `push` | プレイヤーへの影響。毎ステップ プレイヤーを押す | `forceX`・`forceY`：1ステップあたりの速度（px）。右・下がプラス |
| `flower` | 花の変化。花の設定を一時的に変え、終わったら元に戻す | `set`：`{"swayWidth": 160, "hitRadius": 16}` のように花の設定と同じ名前で書く |

**用意されているが、使うステージで有効にするイベント**（`StageN.registerEvents()` で `types.use("名前")`）と、**ステージ固有のイベント**

| `type` | 使っているステージ | 内容 | 項目 |
|---|---|---|---|
| `breeze` | ステージ1（`stages/stage1/BreezeEvent.java`） | そよ風。花をプレイヤーへの向かい風の向きに押す力として当たる。花の動きは茎のばね（`stiffness`・`damping`）に任せるので、押し始めは行き過ぎて戻り、風がやむと反動で揺れてから通常の揺れに戻る。吹いている間は風の向きに流れる白い「～」の線が見える | `strength`：風の強さ。吹き続けたときに花が落ち着く位置（通常の揺れ＝1。初期値 2）、`rampSeconds`：風が強まる・弱まる秒数（初期値 0.5）、`direction`：`headwind`（初期値）/`left`/`right`。**画面の白い「～」の線**：`lineLength`（長さ。初期値 130）、`waveHeight`（ふくらみ。初期値 13）、`lineCount`（本数。初期値 8）、`lineSpeed`（流れる速さ px/秒。初期値 864）、`lineWidth`（太さ。初期値 4.3） |
| `tailwind` | ステージ2（`stages/stage2/TailwindEvent.java`） | 突風のくり返し。「アイドル → 突風」をステージが終わるまでくり返す。突風の前に「突風注意!!」が出て、突風の間はハチドリが左へ流され、花は左へ大きく傾く（通常の揺れは止まる）。白い直線が右から左へ流れる | `idleTime`：突風と突風の間の秒数（初期値 7）、`gustTime`：突風の秒数（初期値 4）、`warnTime`：「突風注意!!」を出す秒数（初期値 2）、`windSpeed`：ハチドリを流す速さ（初期値 19.4）、`accelScale`・`maxSpeedScale`：突風の間のハチドリの加速・最高速度の倍率（初期値 2.0・1.5）、`tilt`・`memory`：花の傾きの大きさ（初期値 1.5・86）、`tiltIn`：花が傾ききるまでの秒数（初期値 0.35）、`windFade`：風が強まる・弱まる秒数（初期値 0.4）、`streakRate`：白い直線の出る量（初期値 45）、`curlChance`：直線の先が丸まる割合（初期値 0.15）、`streakColor`：直線の色（初期値 `#ffffff`）、`se`：突風の効果音（初期値 `tailwind`）、`warningImage`：「突風注意!!」の画像（初期値 `gust_warning`。無ければ文字で描く） |
| `birds` | ステージ3（`stages/stage3/MigratingBirdsEvent.java`） | 渡り鳥。`duration` 秒かけて鳥の群れが画面の右端から左端へ横切る。鳥が花にぶつかると花を左へ押し、ハチドリにぶつかると左下へ弾き飛ばす（ノックバック。回転・操作不能・目が×。1回横切るあいだに1回だけ） | `count`：鳥の数（初期値 7）、`birdScale`：鳥1羽の横幅（プレイヤーの横幅の何倍か。初期値 1.2）、`flockScale`：群れの広さ（同じく。初期値 4）、`followPlayer`：1 なら群れの高さを現れた瞬間のプレイヤーに合わせる（初期値 1）、`yOffset`：そのときの高さのずれ（px。初期値 0）、`minY`・`maxY`：`followPlayer` が 0 のときの高さの範囲、`texture`：鳥の画像（初期値 `kari_wataridori`）、`hitForce`：花を押す強さ（初期値 3）、`knockbackX`・`knockbackY`：弾き飛ばす距離（プレイヤーの横幅の何倍か。初期値 3・3）、`knockbackSeconds`：操作できない秒数（初期値 0.8）、`knockbackSpins`：回る回数（初期値 3）、`hitScale`・`flowerHitScale`・`playerHitScale`：鳥・花・ハチドリの当たり判定の大きさ（初期値 0.35・0.4・0.5）、`showHitbox`：1 で当たり判定の円を描く（調整用。初期値 0） |
| `eruption` | ステージ4（`stages/stage4/EruptionEvent.java`） | 火山の噴火。「休む → ため → 噴火」をステージが終わるまでくり返す（`when`・`duration` は書かない）。ためている間は山が縮んで青くなり、警告が出る。噴火中は岩が左右から落ち、火山灰で画面が暗くなる。岩がハチドリに当たると弾き飛ばし、燃えた見た目にする。山は花・プレイヤーより奥（`renderBack`）に描く。L キーで次の噴火をすぐ始められる（調整用） | `firstWait`：最初のため動作までの秒数（初期値 2）、`restSeconds`：噴火のあと次のため動作までの秒数（初期値 10）、`chargeSeconds`：ため動作の秒数（初期値 2）、`eruptSeconds`：噴火の秒数（初期値 10）、`chargedScale`：ため終わりの山の高さの倍率（初期値 0.65）、`rocksPerSide`：左右それぞれの岩の数（0 で岩なし。初期値 10）、`rockDelay`・`rockInterval`：最初の岩までの秒数・岩の間隔（初期値 1・0.5）、`rockSize`：岩の大きさ（初期値 86）、`fallSpeed`：落ちる速さ（初期値 6.5）、`rockMargin`：落ちる範囲の余白（初期値 144）、`hitRadius`：ハチドリ側の当たり判定の半径（初期値 43）、`knockback`：弾き飛ばす強さ（初期値 6）、`hurtSeconds`：燃えた見た目の秒数（初期値 0.6）、`ashOpacity`：灰の濃さ（0 で灰なし。初期値 0.85）、`ashColor`：灰の色（初期値 `#3c3d3d`）、`ashFadeIn`・`ashFadeOut`：灰が濃くなる・晴れる秒数（初期値 0.5・2）、`warningText`：警告の画像が無いときの文字（初期値 `Attention!`）、`images`・`sounds`：画像・音の場所（初期値 `""`（共通の `images/`）・`sounds/se/`）、`eruptionSound`・`fallingSound`・`hitSound`：音のファイル名（初期値 `eruption.wav`・`rock_falling.wav`・`rock_hit.wav`） |
| `gust` | まだどこも使っていない（`game/event/GustEvent.java`） | 風の変化。花が風に押されてしなる（`swayPeriod` が 0 のときだけ効く） | `strength`：-1.0（左）〜 1.0（右）。初期値 0.8 |
| `fog` | まだどこも使っていない（`game/event/FogEvent.java`） | 視界不良。画面に霧をかける（0.5秒かけて濃く・薄くなる） | `density`：濃さ 0.0〜1.0（初期値 0.7）、`color`：色（初期値 `#ffffff`） |

使うときは `StageN.java` の `registerEvents` で `types.use("fog");` と1行書きます。初期値や動きの変え方は [stages/README.md](app/src/main/resources/stages/README.md) の「イベントの使い方」を見てください。

**新しい種類のイベントを足すとき**（例: 吸蜜を妨害するエネミー）

1. `game/event/` に `StageEvent` を継承したクラスを作る。使うメソッドだけ書けばよい
   - `onStart`（始まったとき）・`onUpdate`（毎ステップ）・`onEnd`（終わったとき。変えたものを元に戻す）・`render`（描画。花・プレイヤーより手前）・`renderBack`（描画。背景のすぐ上で、花・プレイヤーより奥）
   - 風・花・プレイヤー・`GameState` などは引数の `EventContext` から触る
   - 蜜を吸えなくするには、`onUpdate` の中で `ctx.blockFeeding()` を呼ぶ（呼んだステップだけ吸えなくなる）
2. `StageEvents` の `TYPES` に `"enemy", EnemyEvent::new` のように1行足す
3. JSON に `{ "type": "enemy", ... }` と書く

きっかけ（`when`）の種類を増やすときは、`StageEvent.trigger()` に条件を1つ足します。

右上にはゲーム開始からの経過時間が秒で表示されます（例: `12.34`。小数第2位まで、最大 `9999.99`、頭に 0 は付けない）。クリアすると止まります。

## ベストタイムの記録（`records.properties`）

クリアすると、ステージごとのベストタイムが**ユーザーフォルダ**の `.humming/records.properties`（例: `C:/Users/〇〇/.humming/records.properties`）に保存されます。遊んだ人ごとの記録なので、プロジェクトの外に置いて git に入らないようにしています。ファイルを消すと記録がリセットされます。前の場所（HUMMING 直下の `records.properties`）に記録があれば、最初の1回だけ引き継ぎます。配布したゲームでは保存先が変わります（下の「配布用のフォルダを作る」を参照）。

クリアしたときに `GameState` に次の値が入るので、リザルト画面ではこれを使います。

| 項目 | 内容 |
|---|---|
| `state.stage` | ステージ番号 |
| `state.clearTime` | 今回のクリアタイム（秒） |
| `state.bestTime` | ベストタイム（今回の記録を反映したあと） |
| `state.newBest` | ベストを更新したら `true`（NEW RECORD の表示に使う。初めてのクリアも `true`） |

ステージ選択画面など、ゲーム画面の外でベストタイムを出すときは `Records` を使います。

```java
if (Records.hasBest(1)) {
    String text = "BEST " + Records.format(Records.best(1));   // 例: BEST 12.34
}
Records.format(state.clearTime);   // リザルト画面の表示用 "12.34"
```

満腹度はゲーム画面の左上にゲージで表示されます（吸蜜中は黄色）。ゲージの長さは固定で、`clearNectar` に対する割合で中身が伸びます。くちばしの先が花の入り口に触れている間だけ増えます。

### 吸蜜エフェクト（`effect_config.properties`）

吸っている間、花の入り口のまわりから蜜の粒が出て、くちばしの「花の入り口に一番近い点」に吸い付き、くちばしの上を顔へ流れて消えます（`game/flower/FeedingEffect.java`）。吸い始めには花から粒がはじけ、吸い付く点のまわりにときどき十字のきらめきが出ます。

数値は HUMMING 直下の `effect_config.properties` で変えられます（ゲーム画面で R キーを押すと読み直す）。ファイルが無いときや書き間違えた項目は、初期値で動きます。

| 項目 | 意味 | 初期値 |
|---|---|---|
| `feeding.rate` | 1秒に出る粒の数 | 25 |
| `feeding.burstCount` | 吸い始めにはじける粒の数（0 ではじけない） | 14 |
| `feeding.travelSeconds` | 花からくちばしに届く秒数 | 0.4 |
| `feeding.slideSeconds` | くちばしの上を顔まで流れる秒数 | 0.28 |
| `feeding.sizeMin`・`feeding.sizeMax` | 粒の大きさ（半径 px）の範囲 | 3.0・6.0 |
| `feeding.sparkleRatio` | 白く光る粒の割合（0.0〜1.0） | 0.25 |
| `feeding.crossPerSecond` | 十字のきらめきが1秒に出る数（0 で出ない） | 6 |
| `feeding.spawnRadius` | 粒が出る範囲の半径（花の入り口から、px） | 19 |
| `feeding.colors` | 粒の色（カンマ区切り） | `#ffd166, #ffa502` |

`GameScene` では、`update()` の吸蜜の判定（`feeding.update`）のあとに `feedingEffect.update(...)` を呼び、`render()` ではプレイヤーの前に `renderBehind`（花からくちばしへ飛んでいる粒。ハチドリより奥）、あとに `renderFront`（くちばしの上を流れる粒と、きらめき。ハチドリより手前）を呼んでいます。

### 右下のキー案内（`ui_config.properties`）

タイトル・ステージセレクトの右下に、キー操作の案内（`images/UI_Key.png`「Enter 決定 ESC 戻る」）を出します（`ui/KeyGuide.java`）。大きさと隙間は HUMMING 直下の `ui_config.properties` で変えられます（2つの画面で共通。書き換えたら、その画面を開き直すと反映される）。

| 項目 | 意味 | 初期値 |
|---|---|---|
| `keyGuide.width` | 表示する横幅（px）。高さは画像の縦横比で決まる | 470 |
| `keyGuide.marginX` | 画面の右端からの隙間（px） | 40 |
| `keyGuide.marginY` | 画面の下端からの隙間（px） | 30 |

## 配布用のフォルダを作る

Java を入れていない PC でも、ダブルクリックで遊べるフォルダ（Java 入り）を作れます。

```bash
gradlew jpackageImage
```

- できるもの: `app/build/jpackage/HUMMING/`（中の `HUMMING.exe` で起動）。このフォルダごと zip にして渡す（約 100MB）
- **Windows で実行すると Windows 用ができる**（Mac 用は Mac で実行する）
- 作る前に `app/src/main/java/org/example/core/DevMode.java` の `ENABLED` が `false` になっているか確認する（開発用のキーが配布先で効いてしまうため）
- 同梱する Java は、Gradle が使う Java 25 から作る。その Java が JavaFX を持っている版（Zulu の FX 版など）ならそれを、持っていなければ Gradle が取ってきた JavaFX を同梱する

**設定ファイルの扱い**: HUMMING 直下の `player_config.properties`・`effect_config.properties`・`ui_config.properties` は、ビルドのときに jar の `/config/` に組み込まれます。配布したゲームは HUMMING のフォルダが無いので、起動するたびに組み込みのものを `save/config/` に書き出して読みます（開発中は今までどおり HUMMING 直下のファイルを読む）。`stage.json` は組み込みのものを直接読みます。

**配布したゲームの保存先**: `HUMMING.exe` の隣の `save` フォルダに、ベストタイム（`save/records.properties`）と調整ファイル（`save/config/`）を置きます（`core/ProjectFiles.java` の `saveDir()`）。フォルダを消せば記録も消え、フォルダごと別の PC に持っていけば記録も一緒に移ります。開発中の記録（`.humming/records.properties`）は引き継がず、`save` に記録が無ければ記録なしで始まります。展開した場所に書き込めないとき（`C:/Program Files` に置いたときなど）は、ユーザーフォルダの `.humming` に保存します。

- 確かめるときは、`HUMMING.exe` をダブルクリックで起動する。HUMMING のプロジェクトの中を作業フォルダにしてコマンドで起動すると、`settings.gradle` が見つかって開発中と同じ動きになる

## 構成

```
app/src/main/java/org/example/
├── App.java              起動。Canvas を作って Game を開始する
├── core/                 ゲームの土台（共通）
│   ├── Game              ゲームループ（1/60秒の固定ステップ）と画面の切り替え
│   ├── BaseScene         画面の共通インターフェース
│   ├── Input             キー入力（isDown / isPressed）
│   ├── Entity            画面に出るものの親クラス
│   └── ProjectFiles      HUMMING 直下のファイルの場所（どこから起動しても同じ場所を指す）
├── state/                共有するデータ（共通）
│   ├── GameState         担当どうしでやり取りするデータ
│   └── Records           ステージごとのベストタイム（ユーザーフォルダに保存）
├── media/                素材（共通）
│   ├── Assets            画像・フォントの読み込み（ファイルが無くても動く）
│   └── Sound             効果音・BGM の再生と音量（ファイルが無くても動く）
├── game/                 ゲームの仕組み（共通）
│   ├── GameScene         ゲーム画面。各部品を呼ぶ順番と描く順番を決める
│   ├── flower/           Flower（花と茎）, FlowerConfig（花の設定）, Wind（風）, Feeding（吸蜜の判定）, Target（ガイドリング）,
│   │                     FeedingEffect・FeedingEffectConfig（吸蜜エフェクト）
│   ├── stage/            StageConfig（stage.json の読み込み）, StageScript（ステージごとの処理の入口）
│   ├── event/            StageEvent（親クラス）, StageEvents, EventContext, EventTypes, EventTemplate（ひな型）,
│   │                     PushEvent, FlowerChangeEvent, GustEvent, FogEvent
│   └── util/             Json（JSON の読み込み）, Easing（動きの緩急）, GameTimer（経過時間）, DebugOverlay（F3 の表示）
├── stages/stageN/        ステージごとの処理 StageN（ステージ1は Stage1 と BreezeEvent が見本）
├── player/               プレイヤー担当   Player, PlayerParams
└── ui/                   UI担当         TitleScene, StageSelectScene, StageDetailPanel, ResultScene, Hud など
app/src/main/resources/
├── stages/stageN/        ステージごとの設定（stage.json）。画像・音は images/・sounds/ に置く（作り方は stages/README.md）
├── images/  sounds/  fonts/   素材（一覧は resources/README.md）
```

## 担当

| 担当 | パッケージ | 担当者 |
|---|---|---|
| UI | `ui/` | |
| プレイヤー挙動 | `player/` | |
| それ以外 | `game/`・共通の土台 | |
