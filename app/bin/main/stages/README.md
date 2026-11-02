# ステージの作り方

1つのステージに関するものは、すべてそのステージのフォルダにまとめます。
**担当者は、原則として自分のステージのフォルダだけを触ります**（ほかの人とファイルがぶつからない）。

```
app/src/main/resources/stages/stageN/
├── stage.json   設定（名前・BGM・背景・ランクの基準・花・イベント）
├── images/      このステージだけの画像（bg.png など）
└── sounds/      このステージだけの音（BGM・効果音）

app/src/main/java/org/example/stages/stageN/
└── StageN.java  このステージだけの特別な処理（設定だけで作れない動き）。部品もこのフォルダに作る
```

## stage.json に書くこと

| 項目 | 内容 |
|---|---|
| `name` | ステージの名前（ステージ選択の詳細パネルと、ゲーム画面の右上に出る） |
| `description` | ステージ選択の詳細パネルに出る説明文 |
| `difficulty` | 難しさ（1〜5）。ステージ選択の★の数 |
| `preview` | ステージ選択の詳細パネルに出る画像（`images/` に置けばそれ、無ければ共通の `images/`。無いと「準備中」） |
| `bgm` | BGM のファイル名（拡張子なし）。`sounds/` に置けばそれを、無ければ共通の `sounds/bgm/` を使う |
| `background` | 背景の画像のファイル名（拡張子なし）。`images/` に置けばそれを、無ければ共通の `images/` を使う |
| `rank` | ランクの基準（秒）。`{ "SS": 30, "S": 40, "A": 60, "B": 80, "C": 100 }`（この秒数より速ければそのランク。C より遅いと D。書いていないランクは初期値） |
| `flower` | 花の設定（項目は HUMMING の README「花の設定」） |
| `events` | イベントの一覧（書き方は HUMMING の README「ステージのイベント」） |

ゲーム中に R キーを押すと読み直します（花の設定・ランクの基準はすぐ、イベントは次にステージを始めたとき）。

## イベントの使い方

`game/event/` に用意されているイベントは、**コピーしなくても呼び出せて、必要な分だけカスタマイズ**できます。
簡単な順に5つのやり方があります。

| やり方 | 書く場所 | 例 |
|---|---|---|
| ① そのまま使う | `stage.json` | `push`（プレイヤーを押す）・`flower`（花の設定を変える）は登録なしで使える |
| ② 用意されたイベントを有効にする | `StageN.registerEvents` | `types.use("fog", "gust");` → `stage.json` で `"type": "fog"` と書ける |
| ③ 初期値を変えて別の名前で使う（コードを書かずに） | `StageN.registerEvents` | `types.register("thickFog", EventTypes.preset("fog", Map.of("density", 0.95, "color", "#888888")));`（`stage.json` に書いた値のほうが優先） |
| ④ 動きを少し変える | `StageN.registerEvents` | 用意されたイベントを継承して、`protected` のメソッドだけ上書きする（下の例） |
| ⑤ 大きく変える・新しく作る | 自分のステージのフォルダ | `game/event/EventTemplate.java`（ひな型）か、近いイベントをコピーして作る |

④ の例（色だけ変えた霧）:

```java
public void registerEvents(EventTypes types) {
    types.register("pinkFog", p -> new FogEvent(p) {
        @Override
        protected Color color(EventContext ctx) {
            return Color.PINK;
        }
    });
}
```

**上書きできるメソッド**

| イベント | 上書きできるもの |
|---|---|
| `fog`（`FogEvent`） | `color(ctx)`（色）・`alpha(ctx)`（濃さ）・`drawFog(gc, ctx, color)`（霧の形。プレイヤーの周りだけ晴れるなど） |
| `gust`（`GustEvent`） | `strength(ctx)`（強さ。時間で変えるなど） |
| `push`（`PushEvent`） | `forceX(ctx)`・`forceY(ctx)`（押す力） |
| `flower`（`FlowerChangeEvent`） | `onStart(ctx)`・`onEnd(ctx)`（`super` を呼んでから処理を足す） |
| すべて | `onStart`・`onUpdate`・`onEnd`・`render`（花・プレイヤーより手前に描く）・`renderBack`（背景のすぐ上、花・プレイヤーより奥に描く）（`StageEvent` の基本のメソッド） |

`game/event/` のクラスそのものは書き換えないでください（それを使っているほかのステージに影響します）。

## StageN.java でできること

`StageScript` の必要なメソッドだけを書きます。風・花・プレイヤー・`GameState` は引数の `EventContext` から触ります。

| メソッド | 呼ばれるとき |
|---|---|
| `registerEvents(types)` | このステージだけのイベントを登録する（`types.register("enemy", EnemyEvent::new)`）。stage.json の `type` で使えるようになる |
| `onStart(ctx)` | ステージが始まったとき |
| `update(ctx)` | 毎ステップ（1/60秒） |
| `renderBack(gc, ctx)` | 背景のすぐ上に描く |
| `renderFront(gc, ctx)` | 花・プレイヤーより上（UI より下）に描く |
| `onExit(ctx)` | ステージを抜けるとき（変えたものを元に戻す） |

蜜を吸えなくする（エネミーなど）ときは、`update` かイベントの中で `ctx.blockFeeding()` を呼びます。

## 素材（画像・音）の置き場所

| 素材 | 置く場所 |
|---|---|
| そのステージだけで使う画像 | `app/src/main/resources/stages/stageN/images/` |
| そのステージだけで使う音（BGM・効果音） | `app/src/main/resources/stages/stageN/sounds/` |
| 全ステージで使う画像・音・フォント | `resources/images/`・`resources/sounds/bgm/`・`resources/sounds/se/`・`resources/fonts/`（置く前に全体の担当に相談） |

- **`java` のフォルダ（`java/org/example/stages/stageN/`）には置かない。** `java` の下に置いた画像や音はビルドに含まれず、ゲームから読めない
- ファイル名は英小文字と `_` だけ（例: `kari_wataridori.png`）。大文字が混ざると、配布したときに読めなくなることがある
- 形式: 画像は `.png`（背景を透明にしたいものは透明の PNG）、BGM は `.mp3` か `.wav`、効果音は `.wav` か `.mp3`
- `data`・`bin` という名前のフォルダは使わない（リポジトリ全体の `.gitignore` で無視され、git に上がらない）

**使い方**

- `stage.json` の `bgm`・`background`・`preview` には**名前だけ**を書く。このステージのフォルダを先に探し、無ければ共通のフォルダから探す
- イベントの `se`（効果音）は名前だけだと共通の `sounds/se/` しか探さない。このステージの `sounds/` に置いた音は場所ごと書く（例: `"se": "stages/stage5/sounds/thunder"`）
- 背景・BGM・効果音の置き方の一覧は [resources/README.md](../README.md) を参照
- コード（`StageN.java`・イベント）では**場所を含めて**書く

```java
Assets.image("stages/stage3/images/kari_wataridori");   // 画像
Sound.se("stages/stage3/sounds/hit");                    // 効果音
Sound.bgm("stages/stage3/sounds/stage3_bgm");            // BGM
```

## ルール

- 自分のステージのフォルダ以外は触らない。共通の仕組み（`game/`・`game/event/`・共通の土台）を変えたいときは3人で相談する
- **共通のイベントや、ほかのステージのイベント（`FlowerChangeEvent`、ステージ1の `BreezeEvent` など）の動きを変えたくなったら、元のクラスは直さない。** 少しなら上の「イベントの使い方」の ③④、大きく変えるなら自分のステージのフォルダにコピーして、別の名前で登録する（元のクラスを直すと、そのクラスを使っているほかのステージに影響するため）
  ```java
  // 1. stages/stage1/BreezeEvent.java を stages/stage3/StrongBreezeEvent.java にコピーして、クラス名と package を変えて直す
  // 2. Stage3.java で登録する
  public void registerEvents(EventTypes types) {
      types.register("strongBreeze", StrongBreezeEvent::new);
  }
  // 3. stage.json の events で "type": "strongBreeze" と書く
  ```
- **PR の前に `gradlew build` を実行する。** `stage.json` の書き間違い（JSON の形・足りない項目）や、git に上げ忘れたファイルがテストで見つかる（VS Code の Run ボタンではテストは動かない）
- 全ステージで使えそうなイベントができたら、共通のイベント（`EventTypes.common()`）へ移すことを相談する
- 素材は上の「素材（画像・音）の置き場所」のとおりに置く（`java` のフォルダには置かない）
- 新しいステージを足すときは、ほかのステージのフォルダと `StageN.java` をコピーして番号を変える（`StageConfig.STAGE_COUNT` も更新）
