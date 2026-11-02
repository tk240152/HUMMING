package org.example.game;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import org.example.core.Game;

/**
 * カットが切り替わった瞬間の効果。画面の周りが白くなる「ビネット」と、
 * 一点（ハチドリ）へ向かって集まる白い「集中線」を一瞬だけ出して、すぐに消す。
 * 大きさはすべて画面の大きさに対する割合で決めるので、解像度が変わってもそのまま使える。
 *
 * <pre>
 * cutEffect.start(CutEffect.Power.WEAK);   // カットの瞬間に1回呼ぶ
 * cutEffect.update();                      // 毎ステップ
 * cutEffect.render(gc, birdX, birdY);      // ズームをかけていない画面の座標で描く
 * </pre>
 */
public class CutEffect {

    /**
     * 効果の強さ。
     * lines: 集中線の本数、lineWidth: 線の根元の太さ（画面の高さに対する割合）、
     * lineLengthMin・Max: 画面に見える線の長さ（画面の端から集まる点までを 1 とした割合。大きいほど中まで伸びる）、
     * lineAlpha: 線の濃さ、vignetteAlpha: 周りの白の濃さ、vignetteInner: 白くなり始める所（中心から画面の角までを 1 とした割合）
     */
    public enum Power {
        WEAK(36, 0.015, 0.14, 0.26, 0.8, 0.75, 0.50),
        STRONG(52, 0.026, 0.24, 0.38, 1.0, 1.0, 0.32),
        STRONGEST(72, 0.040, 0.34, 0.52, 1.0, 1.0, 0.18);

        final int lines;
        final double lineWidth, lineLengthMin, lineLengthMax, lineAlpha, vignetteAlpha, vignetteInner;

        Power(int lines, double lineWidth, double lineLengthMin, double lineLengthMax,
                double lineAlpha, double vignetteAlpha, double vignetteInner) {
            this.lines = lines;
            this.lineWidth = lineWidth;
            this.lineLengthMin = lineLengthMin;
            this.lineLengthMax = lineLengthMax;
            this.lineAlpha = lineAlpha;
            this.vignetteAlpha = vignetteAlpha;
            this.vignetteInner = vignetteInner;
        }
    }

    /** くっきり出ている秒数と、出てから消えきるまでの秒数 */
    private static final double HOLD_SECONDS = 0.05;
    private static final double TOTAL_SECONDS = 0.12;

    private final Random random = new Random();

    private Power power;
    private double elapsed;
    // 集中線1本ずつの向き（ラジアン）と、画面に見える長さ（割合）
    private double[] angles = new double[0];
    private double[] lengths = new double[0];

    /** 効果を出す。前の効果が残っていても、新しい効果に置きかわる。 */
    public void start(Power power) {
        this.power = power;
        elapsed = 0;
        angles = new double[power.lines];
        lengths = new double[power.lines];
        for (int i = 0; i < power.lines; i++) {
            // 等間隔に並べてから少しずつずらし、手描きの集中線のようにばらつかせる
            angles[i] = (i + random.nextDouble() * 0.8) * 2 * Math.PI / power.lines;
            lengths[i] = power.lineLengthMin + random.nextDouble() * (power.lineLengthMax - power.lineLengthMin);
        }
    }

    public void update() {
        if (power != null) elapsed += Game.STEP;
    }

    /** 今の濃さ（0〜1）。HOLD_SECONDS の間は 1、そのあと TOTAL_SECONDS までに 0 になる。 */
    private double alpha() {
        if (power == null) return 0;
        if (elapsed < HOLD_SECONDS) return 1;
        return Math.clamp(1 - (elapsed - HOLD_SECONDS) / (TOTAL_SECONDS - HOLD_SECONDS), 0, 1);
    }

    /** (focusX, focusY) は集中線が集まる点（画面の座標）。 */
    public void render(GraphicsContext gc, double focusX, double focusY) {
        double a = alpha();
        if (a <= 0) return;
        gc.save();
        drawVignette(gc, a);
        drawLines(gc, a, focusX, focusY);
        gc.restore();
    }

    /** 画面の真ん中は透明のまま、周りほど白くする。 */
    private void drawVignette(GraphicsContext gc, double a) {
        double cx = Game.WIDTH / 2.0, cy = Game.HEIGHT / 2.0;
        double radius = Math.hypot(cx, cy); // 真ん中から画面の角まで
        gc.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(power.vignetteInner, Color.color(1, 1, 1, 0)),
                new Stop(1, Color.color(1, 1, 1, power.vignetteAlpha * a))));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
    }

    /** 画面の外から (fx, fy) へ向かう、先のとがった白い線を描く。 */
    private void drawLines(GraphicsContext gc, double a, double fx, double fy) {
        // 集まる点から一番遠い画面の角まで（線の根元は必ず画面の外に出す）
        double reach = Math.hypot(Math.max(fx, Game.WIDTH - fx), Math.max(fy, Game.HEIGHT - fy)) * 1.05;
        double halfWidth = Game.HEIGHT * power.lineWidth / 2;
        gc.setFill(Color.color(1, 1, 1, power.lineAlpha * a));
        double[] xs = new double[3];
        double[] ys = new double[3];
        for (int i = 0; i < angles.length; i++) {
            double dx = Math.cos(angles[i]), dy = Math.sin(angles[i]);
            // 線の先は、集まる点からその向きの画面の端までのうち、端から lengths の割合だけ内側
            double inner = edgeDistance(fx, fy, dx, dy) * (1 - lengths[i]);
            xs[0] = fx + dx * inner;                       // 線の先（とがった所）
            ys[0] = fy + dy * inner;
            xs[1] = fx + dx * reach - dy * halfWidth;      // 線の根元（画面の外）
            ys[1] = fy + dy * reach + dx * halfWidth;
            xs[2] = fx + dx * reach + dy * halfWidth;
            ys[2] = fy + dy * reach - dx * halfWidth;
            gc.fillPolygon(xs, ys, 3);
        }
    }

    /** (fx, fy) から (dx, dy) の向きに進んだとき、画面の端に着くまでの距離。 */
    private static double edgeDistance(double fx, double fy, double dx, double dy) {
        double tx = dx > 0 ? (Game.WIDTH - fx) / dx : dx < 0 ? -fx / dx : Double.MAX_VALUE;
        double ty = dy > 0 ? (Game.HEIGHT - fy) / dy : dy < 0 ? -fy / dy : Double.MAX_VALUE;
        return Math.max(0, Math.min(tx, ty));
    }
}
