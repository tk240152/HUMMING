package org.example.game.stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.example.core.ProjectFiles;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.Json;
import org.example.state.GameState;

/**
 * ステージごとの設定。app/src/main/resources/stages/stage{番号}/stage.json を読み込む。
 * 中身は「名前・説明・難しさ・画像・BGM・背景・ランクの基準・花の設定（flower）・イベントの一覧（events）」。
 * 無ければファイルは作らずに初期値で動く（警告を出す）。
 * ゲーム中に R キーで読み直せる（花の設定はすぐ反映。イベントは次にステージを始めたときに反映）。
 *
 * 画像・音は共通の images/・sounds/bgm/・sounds/se/ に置き、stage.json ではファイル名だけを書く。
 */
public class StageConfig {

    /** ステージの数 */
    public static final int STAGE_COUNT = 5;

    public final int stage;
    /** 花の設定。Flower はこのオブジェクトを見続けるので、読み直しても差し替えずに値だけ変える */
    public final FlowerConfig flower = new FlowerConfig();
    /** ステージの名前（表示用） */
    public String name;
    /** 説明文（ステージ選択の詳細パネル） */
    public String description = "";
    /** 難しさ（1〜5） */
    public int difficulty = 1;
    /** 詳細パネルの画像（images/ のファイル名） */
    public String preview = "";
    /** BGM（sounds/bgm/ のファイル名） */
    public String bgm = "kari_bgm";
    /** 背景の画像（images/ のファイル名。.png・.jpg） */
    public String background = "bg_game";
    /** ランクの基準（秒）。SS・S・A・B・C の順に「この秒数より速ければそのランク」。どれより遅ければ D */
    public double[] rankTimes = GameState.DEFAULT_RANK_TIMES.clone();
    /** イベントの定義（JSON の events の中身そのまま） */
    public List<Object> events = List.of();

    private final Path path;

    private StageConfig(int stage, Path path) {
        this.stage = stage;
        this.path = path;
        this.name = "STAGE " + stage;
    }

    /** stages/stage{番号}/stage.json を読み込む。無ければ初期値で動く（ファイルは作らない）。 */
    public static StageConfig load(int stage) {
        StageConfig config = new StageConfig(stage, ProjectFiles.resolve(sourcePath(stage)));
        config.reload();
        return config;
    }

    /** ファイルを使わず、JSON の文字列から作る（テスト用）。 */
    static StageConfig fromJson(int stage, String json) {
        StageConfig config = new StageConfig(stage, null);
        config.apply(Json.parseObject(json));
        return config;
    }

    /** resources の中での、そのステージのフォルダ（例: stages/stage1）。 */
    public static String folder(int stage) {
        return "stages/stage" + stage;
    }

    /**
     * HUMMING から見た stage.json の場所（例:
     * app/src/main/resources/stages/stage1/stage.json）。
     */
    static String sourcePath(int stage) {
        return "app/src/main/resources/" + folder(stage) + "/stage.json";
    }

    /**
     * ファイルを読み直す。書き方が間違っていたら、エラーを出して今の値のまま。
     * 開発中は書き換えたファイルをすぐ読めるよう src の中を直接読み、無ければ（配布したときなど）組み込みのものを読む。
     */
    public void reload() {
        if (path == null)
            return;
        try {
            String json = read();
            if (json == null) {
                // ファイルは作らない（作ると git の管理外のファイルができて、あとで pull が止まる）
                System.err.println("[StageConfig] 警告: " + folder(stage) + "/stage.json が見つかりません。"
                        + "イベントなし・初期値の花で動きます。git pull で取得するか、ほかのステージのフォルダをコピーして作ってください: "
                        + path.toAbsolutePath());
                return;
            }
            apply(Json.parseObject(json));
            System.out.println("[StageConfig] 読み込みました: " + folder(stage) + "/stage.json");
        } catch (IOException e) {
            System.err.println("[StageConfig] 読み込みエラー: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[StageConfig] " + folder(stage) + "/stage.json の書き方が間違っています: " + e.getMessage());
        }
    }

    private String read() throws IOException {
        if (Files.exists(path))
            return Files.readString(path, StandardCharsets.UTF_8);
        try (InputStream in = StageConfig.class.getResourceAsStream("/" + folder(stage) + "/stage.json")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void apply(Map<String, Object> json) {
        name = Json.string(json, "name", name);
        description = Json.string(json, "description", description);
        difficulty = (int) Math.clamp(Json.number(json, "difficulty", difficulty), 1, 5);
        preview = Json.string(json, "preview", preview);
        bgm = Json.string(json, "bgm", bgm);
        background = Json.string(json, "background", background);
        Map<String, Object> rank = Json.object(json, "rank");
        if (rank != null) {
            // 書いていないランクは初期値のまま（SS を書いていない古い stage.json でも動く）
            for (int i = 0; i < rankTimes.length; i++)
                rankTimes[i] = Json.number(rank, GameState.RANKS[i], rankTimes[i]);
        }
        flower.apply(Json.object(json, "flower"));
        events = Json.array(json, "events");
    }

    /** BGM のキー（共通の sounds/bgm/ のファイル名）。 */
    public String bgmKey() {
        return bgm;
    }

    /** 詳細パネルの画像のキー（共通の images/ のファイル名）。 */
    public String previewKey() {
        return preview;
    }

    /** 背景の画像のキー（共通の images/ のファイル名）。 */
    public String backgroundKey() {
        return background;
    }

    /** 初期値の JSON（説明付き）。新しいステージのファイルを作るときのひな型（テストでも使う）。 */
    String defaultJson() {
        String flowerJson = flower.toJson().strip().replace("\n", "\n  ");
        return """
                {
                  "_使い方": "このステージの設定です。数値を書き換えて保存し、ゲーム画面で R キーを押すと花の設定がすぐに反映されます（イベントは次にステージを始めたときに反映）。_ で始まる項目は説明なので消しても動きます。",

                  "_name": "ステージの名前。ステージ選択の詳細パネルと、ゲーム画面の右上に出る。",
                  "name": "%s",

                  "_description": "ステージ選択の詳細パネルに出る説明文。",
                  "description": "%s",

                  "_difficulty": "難しさ（1〜5）。ステージ選択で★の数になる。",
                  "difficulty": %s,

                  "_preview": "ステージ選択の詳細パネルに出る画像のファイル名（拡張子なし）。共通の images/ に置く。",
                  "preview": "%s",

                  "_bgm": "BGM のファイル名（拡張子なし）。共通の sounds/bgm/ に置く。",
                  "bgm": "%s",

                  "_background": "背景の画像のファイル名（拡張子なし）。共通の images/ に置く。",
                  "background": "%s",

                  "_rank": "ランクの基準（秒）。この秒数より速くクリアするとそのランク。C より遅いと D。",
                  "rank": { "SS": %s, "S": %s, "A": %s, "B": %s, "C": %s },

                  "_flower": "花の設定。",
                  "flower": %s,

                  "_events": "このステージで起きるイベントの一覧。書き方は README の「ステージのイベント」を参照。",
                  "events": []
                }
                """
                .formatted(name, description, difficulty, preview, bgm, background,
                        rankTimes[0], rankTimes[1], rankTimes[2], rankTimes[3], rankTimes[4], flowerJson);
    }
}
