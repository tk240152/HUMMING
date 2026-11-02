package org.example.stages.stage1;

import java.util.Map;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import org.example.core.Game;
import org.example.media.Sound;
import org.example.game.util.Easing;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;

/**
 * ステージ1だけのイベント「そよ風」（Stage1.registerEvents で "breeze" として登録している）。
 * 共通のイベントではないので、ほかのステージでは使えない。似たものを作るときは、自分のステージのフォルダにコピーする。
 *
 * そよ風。吹いている間、花を向かい風（花からプレイヤーの方向）に押す。
 * 花の動きは物理（茎のばね）に任せるので、押し始めに行き過ぎて戻り、風がやむと反動で揺れてから通常の揺れに戻る。
 * 風の強さだけは、強まり・弱まりをなめらかにする（easeInOutSine）。
 * 吹いている間は、風の向きに流れる白い「～」の線を画面に描く（風の強さに合わせて濃くなる・薄くなる）。
 * <pre>
 * {"type": "breeze", "when": {"rest": 5}, "duration": 3, "strength": 2, "rampSeconds": 0.5,
 *  "lineLength": 130, "waveHeight": 13, "lineCount": 8, "lineSpeed": 864, "lineWidth": 4.3}
 * strength    : 風の強さ。ずっと吹き続けたときに花が落ち着く位置（通常の揺れの大きさを 1 とした倍率）。省略すると 2
 * rampSeconds : 風が強まる秒数・弱まる秒数（吹き始めと終わり）。0 だと急に吹いて急にやむ。省略すると 0.5
 * direction   : "headwind"（プレイヤーへの向かい風。省略時）/ "left" / "right"
 * lineLength  : 「～」の線の長さ（px）。省略すると 130
 * waveHeight  : 「～」のふくらみの大きさ（山の高さ。px）。省略すると 13
 * lineCount   : 画面に出す線の本数。省略すると 8
 * lineSpeed   : 線が流れる速さ（px/秒）。省略すると 864
 * lineWidth   : 線の太さ（px）。省略すると 4.3
 * se          : 吹き始めに鳴らす音（Sound.se の名前）。省略すると "breeze"。"" なら鳴らさない
 * </pre>
 */
public class BreezeEvent extends StageEvent {

    /** 「～」1本を描く点の数。多いほどなめらか */
    private static final int WAVE_POINTS = 24;
    /** 線のいちばん濃いときの不透明度 */
    private static final double MAX_ALPHA = 0.85;

    private final double strength;
    private final double rampSeconds;
    private final String direction;

    private final double lineLength;
    private final double waveHeight;
    private final int lineCount;
    private final double lineSpeed;
    private final double lineWidth;
    private final String se;

    private final Random random = new Random();
    final double[] lineX;
    private final double[] lineY;
    final double[] pointX = new double[WAVE_POINTS + 1];
    final double[] pointY = new double[WAVE_POINTS + 1];

    private double sign;  // 押す向き（右 +1 / 左 -1）

    public BreezeEvent(Map<String, Object> params) {
        super(params);
        strength = number("strength", 2);
        rampSeconds = Math.max(0, number("rampSeconds", 0.5));
        direction = string("direction", "headwind");

        lineLength = Math.max(1, number("lineLength", 130));
        waveHeight = number("waveHeight", 13);
        lineCount = (int) Math.max(0, number("lineCount", 8));
        lineSpeed = Math.max(0, number("lineSpeed", 864));
        lineWidth = Math.max(0.5, number("lineWidth", 4.3));
        se = string("se", "breeze");
        lineX = new double[lineCount];
        lineY = new double[lineCount];
    }

    @Override
    protected void onStart(EventContext ctx) {
        sign = sign(ctx);
        if (!se.isEmpty()) Sound.longSe(se); // 風の「サァー」（数秒続くので、ポーズで一時停止できる鳴らし方）
        // 線を画面のあちこちにばらまく
        for (int i = 0; i < lineCount; i++) {
            lineX[i] = random.nextDouble() * (Game.WIDTH + lineLength) - lineLength;
            lineY[i] = randomY();
        }
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        ctx.flower.push(sign * strength * envelope(activeTime()));
        moveLines();
    }

    /** 吹き始めてから t 秒後の風の強さの割合（0.0〜1.0）。強まる → 吹く → 弱まる。 */
    double envelope(double t) {
        if (rampSeconds <= 0) return 1;
        double in = Easing.inOutSine(t / rampSeconds);
        double out = duration > 0 ? Easing.inOutSine((duration - t) / rampSeconds) : 1;
        return Math.min(in, out);
    }

    /** 線を風の向きに流す。画面の外に出たら、反対側から別の高さで入り直す。 */
    private void moveLines() {
        double dx = sign * lineSpeed * Game.STEP;
        for (int i = 0; i < lineCount; i++) {
            lineX[i] += dx;
            if (sign < 0 && lineX[i] + lineLength < 0) {
                lineX[i] = Game.WIDTH + random.nextDouble() * lineLength;
                lineY[i] = randomY();
            } else if (sign > 0 && lineX[i] > Game.WIDTH) {
                lineX[i] = -lineLength - random.nextDouble() * lineLength;
                lineY[i] = randomY();
            }
        }
    }

    private double randomY() {
        double margin = Math.abs(waveHeight) + lineWidth;
        return margin + random.nextDouble() * (Game.HEIGHT - margin * 2);
    }

    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        double alpha = MAX_ALPHA * envelope(activeTime());
        if (alpha <= 0 || lineCount == 0) return;

        gc.save();
        gc.setStroke(Color.color(1, 1, 1, alpha));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        for (int i = 0; i < lineCount; i++) {
            wave(lineX[i], lineY[i]);
            gc.strokePolyline(pointX, pointY, pointX.length);
        }
        gc.restore();
    }

    /** (x, y) から右へ lineLength の長さの「～」の点を作る（山 → 谷の1往復）。 */
    void wave(double x, double y) {
        for (int p = 0; p <= WAVE_POINTS; p++) {
            double s = (double) p / WAVE_POINTS;
            pointX[p] = x + s * lineLength;
            pointY[p] = y - waveHeight * Math.sin(2 * Math.PI * s);
        }
    }

    /** 押す向き。右なら +1、左なら -1。 */
    private double sign(EventContext ctx) {
        return switch (direction) {
            case "left" -> -1;
            case "right" -> 1;
            default -> {
                // 向かい風: 花からプレイヤーのいる方へ吹く（プレイヤーが左にいれば左へ倒れる）
                if (ctx.player == null) yield -1;
                yield ctx.player.x < ctx.flower.x ? -1 : 1;
            }
        };
    }
}
