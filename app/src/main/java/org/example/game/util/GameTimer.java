package org.example.game.util;

/**
 * ゲーム開始からの経過時間（秒）。
 * 表示は小数第2位まで（例: 12.34）。最大 9999.99 秒で止まる。頭に 0 は付けない。
 */
public class GameTimer {

    /** 1秒あたりのステップ数（Game.STEP = 1/60 秒） */
    private static final long STEPS_PER_SECOND = 60;
    /** 表示できる最大の時間（1/100 秒単位）。9999.99 秒 */
    static final long MAX_CENTIS = 999_999;

    // 小数を足し続けると誤差がたまるので、ステップ数（整数）で数える
    private long steps;
    private boolean running = true;

    /** 1ステップ進める。止めている間と最大に達したあとは進まない。 */
    public void update() {
        if (running && centis() < MAX_CENTIS) steps++;
    }

    public void stop() {
        running = false;
    }

    /** 経過時間（秒）。 */
    public double seconds() {
        return centis() / 100.0;
    }

    /** 表示用の文字列（例: 0.00 / 12.34 / 9999.99）。 */
    public String text() {
        return format(centis());
    }

    /** 経過時間（1/100 秒単位、切り捨て）。 */
    private long centis() {
        return Math.min(steps * 100 / STEPS_PER_SECOND, MAX_CENTIS);
    }

    static String format(long centis) {
        long c = Math.clamp(centis, 0, MAX_CENTIS);
        return (c / 100) + "." + String.format("%02d", c % 100);
    }
}
