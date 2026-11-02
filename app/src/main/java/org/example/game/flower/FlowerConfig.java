package org.example.game.flower;

import java.util.Map;
import org.example.game.util.Json;

/**
 * 花の調整値。ステージごとの設定ファイル（stages/stageN/stage.json）の "flower" の中身を読み込む。
 * 読み込み・保存は StageConfig が行い、このクラスは値を持つだけ。
 *
 * 単位: 長さは px、swaySpeed は倍率（1.0 が標準）、nectarRate は 1ステップあたりの蜜の量。
 */
public class FlowerConfig {

    /** 茎の根元の x 座標（根元は画面の下端） */
    public double rootX = 1560;
    /** 茎の長さ */
    public double stemLength = 540;
    /** 茎の太さ */
    public double stemWidth = 17;
    /** 花の大きさ（花びらの長さ） */
    public double flowerSize = 130;
    /** 揺れの大きさ。花の先が左右に動く幅（左端〜右端） */
    public double swayWidth = 86;
    /** 揺れの速さ */
    public double swaySpeed = 1.0;
    /** 1往復の秒数。0 より大きいと一定のリズムで揺れる。0 なら不規則な風で揺れる */
    public double swayPeriod = 0;
    /** 茎の硬さ（ばねの強さ）。大きいほど速く元に戻り、細かく揺れる */
    public double stiffness = 25;
    /** 揺れの収まりやすさ（空気抵抗）。小さいほど長く弾む */
    public double damping = 2.5;
    /** くちばしが重なったとみなす半径 */
    public double hitRadius = 52;
    /** 1ステップで吸える蜜の量（満腹度の増え方） */
    public double nectarRate = 0.45;
    /** この満腹度に達したらゲームクリア */
    public double clearNectar = 100;
    /** この距離より近づくと、花の周りに外側のガイドリングが出る（状態も APPROACH になる） */
    public double guideDistance = 756;
    /** 花の画像の回りの上乗せ。0 なら茎と同じだけ傾き、0.4 なら茎の傾きの 1.4 倍まで花が回る（花の真ん中が軸） */
    public double textureSpin = 0;
    /**
     * 花の画像の中で、花芯（蜜の入り口）が画像の真ん中からどれだけずれているか（画像の幅に対する %。右・下がプラス）。
     * 花芯が真ん中からずれた画像でも、花芯をリング（当たり判定）の中心に合わせて描ける
     */
    public double textureCenterX = 0;
    public double textureCenterY = 0;
    /**
     * 花の画像（正面から見た花）。stage.json には名前だけ書き（例: "petunia"）、
     * 画像は共通の images/ に置く。
     * 画像の真ん中を花の入り口に合わせ、茎の傾きに合わせて回して描く。空なら今までの花（共通の flower 画像か図形）。
     */
    public String texture = "";

    /** JSON の文字列を反映する。書き方が間違っていたら、エラーを出して今の値のまま。 */
    public void apply(String json) {
        try {
            apply(Json.parseObject(json));
        } catch (IllegalArgumentException e) {
            System.err.println("[FlowerConfig] JSON の書き方が間違っています: " + e.getMessage());
        }
    }

    /**
     * 読み込んだ JSON の項目を反映する（ステージごとの上書きにも使う）。
     * "_" で始まる項目は説明なので無視する。知らない項目は警告を出して無視する。
     */
    public void apply(Map<String, Object> values) {
        if (values == null) return;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            if (e.getKey().equals("texture")) {
                if (e.getValue() instanceof String s) texture = s.strip();
                else System.err.println("[FlowerConfig] texture は画像の名前（文字）で書いてください");
            } else if (e.getValue() instanceof Double v) {
                if (!set(e.getKey(), v)) System.err.println("[FlowerConfig] 知らない項目です: " + e.getKey());
            } else {
                System.err.println("[FlowerConfig] 数値ではありません: " + e.getKey());
            }
        }
    }

    /** 名前で値を変える（イベントで一時的に変えるときにも使う）。知らない名前なら false。 */
    public boolean set(String name, double v) {
        switch (name) {
            case "rootX" -> rootX = v;
            case "stemLength" -> stemLength = Math.max(1, v);
            case "stemWidth" -> stemWidth = Math.max(0, v);
            case "flowerSize" -> flowerSize = Math.max(0, v);
            case "swayWidth" -> swayWidth = Math.max(0, v);
            case "swaySpeed" -> swaySpeed = Math.max(0, v);
            case "swayPeriod" -> swayPeriod = Math.max(0, v);
            case "stiffness" -> stiffness = Math.max(0.1, v);
            case "damping" -> damping = Math.max(0, v);
            case "hitRadius" -> hitRadius = Math.max(0, v);
            case "nectarRate" -> nectarRate = Math.max(0, v);
            case "clearNectar" -> clearNectar = Math.max(1, v);
            case "guideDistance" -> guideDistance = Math.max(0, v);
            case "textureSpin" -> textureSpin = Math.max(0, v);
            case "textureCenterX" -> textureCenterX = Math.clamp(v, -50, 50);
            case "textureCenterY" -> textureCenterY = Math.clamp(v, -50, 50);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** 名前で値を読む。知らない名前なら NaN。 */
    public double get(String name) {
        return switch (name) {
            case "rootX" -> rootX;
            case "stemLength" -> stemLength;
            case "stemWidth" -> stemWidth;
            case "flowerSize" -> flowerSize;
            case "swayWidth" -> swayWidth;
            case "swaySpeed" -> swaySpeed;
            case "swayPeriod" -> swayPeriod;
            case "stiffness" -> stiffness;
            case "damping" -> damping;
            case "hitRadius" -> hitRadius;
            case "nectarRate" -> nectarRate;
            case "clearNectar" -> clearNectar;
            case "guideDistance" -> guideDistance;
            case "textureSpin" -> textureSpin;
            case "textureCenterX" -> textureCenterX;
            case "textureCenterY" -> textureCenterY;
            default -> Double.NaN;
        };
    }

    /**
     * 説明付きの JSON。"_" で始まる項目は説明用で、読み込むときは無視される
     * （JSON にはコメントを書けないため）。
     */
    public String toJson() {
        return """
                {
                  "_rootX": "茎の根元の横の位置（px）。根元は画面の下端に固定。0 が左端、1920 が右端。大きくすると花が右へ寄る。",
                  "rootX": %s,

                  "_stemLength": "茎の長さ（px）。揺れても長さは変わらない。花の高さ ＝ 1080 − この値（540 なら画面の真ん中の高さ）。970 を超えると花が画面の上にはみ出る。",
                  "stemLength": %s,

                  "_stemWidth": "茎の太さ（px）。見た目だけで、当たり判定には関係しない。目安は 9〜26。",
                  "stemWidth": %s,

                  "_flowerSize": "花の大きさ（px）。花びらの長さ。見た目だけなので、狙いやすさは hitRadius で変える。目安は 86〜216。",
                  "flowerSize": %s,

                  "_swayWidth": "揺れの大きさ（px）。花が左右に動く幅（左端から右端まで）。プレイヤーの横幅は 86。0 にすると揺れない。大きいほど難しい。",
                  "swayWidth": %s,

                  "_swayPeriod": "揺れの1往復の秒数。0 より大きいと、この秒数で一定のリズムで左右に揺れる。0 にすると、不規則な風で揺れる（そのときの速さは swaySpeed）。",
                  "swayPeriod": %s,

                  "_stiffness": "茎の硬さ（ばねの強さ）。大きいほど押されてもすぐ戻り、細かく揺れる。小さいほどゆったりしなる。目安は 10〜60。",
                  "stiffness": %s,

                  "_damping": "揺れの収まりやすさ（空気抵抗）。小さいほど押されたあと長くびよんびよん弾み、大きいほどすぐ落ち着く。目安は 1〜8。",
                  "damping": %s,

                  "_swaySpeed": "不規則な風で揺れるとき（swayPeriod が 0）の揺れの速さ。1.0 が標準。2.0 で2倍の速さ、0.5 で半分。大きいほど難しい。",
                  "swaySpeed": %s,

                  "_hitRadius": "当たり判定の半径（px）。くちばしの先が花の入り口からこの距離以内なら蜜を吸える。花の周りのリング（緑になる円）の大きさも同じ。小さいほど難しい。目安は 17〜65。",
                  "hitRadius": %s,

                  "_nectarRate": "1ステップ（1/60秒）で増える満腹度。クリアまでの秒数 ＝ clearNectar ÷ この値 ÷ 60（0.45 で100 なら約3.7秒）。",
                  "nectarRate": %s,

                  "_clearNectar": "クリアに必要な満腹度。ここまで吸うとリザルトへ進む。左上のゲージが満タンになる値（ゲージの長さは変わらない）。",
                  "clearNectar": %s,

                  "_guideDistance": "花の周りに外側の点線リングが出始める距離（px）。近づくほどリングが縮む。大きいほど遠くから狙いを付けやすい。",
                  "guideDistance": %s,

                  "_texture": "花の画像の名前（拡張子なし）。共通の images/ に置いた正面向きの花の画像を使う。画像の真ん中が蜜の入り口になり、茎の傾きに合わせて回る。空なら図形の花。",
                  "texture": "%s",

                  "_textureSpin": "花の画像の回りの上乗せ。0 なら茎と同じだけ傾き、0.4 なら茎の傾きの 1.4 倍まで花が回る（花の真ん中が軸）。",
                  "textureSpin": %s,

                  "_textureCenterX": "花の画像の中で、花芯（蜜の入り口）が画像の真ん中から右へどれだけずれているか（画像の幅に対する %%。左はマイナス）。花芯をリングの中心に合わせるために使う。0 なら真ん中。",
                  "textureCenterX": %s,

                  "_textureCenterY": "同じく、花芯が画像の真ん中から下へどれだけずれているか（画像の高さに対する %%。上はマイナス）。",
                  "textureCenterY": %s
                }
                """.formatted(rootX, stemLength, stemWidth, flowerSize, swayWidth, swayPeriod, stiffness, damping, swaySpeed, hitRadius, nectarRate,
                        clearNectar, guideDistance, texture, textureSpin, textureCenterX, textureCenterY);
    }
}
