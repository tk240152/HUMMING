# チーム開発のルール

## 1. 担当とファイルの持ち主

| 担当 | 触ってよい場所 |
|---|---|
| **ステージの担当** | `resources/stages/stageN/stage.json`、そのステージ用の素材（`resources/images/`・`resources/sounds/` の中の自分のステージの分）と `java/org/example/stages/stageN/`。**誰がどのステージを作ってもよい**ので、作業を始める前にチームで「今どのステージを触っているか」を共有する |
| UI | `ui/` |
| プレイヤー挙動 | `player/` |
| 共通の仕組み | `game/`（`flower/` 花・風・判定、`stage/` ステージの読み込み、`event/` イベント、`util/` 道具） |
| 全員（共通の土台） | `App`、`core/`（Game・BaseScene・Input・Entity・ProjectFiles）、`state/`（GameState・Records）、`media/`（Assets・Sound）、`build.gradle`、`.gitignore` |

- **ステージの作業は、そのステージのフォルダの中だけで完結させる。** 作り方は [stages/README.md](app/src/main/resources/stages/README.md)。
- 同じステージを2人が同時に触らない。共通の仕組みや共通のイベントを変えたくなったら、ステージのフォルダで試してから3人で相談する。

- **自分の担当以外のファイルは原則として触らない。** 触る必要があるときは、そのファイルの担当者に PR をレビューしてもらう。
- **共通の土台**を変更する PR は、3人全員のレビューが必要。
- 調整値（速度・色など）は自分のクラスの定数に置く。共通の定数ファイルは作らない。

## 2. 担当どうしの約束

```
Input ─▶ Player ─▶ GameState ◀─ game/（花・風・判定）
                       │ 読むだけ
                       ▼
                     ui/Hud
```

- 担当どうしのやり取りは **`GameState` と `Player` の public メソッドだけ**で行う。
- `ui/` は `GameState` を**読むだけ**で、値を書き換えない。
- `Player` のメソッド名と引数（`update` `addForce` `getBeakTip` `writeTo`）は勝手に変えない。変えるときは全員で相談する。
- **単位**: 位置は px、速度・力は「1ステップ（1/60秒）あたりの px」、角度はラジアン。

## 3. ブランチ

```
main ← develop ← feature/HUMMING-<担当>-<内容>
```

例: `feature/HUMMING-ui-hud`、`feature/HUMMING-player-move`、`feature/HUMMING-game-flower`

- `develop` から作業ブランチを作り、`develop` へ PR を出す。
- `main` へのマージはチームでそろって確認してから行う。

## 4. コミットメッセージ

先頭に種類を付ける。

| 種類 | 使うとき |
|---|---|
| `[追加]` | 新しい機能・ファイル |
| `[修正]` | バグの修正 |
| `[変更]` | 仕様・見た目の変更 |
| `[整理]` | 動きを変えないコードの整理 |
| `[素材]` | 画像・音・フォントの追加 |

例: `[追加] プレイヤーの慣性移動`

## 5. PR

1つの PR には1つの機能だけを入れる。PR の本文に次の内容を書く。

```
## 概要
## 変更したファイル
## 動作確認
- [ ] `gradlew build` が通る（テストで、上げ忘れたファイルが無いかも確認される。VS Code の Run ボタンではテストは動かない）
- [ ] VS Code の Run（App.java）でタイトル → ステージ選択 → ゲーム → リザルト と P キーで一周できる
- [ ] 自分の担当以外のファイルを変更していない（変更した場合は理由を書く）
## スクリーンショット
```

- レビューで1人以上が承認してからマージする。

## 6. 機能の追加のしかた

### 画面を追加する
`BaseScene` を実装したクラスを作り、前の画面の `next()` で返すようにする。

```java
public class HowToScene implements BaseScene {
    public void update() {}
    public void render(GraphicsContext gc) { ... }
    public BaseScene next() { return new GameScene(); }
}
```

コードの中から画面を切り替えたいときは `Game.change(new 〇〇Scene())` を使う。

### 画面に出るもの（キャラクター・花など）を追加する
`Entity` を継承し、`update()` を書く。`render()` を書かなければ `textureKey` の画像が自動で描かれる。

```java
public class Flower extends Entity {
    public Flower() { super("flower", 60, 40); }
    @Override public void update() { ... }
}
```

追加したら `GameScene` の `update()` と `render()` から呼ぶ。

### 画像・音を追加する
`resources/images/{キー名}.png` などを置くだけ。`resources/README.md` の一覧に書き足す。

### 音を鳴らす
```java
Sound.se("feed");       // 効果音   resources/sounds/se/feed.wav
Sound.se("feed", 0.5);  // この1回だけ音量を半分にする
Sound.bgm("game");      // BGM（ループ） resources/sounds/bgm/game.mp3
Sound.stopBgm();
```
- 画面ごとの BGM は、その画面の `onEnter()` で `Sound.bgm(...)` を呼ぶ。同じキーなら途切れずに鳴り続ける。
- 音量は 0.0〜1.0。`Sound.setMasterVolume` / `setBgmVolume` / `setSeVolume` / `setMuted` で変えられる（オプション画面などから呼ぶ）。
- 実際の音量 = 全体の音量 × 種類ごとの音量。音量は保存されず、起動するたびに初期値に戻る。

### キー入力を使う
- 押している間: `Input.isDown(KeyCode.W)`
- 押した瞬間: `Input.isPressed(KeyCode.SPACE)`
- P は開発用の画面送りに使っている（どの画面でも次の画面へ進む）。P をゲームの操作に使いたくなったら `Game.enterToNext = false` にする。Enter は各画面で決定などに使ってよい。

## 7. コードの書き方

- 名前: クラスは `PascalCase`、メソッド・変数は `camelCase`、定数は `UPPER_SNAKE_CASE`、画面は `〇〇Scene`。
- 書式は `.editorconfig` に従う（スペース4つ、UTF-8）。
- `update()` には処理、`render()` には描画だけを書く。`render()` の中で値を変えない。

## 8. JavaScript のプロトタイプから移すときの注意

- `gc.rotate()` は**度**で指定する。ラジアンは `Math.toDegrees()` で変換する（`Entity` は変換済み）。
- 回転した楕円は `gc.save()` → `translate` → `rotate` → `fillOval` → `gc.restore()` で描く。
- `Courier New` には日本語の文字が無い。日本語を表示するときは日本語に対応したフォントを `fonts/` に置く。
- CSS のグラデーションは `LinearGradient` で描く。
- キーは `e.code`（`KeyW`）ではなく `KeyCode.W` で判定する。
- プロトタイプは1フレームあたりの値で動いているので、調整値はそのまま使える（`Game` は1秒に60回 update する）。

## 9. ファイルの置き場所（「自分の PC でだけ動く」を防ぐ）

- **プロジェクトの中のファイルを直接読むとき**（`stage.json`・`player_config.properties` など）は、`ProjectFiles.resolve("HUMMING からの場所")` で場所を決める。
  `new File("…")` や `Path.of("…")` で直接書くと、起動した場所によって別のファイルを読んでしまう。
- **ゲームに同梱するファイル**（画像・CSS・ステージ情報など）は `app/src/main/resources/` に置き、`getResource("/…")` で読む。
- **`data`・`bin` という名前のフォルダは使わない。** リポジトリ全体の `.gitignore` で無視され、git に上がらない。
- **`git add -f` は使わない。** 無視されているファイルは、遊んだ人ごとに中身が変わるもの（記録など）なので、共有してはいけない。
- 新しいステージを足すときは `stages/README.md` の手順で、フォルダ・`stage.json`・`StageN.java` をそろえて git に上げる
  （`RequiredFilesTest` が足りないファイルを見つけて、ビルドを失敗させる）。
- 新しく必要なファイルを増やしたら、`RequiredFilesTest` の一覧にも足す。
- pull したあとにおかしいときは、まず VS Code で「Java: Clean Java Language Server Workspace」を実行して再起動する。直らなければ `gradlew clean build` でエラーを確認する。
- VS Code では **HUMMING フォルダを開いて** App.java の Run で起動する（チームの標準）。
