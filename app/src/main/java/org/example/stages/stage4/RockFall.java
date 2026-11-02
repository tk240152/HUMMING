package org.example.stages.stage4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;

import org.example.player.Player;

/** 左右のランダムな位置から、時間差で落石を発生させる（EruptionEvent の部品）。 */
public class RockFall {

    // 落下X座標の範囲。左は [400, 961)、右は [961, 1520)。
    private static final double LEFT_MIN_X = 400;
    private static final double SPLIT_X = 961;
    private static final double RIGHT_MAX_X = 1520;

    // 噴火開始から、最初の岩が落ちるまでの秒数。
    private final double startDelay;

    // 一個ずつ生成する間隔（秒）。
    private final double spawnInterval;

    // 左右それぞれの個数と、合計の個数。
    private final int totalRocks;

    // 鳥の本体の判定半径。
    private final double birdHitRadius;

    // 岩の大きさ・落ちる速さ・画像。
    private final double rockSize;
    private final double fallSpeed;
    private final String rockTexture;

    private final List<FallingRock> rocks = new ArrayList<>();
    private final Random random = new Random();

    // 今回の噴火で生成した岩の数。
    private int spawnedCount = 0;

    public RockFall(double startDelay, double spawnInterval, int rocksPerSide,
            double birdHitRadius,
            double rockSize, double fallSpeed, String rockTexture) {
        this.startDelay = startDelay;
        this.spawnInterval = Math.max(0.01, spawnInterval);
        this.totalRocks = Math.max(0, rocksPerSide) * 2;
        this.birdHitRadius = birdHitRadius;
        this.rockSize = rockSize;
        this.fallSpeed = fallSpeed;
        this.rockTexture = rockTexture;
    }

    /** 岩と生成時刻を更新し、今回衝突が受理された場合はtrueを返す。 */
    public boolean update(boolean erupting, double eruptionTime,
            Player player, Stage4HitEffect hitEffect) {
        boolean hitAccepted = false;

        for (FallingRock rock : rocks) {
            rock.update();

            if (player != null && hitEffect != null) {
                var body = player.getBodyCenter();

                if (rock.tryContact(
                        body.getX(), body.getY(), birdHitRadius)) {
                    // 接触中は毎ステップ押す。
                    hitEffect.receiveHit(player,
                            body.getX(), body.getY(),
                            rock.hitCenterX(), rock.hitCenterY());
                    // 音は各岩との接触開始時だけ鳴らす。
                    hitAccepted |= rock.isNewContact();
                }
            }
        }
        rocks.removeIf(FallingRock::hasLeftScreen);

        if (!erupting) {
            // 次の噴火では、一個目から生成する。
            spawnedCount = 0;
            return hitAccepted;
        }

        // 時刻を過ぎた岩を生成する。合計の個数で止める。
        while (spawnedCount < totalRocks
                && eruptionTime >= startDelay
                        + spawnInterval * spawnedCount - 1e-6) {
            spawnNextRock();
        }

        return hitAccepted;
    }

    /** 左右を交互に選び、その領域内のランダムな位置に生成する。 */
    private void spawnNextRock() {
        boolean leftSide = spawnedCount % 2 == 0;

        // 左側と右側で、生成できる横位置の範囲を分ける。
        double minX = leftSide ? LEFT_MIN_X : SPLIT_X;
        double maxX = leftSide ? SPLIT_X : RIGHT_MAX_X;

        // 横位置は生成時に一度だけ決める。
        double x = random.nextDouble(minX, maxX);

        rocks.add(new FallingRock(x, rockSize, fallSpeed, rockTexture));
        spawnedCount++;
    }

    /** 生成済みの岩が、まだ落下しているかどうかを返す。 */
    public boolean hasFallingRocks() {
        return !rocks.isEmpty();
    }

    /** 管理している岩をすべて描く。 */
    public void render(GraphicsContext gc) {
        for (FallingRock rock : rocks) {
            rock.render(gc);
        }
    }

    /** ステージ終了時に、残っている岩を取り除く。 */
    public void clear() {
        rocks.clear();
        spawnedCount = 0;
    }
}
