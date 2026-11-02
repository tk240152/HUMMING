package org.example.stages.stage4;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.player.Player;

/** 岩に当たったときの衝撃、保護時間、炎の描画。 */
public class Stage4HitEffect {

    // 炎だけを表示する時間（秒）。
    private static final double BURN_SECONDS = 1.8;

    // 弾き飛ばす強さ。6を標準とし、移動距離に反映する。
    private final double knockbackForce;

    // 次の衝撃を受けない時間（秒）。
    private final double hurtSeconds;

    private double hurtTime = 0;
    private double burnTime = 0;
    private double burnElapsed = 0;

    public Stage4HitEffect(double knockbackForce, double hurtSeconds) {
        this.knockbackForce = knockbackForce;
        this.hurtSeconds = Math.max(0.01, hurtSeconds);
    }

    /** 姿勢と炎の表示時間を進める。 */
    public void update() {
        if (burnTime > 0) {
            burnElapsed += Game.STEP;
        }
        hurtTime = Math.max(0, hurtTime - Game.STEP);
        burnTime = Math.max(0, burnTime - Game.STEP);
    }

    /** 次の衝撃を受けない保護時間中か。 */
    public boolean isActive() {
        return hurtTime > 0;
    }

    /** 接触している間、岩から離れる方向へ押す。 */
    public boolean receiveHit(Player player,
            double birdX, double birdY, double sourceX, double sourceY) {
        if (player == null) {
            return false;
        }

        double dx = birdX - sourceX;
        double dy = birdY - sourceY;
        double distance = Math.hypot(dx, dy);

        // 中心が重なった場合は、下方向へ押す。
        if (distance < 1.0e-6) {
            dx = 0;
            dy = 1;
            distance = 1;
        }

        // 設定値6のとき、1ステップあたり0.6の速度を加える。
        double push = Math.max(0.0, knockbackForce) * 0.1;
        player.addForce(
                dx / distance * push,
                dy / distance * push);

        // 音の開始判定だけに使用する。押す処理は毎回実行する。
        boolean newBurn = burnTime <= 0;
        if (newBurn) {
            burnElapsed = 0;
        }

        // この時間は外見だけに使い、衝突を拒否しない。
        hurtTime = hurtSeconds;
        burnTime = BURN_SECONDS;
        return newBurn;
    }

    /**
     * Playerが合わせた位置と傾きの中で、姿勢と炎を描く。
     * falseを返し、通常のハチドリと羽ばたきの描画を続ける。
     */
    public boolean render(GraphicsContext gc) {
        if (burnTime <= 0) {
            return false;
        }

        double elapsed = burnElapsed;

        // 最初の0.1秒で頭を下げ、保護時間の最後の0.2秒で戻す。
        double tiltIn = Math.min(1.0, elapsed / 0.1);
        double tiltOut = Math.min(1.0, hurtTime / 0.2);

        // 姿勢の変化を滑らかにする。
        tiltIn = tiltIn * tiltIn * (3.0 - 2.0 * tiltIn);
        tiltOut = tiltOut * tiltOut * (3.0 - 2.0 * tiltOut);

        // この回転は、後から描く通常のハチドリにも適用する。
        // Player.render()の最後で元の座標系に戻る。
        gc.rotate(40.0 * tiltIn * tiltOut);

        gc.save();

        // 燃焼時間の最後の0.35秒で、炎を薄くする。
        gc.setGlobalAlpha(
                gc.getGlobalAlpha() * Math.min(1.0, burnTime / 0.35));

        // 一つにつながった炎を、全体でゆっくり揺らす。
        double sway = Math.sin(elapsed * 5) * 2;
        double stretch = 1.0 + Math.sin(elapsed * 6) * 0.06;

        gc.save();
        // 炎の形は 800×500 の画面のときの大きさで書いてあるので、1920×1080 に合わせて拡大する
        gc.scale(2.16, 2.16);
        gc.translate(-10, 12);
        gc.rotate(-6 + sway);
        gc.scale(1.0, stretch);

        // 外側のオレンジ色の炎。
        drawFlame(gc, Color.web("#F06424"));

        // 内側の黄色い炎。同じ輪郭を小さく重ねる。
        gc.translate(0, 5);
        gc.scale(0.62, 0.68);
        drawFlame(gc, Color.web("#FFD166"));

        gc.restore();
        gc.restore();

        // 通常のハチドリと羽ばたきの描画を続ける。
        return false;
    }

    /** 三つの火先を持つ、一続きの炎を描く。 */
    private void drawFlame(GraphicsContext gc, Color color) {
        gc.setFill(color);
        gc.beginPath();
        gc.moveTo(-28, 12);

        // 左の低い火先。
        gc.quadraticCurveTo(-38, -8, -24, -36);
        gc.quadraticCurveTo(-25, -21, -13, -16);

        // 中央の高い火先。
        gc.quadraticCurveTo(-15, -40, -4, -62);
        gc.quadraticCurveTo(2, -43, 6, -27);

        // 右の火先。
        gc.quadraticCurveTo(16, -37, 18, -46);
        gc.quadraticCurveTo(34, -10, 28, 12);

        // 下側をつないで閉じる。
        gc.quadraticCurveTo(0, 24, -28, 12);
        gc.closePath();
        gc.fill();
    }

    /** ステージを抜けたら、専用の状態を残さない。 */
    public void reset() {
        hurtTime = 0;
        burnTime = 0;
    }
}