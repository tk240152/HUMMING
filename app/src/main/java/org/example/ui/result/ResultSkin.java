package org.example.ui.result;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

import org.example.media.Assets;

/**
 * リザルト画面の「見た目の設定」を1か所に集めたクラス。
 *
 * 【テクスチャの差し替え方】
 *  1. 画像を src/main/resources/images/ に「キー名.png」で置く（キーは、下の各 Slot に書いてある名前）
 *  2. ゲームを再起動する（コードの変更は不要。画像があれば自動で切り替わる）
 *  3. 位置や大きさを直したいときは、その Slot の数字を変える
 *
 * 画像が無いキーは、コードで描いた絵がそのまま使われる。
 * アニメーション（スタンプの落下、ボタンの脈打ち、ベスト更新のポップなど）は、
 * テクスチャに差し替えてもそのまま効く。
 *
 * 座標・サイズはすべて 1920x1080 のデザイン座標。画面サイズに合わせて自動で拡縮される。
 */
final class ResultSkin {

    private ResultSkin() {
    }

    /** テクスチャ1枚ぶんの置き場所。(cx, cy) が中心、w/h が大きさ。 */
    record Slot(String key, double cx, double cy, double w, double h) {
    }

    // ============================================================
    // 1. テクスチャを使うか（自動判定）
    // ============================================================
    private static final Map<String, Boolean> HAS_TEXTURE = new HashMap<>();

    /**
     * images フォルダに、そのキーの画像（{key}.png）があるか。
     * あればテクスチャを使い、無ければコードで描いた絵を使う。
     * 結果は覚えておく（画像を足したら、ゲームを再起動すると反映される）。
     */
    static boolean hasTexture(String key) {
        return HAS_TEXTURE.computeIfAbsent(key, k -> Assets.image(k) != null);
    }

    // ============================================================
    // 2. 配置（キー / 中心x / 中心y / 幅 / 高さ）
    // ============================================================
    static final Slot BACKGROUND   = new Slot("result_bg",          960,  540, 1920, 1080);
    static final Slot PANEL        = new Slot("result_panel",       960,  490, 1640,  620);
    static final Slot TITLE        = new Slot("result_title",       960,   90, 1000,  140);

    static final Slot LABEL_CLEAR  = new Slot("result_label_clear", 365,  330,  330,   92);
    static final Slot LABEL_BEST   = new Slot("result_label_best",  365,  575,  330,   92);
    static final Slot BAR_CLEAR    = new Slot("result_bar_clear",   715,  425, 1030,   24);
    static final Slot BAR_BEST     = new Slot("result_bar_best",    715,  660, 1030,   24);

    /** ランクのスタンプ。実際のキーは result_rank_S のように、ランク名が付く。 */
    static final Slot STAMP        = new Slot("result_rank",       1530,  400,  350,  350);
    static final Slot BIRD         = new Slot("hummingbird",       1350,  225,  230,  190);
    static final Slot BADGE        = new Slot("result_badge",      1530,  700,  440,   96);

    static final Slot BTN_REPLAY   = new Slot("result_btn_replay",  560,  920,  600,  116);
    static final Slot BTN_STAGE    = new Slot("result_btn_stage",  1360,  920,  600,  116);
    /**
     * 選択中のボタンを囲む赤い枠。ボタンの中心から見た、枠の左・上・右・下の位置。
     * ボタンの画像の白い四角（右下の影は含まない）に合わせてある。
     */
    static final double CURSOR_LEFT = -252, CURSOR_TOP = -57, CURSOR_RIGHT = 239, CURSOR_BOTTOM = 44;
    /** 赤い枠の、白い四角からのすき間と、線の太さ */
    static final double CURSOR_GAP = 6, CURSOR_LINE = 8;
    static final Color CURSOR_COLOR = Color.web("#e63946");

    /** 舞い落ちる花びら1枚のテクスチャ（大きさは演出側で変わる）。 */
    static final String PETAL_KEY = "result_petal";

    /**
     * リザルトの背景で、クリアしたので晴れた絵に変えるステージの画像。
     * ここで画像を返したステージは、ステージの背景の代わりに、水色の空の上にこの画像を描く。
     * 返さないステージは、クリアしたステージの背景（stage.json の background）をそのまま使う。
     * result_bg はステージ以外から来たときだけ。
     */
    static String[] stageBackgroundLayers(int stage) {
        if (stage == 4 || stage == 5) { // 富士山。噴火・嵐がおさまった、普通の色の山・森・湖（1枚の画像）
            return new String[] { "Tex_StageBG_MtFuji_Normal" };
        }
        return new String[0];
    }

    /** ランクごとのスタンプ用キー。例: result_rank_S */
    static String rankKey(String rank) {
        return STAMP.key() + "_" + rank;
    }

    // ============================================================
    // 3. 数字・文字（位置と大きさ。フォントは Assets のキー名）
    // ============================================================
    static final String NUMBER_FONT = "main";
    static final String LABEL_FONT  = "main";

    static final double TIME_NUM_X = 1230, TIME_NUM_Y = 330, TIME_NUM_SIZE = 150;
    static final double BEST_NUM_X = 1230, BEST_NUM_Y = 575, BEST_NUM_SIZE = 120;

    // ============================================================
    // 4. 色（テクスチャを使わない部分のデザイン）
    // ============================================================
    static final Color INK    = Color.web("#2b2d42");
    static final Color TEAL   = Color.web("#00b4d8");
    static final Color GREEN  = Color.web("#38b000");
    static final Color PINK   = Color.web("#ff758f");
    static final Color CENTER = Color.web("#fff3b0");

    static final Color SKY_TOP    = Color.web("#bde9f7");
    static final Color SKY_BOTTOM = Color.web("#effaf0");

    /** 花びらの紙吹雪の色。 */
    static final Color[] PETAL_COLORS = {
            PINK, Color.ORANGE, Color.ORCHID, Color.SKYBLUE, Color.PALEVIOLETRED
    };

    /** ランクごとのスタンプ（花）の色。 */
    static Color rankColor(String rank) {
        switch (rank) {
            case "SS": return Color.GOLD;
            case "S": return PINK;
            case "A": return Color.ORANGE;
            case "B": return Color.ORCHID;
            case "C": return Color.SKYBLUE;
            default:  return Color.web("#ced4da");
        }
    }
}