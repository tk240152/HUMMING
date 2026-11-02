package org.example.game.flower;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.state.GameState;

/**
 * プレイヤーとお花の距離に応じて拡大・縮小するターゲットリング（〇）の管理・描画クラス。
 */
public class Target {

    private double currentDistance = 0.0;
    private boolean isInside = false;
    /** 中心の目標サークルの半径。吸蜜の当たり判定（ステージ設定の hitRadius）と同じ */
    private double targetR = 52.0;
    /** 外側のガイドリングを出し始める距離（ステージ設定の guideDistance） */
    private double guideDistance = 756.0;

    // 外側のガイドリングの半径 ＝ 距離 × この倍率（近づくほど縮む）
    private static final double OUTER_RING_SCALE = 0.45;

    // リングの色。ふだんはネオンシアン、吸っているときは緑
    private static final Color INNER_COLOR = Color.web("#00FEFC", 0.85);
    private static final Color OUTER_COLOR = Color.web("#00FEFC", 0.7);
    private static final Color INNER_FEEDING_COLOR = Color.web("#06d6a0");
    private static final Color OUTER_FEEDING_COLOR = Color.web("#06d6a0", 0.9);

    /**
     * 毎フレームの更新。距離と吸蜜中かどうかは、自分で計算せず Feeding の判定結果（GameState）を使う。
     * これで「リングが緑」と「蜜を吸えている」が必ず一致する。
     * @param state         Feeding.update() の後の GameState（distance・status）
     * @param hitRadius     吸蜜の当たり判定の半径（Flower.hitRadius()）
     * @param guideDistance 外側のリングを出し始める距離
     */
    public void update(GameState state, double hitRadius, double guideDistance) {
        this.currentDistance = state.distance;
        this.targetR = hitRadius;
        this.guideDistance = guideDistance;
        this.isInside = state.status == GameState.Status.FEEDING || state.status == GameState.Status.CLEAR;
    }

    /**
     * ガイドリングの描画
     * @param gc      GraphicsContext
     * @param flowerX お花のX座標
     * @param flowerY お花のY座標
     */
    public void render(GraphicsContext gc, double flowerX, double flowerY) {
        gc.save();

        // 1. 中心の目標サークル（お花の蜜腺の当たり判定位置）
        gc.setStroke(isInside ? INNER_FEEDING_COLOR : INNER_COLOR);
        gc.setLineWidth(isInside ? 7.5 : 4.3);
        gc.strokeOval(flowerX - targetR, flowerY - targetR, targetR * 2, targetR * 2);

        // 2. 遠くにあるときは大きく、近づくにつれてお花に向かってシュッと縮小していく外側のガイドリング（〇）
        // 距離が guideDistance 以内に入っているときだけ表示
        if (currentDistance < guideDistance) {
            double outerRadius = Math.max(targetR, currentDistance * OUTER_RING_SCALE);

            gc.setStroke(isInside ? OUTER_FEEDING_COLOR : OUTER_COLOR);
            gc.setLineWidth(5.4);
            // 点線（ダッシュ）にするとエースコンバットやパステルゲーム風のかわいい雰囲気になります
            gc.setLineDashes(17, 13);
            gc.strokeOval(flowerX - outerRadius, flowerY - outerRadius, outerRadius * 2, outerRadius * 2);
            gc.setLineDashes(null); // リセット
        }

        gc.restore();
    }

    public double getCurrentDistance() {
        return currentDistance;
    }

    public boolean isInside() {
        return isInside;
    }
}