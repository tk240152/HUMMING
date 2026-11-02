package org.example.game.flower;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import org.example.media.Assets;
import org.example.core.Entity;
import org.example.core.Game;

/**
 * 花。茎の根元は画面の下端に固定され、風を受けて左右に揺れる。
 * 茎は同じ長さの節をつないだもので、節の向きを変えて曲げるので、どれだけ揺れても長さは変わらない。
 * x, y は花の入り口（蜜のある場所）。くちばしがここに重なると蜜を吸える。
 *
 * 揺れは物理で動かす。茎を「ばね（stiffness）＋空気抵抗（damping）」とみなし、
 * 通常の揺れ・風・イベント（そよ風など）はすべて茎を押す「力」として加える。
 * 押されると行き過ぎて戻る「しなり」は、ばねの動きから自然に生まれる。
 *
 * 位置は「通常の揺れの大きさを 1 とした値」（swayRatio）で扱う。1 なら右へ swayWidth の半分。
 *
 * stage.json の flower に "texture" を書くと、そのステージの正面向きの花の画像で描く
 * （画像の真ん中を入り口として、flowerSize x flowerSize の範囲に描き、茎の傾きに合わせて回す）。
 * 書いていなければ、画像 resources/images/flower.png（横向き。入り口を左端の中央として描く）か、図形で描く。
 */
public class Flower extends Entity {

    /** 茎の節の数。多いほどなめらかに曲がる */
    static final int SEGMENTS = 12;

    private final FlowerConfig config;

    /** 茎の曲がり具合（先端の傾き。ラジアン、右がプラス） */
    private double bend;

    // ばねの状態（通常の揺れの大きさを 1 とした位置と、その速さ）
    private double ratio;
    private double velocity;

    /** 一定のリズムで揺れるときの経過時間（秒） */
    private double swayTime;

    /** 通常の揺れの力にかける倍率（0〜1）。突風の間は 0 にして揺れを消す */
    private double swayGain = 1.0;

    /** このステップに加えられた外からの力（push の合計） */
    private double externalForce;

    final double[] stemX = new double[SEGMENTS + 1];
    final double[] stemY = new double[SEGMENTS + 1];

    public Flower(FlowerConfig config) {
        super("flower", config.flowerSize, config.flowerSize);
        this.config = config;
        startSwaying();
        updateStem();
    }

    /**
     * 1ステップ進める。wind は風の強さ（-1.0〜1.0）。
     * swayPeriod が 0 より大きければ一定のリズムの力で揺らし、0 なら不規則な風（wind）で揺らす。
     */
    public void update(double wind) {
        double k = config.stiffness;
        double c = config.damping;

        // 通常の揺れを作る力（押されていないときに、だいたい ±1 で揺れるように強さを決める）
        double drive;
        if (config.swayPeriod > 0) {
            swayTime += Game.STEP;
            double w = 2 * Math.PI / config.swayPeriod;
            drive = swayGain * driveStrength(k, c, w) * Math.sin(w * swayTime);
        } else {
            drive = k * wind;
        }

        // ばね: 元に戻ろうとする力 − 空気抵抗 ＋ 揺らす力 ＋ 外からの力
        double acceleration = -k * ratio - c * velocity + drive + k * externalForce;
        velocity += acceleration * Game.STEP;
        ratio += velocity * Game.STEP;
        externalForce = 0;

        bend = ratio * maxBend();
        updateStem();
    }

    /**
     * 花を押す（毎ステップ呼ぶ。呼ばなくなれば力は消える）。
     * force は「ずっと押し続けたときに落ち着く位置」で表す。-2 なら左へ通常の揺れの2倍あたりまで倒れる。
     * 押し始め・押し終わりには、ばねの勢いで行き過ぎて戻る（しなる）。
     */
    public void push(double force) {
        externalForce += force;
    }

    /** 今の位置を、通常の揺れの大きさを 1 とした値で返す（右がプラス）。 */
    public double swayRatio() {
        return ratio;
    }

    /** 通常の揺れの力の倍率を変える。0 で揺れが消え、1 で元の強さ。揺れの時間は止まらない。 */
    public void setSwayGain(double gain) {
        swayGain = Math.clamp(gain, 0, 1);
    }

    /**
     * 揺らす力の強さ。ばねを角速度 w で揺らしたとき、揺れ幅がちょうど 1 になる強さ。
     * （強制振動の振幅 = 力 ÷ √((k − w²)² + (c·w)²) を 1 にする）
     */
    private static double driveStrength(double k, double c, double w) {
        return Math.hypot(k - w * w, c * w);
    }

    /**
     * 最初から決まったリズムで揺れているように、揺れの途中の位置と速さから始める
     * （0 から始めると、揺れが落ち着くまで数秒かかるため）。
     */
    private void startSwaying() {
        if (config.swayPeriod <= 0) return;
        double k = config.stiffness;
        double c = config.damping;
        double w = 2 * Math.PI / config.swayPeriod;
        double lag = Math.atan2(c * w, k - w * w); // 力より揺れが遅れる分
        ratio = Math.sin(-lag);
        velocity = w * Math.cos(-lag);
    }

    /** 花の先が swayWidth の半分だけ横にずれるときの曲がり具合。 */
    double maxBend() {
        double reachPerBend = config.stemLength * (SEGMENTS + 1) / (2.0 * SEGMENTS);
        return (config.swayWidth / 2) / reachPerBend;
    }

    /** 根元から順に節をつなぐ。上の節ほど大きく傾ける。 */
    private void updateStem() {
        double seg = config.stemLength / SEGMENTS;
        stemX[0] = config.rootX;
        stemY[0] = Game.HEIGHT;
        for (int i = 0; i < SEGMENTS; i++) {
            double a = bend * (i + 1) / SEGMENTS;
            stemX[i + 1] = stemX[i] + seg * Math.sin(a);
            stemY[i + 1] = stemY[i] - seg * Math.cos(a);
        }

        x = stemX[SEGMENTS];
        y = stemY[SEGMENTS];
        angle = bend;
        width = height = config.flowerSize;
        
    }

    /** 先端の位置が当たり判定の範囲に入っているか。 */
    public boolean contains(double px, double py) {
        return Math.hypot(px - x, py - y) <= config.hitRadius;
    }

    public double hitRadius() {
        return config.hitRadius;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        // 茎
        gc.setStroke(Color.web("#27ae60"));
        gc.setLineWidth(config.stemWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.strokePolyline(stemX, stemY, stemX.length);

        // 花（入り口を原点にして、茎の先の傾きに合わせて回す）
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angle));

        double s = config.flowerSize;
        Image front = config.texture.isEmpty() ? null : Assets.image(config.texture);
        Image img = Assets.image(textureKey);
        if (front != null && !front.isError()) {
            // ステージの花の画像（正面向き）: 画像の真ん中を入り口に合わせる。茎の傾きと一緒に回り、
            // さらに textureSpin の分だけ花の真ん中を軸に上乗せして回る（傾くほど多めに回る、小さな風車のような動き）
            gc.rotate(Math.toDegrees(angle * config.textureSpin));
            // 花芯が画像の真ん中からずれている分（textureCenterX・Y）だけ逆にずらして、花芯を入り口に合わせる
            double cx = s * config.textureCenterX / 100;
            double cy = s * config.textureCenterY / 100;
            gc.drawImage(front, -s / 2 - cx, -s / 2 - cy, s, s);
        } else if (img != null) {
            gc.drawImage(img, 0, -s / 2, s, s);
        } else {
            double k = s / 60.0; // 花びらの形は大きさ 60 を基準にした比率で描く
            gc.setFill(Color.web("#ff4757"));
            gc.beginPath();
            gc.moveTo(0, -15 * k);
            gc.quadraticCurveTo(40 * k, -35 * k, 60 * k, -5 * k);
            gc.lineTo(60 * k, 5 * k);
            gc.quadraticCurveTo(40 * k, 35 * k, 0, 15 * k);
            gc.closePath();
            gc.fill();

            gc.setFill(Color.web("#ffa502"));
            gc.fillOval(-8 * k, -22 * k, 16 * k, 44 * k);
        }
        gc.restore();
    }
}
