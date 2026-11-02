package org.example.stages.stage4;

import org.example.core.Game;

/** 画面揺れの描画位置を計算する。ゲーム内の座標は変更しない。 */
public final class ScreenShake {

    // 揺れの最大幅（px）。
    // 横方向・縦方向の揺れの最大幅（px）。
    private double amplitudeX;
    private double amplitudeY;

    // 揺れの継続時間と、開始からの経過時間（秒）。
    private double duration;
    private double elapsed;

    /**
     * 画面揺れを開始する。
     * seconds は秒数、amplitudeX・amplitudeY は各方向の最大幅（px）。
     * 秒数が0以下なら揺らさない。
     */
    public void start(double seconds, double amplitudeX, double amplitudeY) {
        duration = Math.max(0.0, seconds);
        this.amplitudeX = Math.max(0.0, amplitudeX);
        this.amplitudeY = Math.max(0.0, amplitudeY);
        elapsed = 0.0;
    }

    /** 毎ステップ（1/60秒）、経過時間を更新する。 */
    public void update() {
        elapsed = Math.min(duration, elapsed + Game.STEP);
    }

    /** 揺れの強さを返す。開始時の1から、終了時の0まで徐々に弱める。 */
    private double strength() {
        if (duration <= 0.0 || elapsed >= duration) {
            return 0.0;
        }
        return 1.0 - elapsed / duration;
    }

    /** 描画時の横方向のずれ（px）を返す。 */
    public double offsetX() {
        return Math.sin(elapsed * 90.0)
                * amplitudeX * strength();
    }

    /** 描画時の縦方向のずれ（px）を返す。 */
    public double offsetY() {
        return Math.sin(elapsed * 113.0)
                * amplitudeY * strength();
    }

    /** 画面揺れを終了し、経過時間をリセットする。 */
    public void reset() {
        duration = 0.0;
        elapsed = 0.0;
    }
}