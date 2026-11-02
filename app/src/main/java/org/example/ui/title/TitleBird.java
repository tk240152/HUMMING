package org.example.ui.title;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import org.example.core.Game;
import org.example.media.Assets;

import org.example.player.PlayerParams;

/** タイトル画面専用のハチドリ。キー入力やゲームの状態は扱わない。 */
public final class TitleBird {

    // ゲーム画面と同じ画像を使用する。
    private final Image body = Assets.image("hummingbird_body");
    private final Image wing = Assets.image("hummingbird_wing");

    // 元画像の大きさと、画像内の羽の付け根。
    private static final double TEXTURE_SIZE = 2048;
    private static final double WING_ROOT_X = 860;
    private static final double WING_ROOT_Y = 1070;

    // ゲーム画面と同じ羽ばたき設定。
    private final PlayerParams params = new PlayerParams();

    // タイトルで1秒に進める、ゲームの羽ばたきステップ数。
    private static final double TITLE_WING_STEPS_PER_SECOND = 24.0;

    // 上下に浮く幅（片側の距離）と、1往復にかかる秒数。
    private static final double HOVER_AMPLITUDE = 13.0;
    private static final double HOVER_PERIOD_SECONDS = 2.4;

    private double elapsedTime = 0;

    // アニメーションの時間（秒）
    private static final double BACK_SECONDS = 0.5;   // 左へ下がる時間
    private static final double ALIGN_SECONDS = 0.15; // 飛び出すために前傾姿勢をとる時間
    private static final double FLIGHT_SECONDS = 0.55; // 右へ飛び出す時間

    // スタート決定後の離場状態と経過時間。
    private boolean departing = false;
    private double departureTime = 0;

    // 飛び出すときの体の角度（度）と、移動する距離。
    private static final double FLIGHT_BODY_ANGLE = 20.0;
    private static final double BACK_DISTANCE = 900.0;    // 左にどれくらい下がるか
    private static final double FLIGHT_DISTANCE = 2500.0; // 右にどれくらい飛ぶか（左端から画面外まで）

    /** 画面に入ったときに、すべてのアニメーションを初期化する。 */
    public void reset() {
        elapsedTime = 0;
        departing = false;
        departureTime = 0;
    }

    /** スタート決定時に、離場アニメーションを開始する。 */
    public void startDeparture() {
        if (departing) {
            return;
        }
        departing = true;
        departureTime = 0;
    }

    /** 離場全体の進み具合を0〜1で返す。 */
    public double departureProgress() {
        if (!departing) {
            return 0.0;
        }
        return Math.clamp(
                departureTime / (BACK_SECONDS + ALIGN_SECONDS + FLIGHT_SECONDS),
                0.0, 1.0);
    }

    /** 羽ばたきと、再生中の離場アニメーションを進める。 */
    public void update() {
        elapsedTime += Game.STEP;

        if (departing) {
            departureTime = Math.min(
                    BACK_SECONDS + ALIGN_SECONDS + FLIGHT_SECONDS,
                    departureTime + Game.STEP);
        }
    }

    public boolean isDeparting() {
        return departing;
    }

    /** 姿勢調整と飛び出しが、両方とも終了したか。 */
    public boolean isDepartureFinished() {
        return departing
                && departureTime >= BACK_SECONDS + ALIGN_SECONDS + FLIGHT_SECONDS;
    }

    /** 現在のX座標の移動量を返す（ワイプ追従用）。 */
    public double currentOffsetX() {
        if (!departing) return 0.0;

        double backProgress = Math.clamp(departureTime / BACK_SECONDS, 0.0, 1.0);
        double backEase = backProgress * backProgress * (3.0 - 2.0 * backProgress);

        double flightProgress = Math.clamp(
                (departureTime - BACK_SECONDS - ALIGN_SECONDS) / FLIGHT_SECONDS, 0.0, 1.0);
        
        return -BACK_DISTANCE * backEase + (FLIGHT_DISTANCE * flightProgress * flightProgress);
    }

    /** 右へ飛んでいる間の進み具合（0〜1）。飛び始める前は0。 */
    public double flightProgress() {
        if (!departing) return 0.0;
        return Math.clamp(
                (departureTime - BACK_SECONDS - ALIGN_SECONDS) / FLIGHT_SECONDS, 0.0, 1.0);
    }

    /** ホバリング・羽ばたき・姿勢調整・飛び出しを描く。 */
    public void render(GraphicsContext gc,
            double x, double y, double size) {

        if (body == null || body.isError()) {
            return;
        }

        // 1. 左へ下がる（開始前0〜完了1）
        double backProgress = departing
                ? Math.clamp(departureTime / BACK_SECONDS, 0.0, 1.0)
                : 0.0;
        double backEase = backProgress * backProgress * (3.0 - 2.0 * backProgress);

        // 2. 前傾姿勢を作る（開始前0〜完了1）
        double alignProgress = departing
                ? Math.clamp((departureTime - BACK_SECONDS) / ALIGN_SECONDS, 0.0, 1.0)
                : 0.0;
        double alignEase = alignProgress * alignProgress * (3.0 - 2.0 * alignProgress);

        // 3. 右へ飛ぶ
        double flightProgress = this.flightProgress();
        double flightEase = flightProgress * flightProgress; // 加速

        // 動きの合算
        double drawX = x + currentOffsetX();
        
        // 姿勢を整えながら、上下の揺れを徐々に止める。
        double hoverY = Math.sin(elapsedTime * 2 * Math.PI / HOVER_PERIOD_SECONDS) * HOVER_AMPLITUDE;
        double drawY = y + hoverY * (1.0 - alignEase);

        // 後ろに下がるときは少し上向きに、前に飛ぶときは前傾させる
        double bodyAngle = -10.0 * backEase + (FLIGHT_BODY_ANGLE + 10.0) * alignEase;

        gc.save();
        gc.translate(drawX + size / 2, drawY + size / 2);
        gc.rotate(bodyAngle);
        gc.translate(-size / 2, -size / 2);

        gc.drawImage(body, 0, 0, size, size);

        if (wing != null && !wing.isError()) {
            double rootX = WING_ROOT_X / TEXTURE_SIZE * size;
            double rootY = WING_ROOT_Y / TEXTURE_SIZE * size;

            long wingStep = (long) Math.floor(
                    elapsedTime * TITLE_WING_STEPS_PER_SECOND + 1e-6);
            double wingTime = wingStep * Game.STEP;
            double wingAngle = Math.sin(
                    wingTime * 2 * Math.PI * params.wingFlapsPerSecond)
                    * params.wingFlapDegrees;
                    
            gc.save();
            gc.translate(rootX, rootY);
            gc.rotate(wingAngle);
            gc.drawImage(wing, -rootX, -rootY, size, size);
            gc.restore();
        }

        gc.restore();
    }
}