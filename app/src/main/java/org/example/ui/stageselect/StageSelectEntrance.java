package org.example.ui.stageselect;

import org.example.core.Game;

/** 花の成長と、選択画面の淡入を管理する。UI部品やキー入力は扱わない。 */
public final class StageSelectEntrance {

    // 花が根元から伸びきるまでの時間（秒）。
    private static final double FLOWER_GROW_SECONDS = 0.65;

    // 黒から選択画面が完全に見えるまでの時間（秒）。
    // 花の速度を変えずに淡入だけ調整できる。
    private static final double SCREEN_FADE_SECONDS = 0.65;

    private double elapsedTime;
    private boolean fadeIn;

    /** 画面に入るときは指定された淡入、詳細を閉じるときはfalseを使う。 */
    public void start(boolean fadeIn) {
        this.fadeIn = fadeIn;
        elapsedTime = 0;
    }

    public void update() {
        elapsedTime = Math.min(totalSeconds(), elapsedTime + Game.STEP);
    }

    public double flowerGrowth() {
        return easeOut(elapsedTime / FLOWER_GROW_SECONDS);
    }

    public double opacity() {
        return fadeIn ? easeOut(elapsedTime / SCREEN_FADE_SECONDS) : 1.0;
    }

    public boolean isFinished() {
        return elapsedTime >= totalSeconds();
    }

    private double totalSeconds() {
        return fadeIn ? Math.max(FLOWER_GROW_SECONDS, SCREEN_FADE_SECONDS)
                : FLOWER_GROW_SECONDS;
    }

    private static double easeOut(double value) {
        double p = Math.clamp(value, 0.0, 1.0);
        return 1 - Math.pow(1 - p, 3);
    }
}
