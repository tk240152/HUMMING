# 素材フォルダ

ファイルを置くだけで反映されます（コードの変更は不要）。置いたあとは**ゲームを起動し直してください**（起動中に置いても反映されません）。
ファイルが無いあいだは、画像は「色付きの四角＋キー名」（リザルトなどはコードで描いた絵）、音は無音、フォントは標準で動きます。

| フォルダ | 形式 | ファイル名 |
|---|---|---|
| `images/` | `.png` | `{キー名}.png` |
| `sounds/se/`  | `.wav`（または `.mp3`） | `{キー名}.wav`（効果音） |
| `sounds/bgm/` | `.mp3`（または `.wav`） | `{キー名}.mp3`（BGM。ループ再生される） |
| `fonts/`  | `.ttf` | `{キー名}.ttf` |
| `stages/stageN/images/`・`stages/stageN/sounds/` | 上と同じ | そのステージだけで使う画像・音（[stages/README.md](stages/README.md)） |

- 新しく作るファイルの名前は英小文字と `_` だけを使います（例: `bg_game.png`）。大文字・空白が混ざると、配布したときや名前の打ち間違いで読めなくなることがあります
- 名前は**キー名と1文字も違わない**ようにします（`result_rank_S .png` のように空白が入ると読めません。実際に S ランクの画像が出ていなかったことがある）
- `java` のフォルダの下には置かない（ビルドに含まれず、ゲームから読めない）

## 背景の置き方

背景の画像は **1920 x 1080**（横:縦 = 16:9）で作ります。画面いっぱいに引き伸ばして描くので、比率が違うとつぶれて見えます。

| 画面 | 置くファイル | 備考 |
|---|---|---|
| ゲーム画面（全ステージ共通） | `images/bg_game.png` | |
| ゲーム画面（そのステージだけ） | `stages/stageN/images/bg_game.png` | 共通のものより優先される |
| ゲーム画面（名前を変えたいとき） | stage.json の `"background"` に名前を書く | 例: `"background": "bg_forest"` → `stages/stageN/images/bg_forest.png`、無ければ `images/bg_forest.png` |
| ゲーム画面（デザインのフォルダをそのまま使う） | stage.json の `"background"` に `resources` からの場所を書く | 例: `"background": "stage_Game_Tex/Tex_Game_Stage01_BG"`（`.png` は書かない） |
| タイトル | `stage_TitleScreen_Tex/Tex_Title_BG.png` | 同じ名前で上書きする |
| ステージ選択 | `stage_StageSelect_Tex/Tex_StageSelect_BG.png` | 同じ名前で上書きする |
| ステージ選択の詳細パネル | `stage_Select_Win_Tex/Tex_SelectWin_BG.png` | 同じ名前で上書きする |
| リザルト | `images/result_bg.png` | 無ければコードで描いた空 |
| ポーズメニューのパネル | `images/UI_Pause_BG.png` | 1080x1080 の中の絵のある所だけを切り出して描く（範囲は `PauseMenu` の `PANEL_SRC`） |

## BGM の置き方

| 画面 | 置くファイル | 備考 |
|---|---|---|
| ゲーム画面（全ステージ共通） | `sounds/bgm/{名前}.mp3` | stage.json の `"bgm"` に名前を書く（今は全ステージ `kari_bgm`） |
| ゲーム画面（そのステージだけ） | `stages/stageN/sounds/{名前}.mp3` | 同じく `"bgm"` に名前を書く。共通のものより優先される |
| タイトル | `sounds/bgm/title.mp3` | |
| ステージ選択 | `sounds/bgm/stage_select.mp3` | 無ければ無音（タイトルの曲は続かない） |
| リザルト | `sounds/bgm/result.mp3` | |

- BGM はすべてくり返し再生されます。曲の最後と最初がつながるように作ると、継ぎ目が目立ちません
- 画面が切り替わると、その画面の曲に切り替わります。ファイルが無い画面は無音です
- ゲーム画面ではポーズで一時停止し、再開で続きから鳴ります
- 新しい画面を作って BGM を付けるときは、その画面のクラスに `bgm()` を書きます（`return "名前";`。書かなければ前の画面の曲が続く）

## 効果音の置き方

`sounds/se/{キー名}.wav`（または `.mp3`）に置きます。今のコードで鳴らしているキー名は次のとおりです。

| キー名 | 鳴るとき | 担当 |
|---|---|---|
| `menu_move` | カーソル移動（タイトル・ステージ選択・ポーズ・リザルト）、オプションで音量を変えている間 | UI |
| `menu_decide` | 決定（タイトル・ステージ選択で詳細を開く／スタート・ポーズ・リザルト） | UI |
| `menu_cancel` | 戻る（ステージ選択で Esc：詳細を閉じる／タイトルへ戻る） | UI |
| `ready` | ゲーム開始の「Ready...」が出たとき | それ以外 |
| `go` | 「Go!」に変わったとき | それ以外 |
| `feed` | 蜜を吸い始めたとき | それ以外 |
| `clear` | 蜜が満タンになったとき | それ以外 |
| `debug` | 確認用（ゲーム画面で O キー、デバッグ表示中に効果音の音量を変えたとき） | それ以外 |
| `tailwind` | ステージ2 の突風（stage.json の `"se"` で変えられる） | ステージ2 |
| `thunder` | ステージ5 の雷（stage.json の `"se"` で変えられる） | ステージ5 |

- stage.json の `"se"` に名前だけを書くと、共通の `sounds/se/` から探します。そのステージのフォルダに置くときは場所ごと書きます（例: `"se": "stages/stage5/sounds/thunder"`）
- ステージ4 の噴火の音は別の決まりです: `stages/stage4/sounds/` に `eruption.wav`・`rock_falling.wav`（落石中ループ）・`rock_hit.wav`（名前は stage.json の `eruptionSound` などで変えられる）

## 音ごとの音量をそろえる（`sounds/volumes.properties`）

フリー音源などで音の大きさがばらばらなときは、`sounds/volumes.properties` に「キー名=倍率」を書いて、大きすぎる音を下げます。

```properties
feed=0.7
kari_bgm=0.8
stages/stage5/sounds/thunder=0.5
```

- 1.0 = そのまま、0.5 = 半分、0 = 鳴らさない。書いていない音は 1.0
- **1.0 より大きくはできない**（ファイルの元の大きさが上限）。小さすぎる音は、Audacity などの「ノーマライズ」で大きくしてから置く
- キー名は上の表の名前（stage.json の `bgm` に書いた名前も同じ）。ステージのフォルダの音は場所ごと書く
- ステージ4 の噴火の音（`EruptionAudio`）にはこの表は効かない（`EruptionAudio` の中の数字で調整する）
- 書き換えたら、ゲームを起動し直すと反映される

## 音のファイルの注意

- 効果音の `.wav` は **16bit の PCM** で書き出す（確かめた形式）。24bit・32bit（浮動小数点）は鳴らないことがある
- 効果音は短く、頭に無音を入れない（押した瞬間に鳴らないように聞こえる）
- 全体の大きさはゲームの音量設定（オプション、F3 のデバッグ表示中の 1〜6 キー）で決まる。音どうしの差は上の `volumes.properties` でそろえる
- 鳴らないときは、ターミナルに `[Sound] 読み込めませんでした` が出ていないか見る（形式が合っていないとき）。何も出ないときは、名前か置き場所が違う

## 必要な素材の一覧

素材を追加したら、この表にも書き足してください。

| キー名 | 種類 | サイズ (px) | 使う場所 | 担当 | 状態 |
|---|---|---|---|---|---|
| `hummingbird_body`・`hummingbird_wing` | 画像 | 正方形 | Player・タイトル | プレイヤー | 済 |
| `bg_game`   | 画像 | 1920 x 1080 | GameScene（全ステージ共通の背景） | それ以外 | 未 |
| `result_bg` | 画像 | 1920 x 1080 | ResultScene | UI | 済 |
| `UI_Pause_BG`・`UI_Pause_Back`・`UI_Pause_Option`・`UI_Pause_Retry`・`UI_Pause_Retire` | 画像 | 1080 x 1080 | PauseMenu（パネルとボタン） | UI | 済 |
| `main`      | フォント | - | ゲーム画面・ポーズ・オプション・リザルト | UI | 未 |
| `title`     | BGM | - | TitleScene | UI | 未 |
| `stage_select` | BGM | - | StageSelectScene | UI | 未 |
| `kari_bgm`  | BGM | - | GameScene（仮のBGM。全ステージ） | それ以外 | 済 |
| `result`    | BGM | - | ResultScene | UI | 未 |
| `ready`・`go` | 効果音 | - | ゲーム開始の Ready / Go | それ以外 | 未 |
| `feed`      | 効果音 | - | 蜜を吸い始めたとき | それ以外 | 未 |
| `clear`     | 効果音 | - | 蜜が満タンになったとき | それ以外 | 未 |
| `menu_move` | 効果音 | - | メニューのカーソル移動・音量の変更 | UI | 未 |
| `menu_decide` | 効果音 | - | メニューの決定 | UI | 未 |
| `menu_cancel` | 効果音 | - | メニューで戻る | UI | 未 |
| `debug`     | 効果音 | - | 確認用の音 | それ以外 | 未 |
| `tailwind`  | 効果音 | - | ステージ2 の突風 | ステージ2 | 未 |
| `thunder`   | 効果音 | - | ステージ5 の雷 | ステージ5 | 未 |

リザルト画面のほかのテクスチャ（`result_title`・`result_rank_S` など）のキー名は `ui/result/ResultSkin.java` に書いてあります。
