package org.example.stages.stage5;

import java.util.Random;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;

import org.example.core.Game;
import org.example.player.Player;

/**
 * 雷に当たったハチドリの「ビリビリ」演出（LightningEvent・RandomLightningEvent の部品）。
 * ハチドリの後ろに、まんがの「とげとげの吹き出し」のような形を出す。弾き飛ばされている間だけ出て、
 * とげの形は少しずつ変わり続ける（バチバチ光って見える）。ハチドリが隠れないよう、ハチドリより奥に描くこと。
 */
public class ElectricShock {

    /** とげの数 */
    private static final int SPIKES = 16;
    /** とげの先と谷の、体の中心からの距離（px）。先はこの範囲でばらつく */
    private static final double TIP_MIN = 105, TIP_MAX = 140, VALLEY = 72;
    /** 横長にする倍率（ハチドリが横長なので） */
    private static final double WIDE = 1.15;
    /** 内側の白いとげとげの大きさ（外側の何倍か） */
    private static final double INNER_SCALE = 0.68;
    /** とげの形を作り直す間隔（秒）。小さいほど激しくバチバチする */
    private static final double RESHAPE_SECONDS = 0.05;

    private static final Color FILL = Color.rgb(255, 225, 40);
    private static final Color INNER = Color.rgb(255, 255, 220);
    private static final Color LINE = Color.rgb(60, 40, 0);

    private final Random random = new Random();
    /** 外側の形（先と谷を交互に並べた点） */
    private final double[] xs = new double[SPIKES * 2];
    private final double[] ys = new double[SPIKES * 2];
    /** 内側の形（外側を縮めたもの） */
    private final double[] innerXs = new double[SPIKES * 2];
    private final double[] innerYs = new double[SPIKES * 2];

    private boolean active;
    private double reshapeWait;

    /** 雷に当たった瞬間に呼ぶ。ハチドリの弾き飛ばしが終わるまで出し続ける。 */
    public void start() {
        active = true;
        reshape(); // すぐ形を作る
        reshapeWait = RESHAPE_SECONDS;
    }

    /** 毎ステップ呼ぶ。弾き飛ばしが終わったら消す。 */
    public void update(Player player) {
        if (player == null || !player.isKnockedBack()) active = false;
        if (!active) return;

        reshapeWait -= Game.STEP;
        if (reshapeWait <= 0) {
            reshape();
            reshapeWait = RESHAPE_SECONDS;
        }
    }

    /** ハチドリの体の中心に、とげとげの吹き出しを描く（ハチドリより奥に描くこと）。 */
    public void render(GraphicsContext gc, Player player) {
        if (!active || player == null) return;
        Point2D c = player.getBodyCenter();

        gc.save();
        gc.translate(c.getX(), c.getY());
        gc.setLineJoin(StrokeLineJoin.MITER);

        // 黄色のとげとげ（こげ茶のふち付き）の上に、ひとまわり小さい白っぽいとげとげを重ねる
        gc.setFill(FILL);
        gc.fillPolygon(xs, ys, xs.length);
        gc.setStroke(LINE);
        gc.setLineWidth(5);
        gc.strokePolygon(xs, ys, xs.length);
        gc.setFill(INNER);
        gc.fillPolygon(innerXs, innerYs, innerXs.length);

        gc.restore();
    }

    /** とげの形を作り直す。全周に先と谷を交互に並べて、先の長さと向きを少しずつ変える。 */
    private void reshape() {
        double offset = random.nextDouble() * Math.PI * 2 / SPIKES;
        for (int i = 0; i < SPIKES * 2; i++) {
            double angle = offset + Math.PI * i / SPIKES;
            boolean tip = i % 2 == 0;
            double r = tip ? TIP_MIN + random.nextDouble() * (TIP_MAX - TIP_MIN) : VALLEY;
            xs[i] = Math.cos(angle) * r * WIDE;
            ys[i] = Math.sin(angle) * r;
            innerXs[i] = xs[i] * INNER_SCALE;
            innerYs[i] = ys[i] * INNER_SCALE;
        }
    }
}
