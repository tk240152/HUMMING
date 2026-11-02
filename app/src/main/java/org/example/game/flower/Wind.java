package org.example.game.flower;

import java.util.Random;

import org.example.core.Game;

/**
 * 風。-1.0（左向き）〜 1.0（右向き）の強さを、不規則に変化させる。
 * 周期のそろわないサイン波（そよ風）に、ときどき向きと強さが変わる突風を混ぜている。
 */
public class Wind {

    private final Random random;

    private double time;
    private double gust;
    private double gustTarget;
    private double gustTimer;
    private double value;

    /** イベント（突風など）で足される風。-1.0〜1.0 の範囲に収まるよう、合計してから切り詰める */
    public double extra;

    public Wind() {
        this(new Random());
    }

    /** 乱数を固定したいとき（テストなど）に使う。 */
    public Wind(Random random) {
        this.random = random;
    }

    /** speed は揺れの速さの倍率（1.0 が標準）。 */
    public void update(double speed) {
        double dt = Game.STEP * speed;
        time += dt;

        // 突風: 1〜3秒ごとに次の目標を決め、そこへゆっくり近づく
        gustTimer -= dt;
        if (gustTimer <= 0) {
            gustTarget = random.nextDouble() * 2 - 1;
            gustTimer = 1 + random.nextDouble() * 2;
        }
        gust += (gustTarget - gust) * Math.min(1, 1.5 * dt);

        // そよ風: 周期が割り切れない3つの波を重ねるので、同じ動きを繰り返さない
        double breeze = 0.5 * Math.sin(time * 1.1)
                + 0.3 * Math.sin(time * 2.7 + 1.3)
                + 0.2 * Math.sin(time * 0.37 + 2.1);

        value = Math.clamp(breeze * 0.6 + gust * 0.4 + extra, -1.0, 1.0);
    }

    /** 今の風の強さ（-1.0〜1.0）。 */
    public double value() {
        return value;
    }
}
