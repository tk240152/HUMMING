package org.example.ui.title;

import org.example.core.Game;

/** タイトル・鳥・ボタンの登場順と移動時間だけを管理する。 */
public final class TitleEntrance {

    // タイトルの登場。
    private static final double TITLE_DELAY = 0.0;
    private static final double TITLE_SECONDS = 0.8;
    private static final double TITLE_OFFSET_Y = -497;

    // ハチドリの登場。羽ばたきや離場はTitleBirdで調整する。
    private static final double BIRD_DELAY = 0.15;
    private static final double BIRD_SECONDS = 0.8;
    private static final double BIRD_OFFSET_Y = 896;

    // 4つのボタンの登場。
    private static final int BUTTON_COUNT = 4;
    private static final double BUTTON_FIRST_DELAY = 0.3;
    private static final double BUTTON_INTERVAL = 0.15;
    private static final double BUTTON_SECONDS = 0.5;
    private static final double BUTTON_OFFSET_X = -816;

    private double elapsedTime;

    public void reset() {
        elapsedTime = 0;
    }

    public void update() {
        elapsedTime = Math.min(totalSeconds(), elapsedTime + Game.STEP);
    }

    public double titleOffsetY() {
        return TITLE_OFFSET_Y * (1 - progress(TITLE_DELAY, TITLE_SECONDS));
    }

    public double birdOffsetY() {
        return BIRD_OFFSET_Y * (1 - progress(BIRD_DELAY, BIRD_SECONDS));
    }

    public double buttonOffsetX(int index) {
        return BUTTON_OFFSET_X * (1 - progress(
                BUTTON_FIRST_DELAY + index * BUTTON_INTERVAL, BUTTON_SECONDS));
    }

    public boolean isFinished() {
        return elapsedTime >= totalSeconds();
    }

    private double totalSeconds() {
        return Math.max(TITLE_DELAY + TITLE_SECONDS,
                Math.max(BIRD_DELAY + BIRD_SECONDS,
                        BUTTON_FIRST_DELAY + (BUTTON_COUNT - 1) * BUTTON_INTERVAL
                                + BUTTON_SECONDS));
    }

    private double progress(double delay, double duration) {
        double p = Math.clamp((elapsedTime - delay) / duration, 0.0, 1.0);
        return 1 - Math.pow(1 - p, 3);
    }
}
