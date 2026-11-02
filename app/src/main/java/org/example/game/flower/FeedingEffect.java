package org.example.game.flower;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.core.DevMode;
import org.example.core.Input;

/**
 * 吸蜜エフェクト（蜜の粒）。吸っている間、花の入り口のまわりから蜜の粒が出て、
 * くちばしの「花の入り口に一番近い点」に吸い付き、くちばしの上を顔（根元）へ流れて消える。
 * くちばしのどこが花に当たっていても、当たっているところへ吸い込まれて見える。
 *
 * <ul>
 * <li>吸い始めの瞬間に、花から粒がはじけ出てから吸い込まれる</li>
 * <li>吸っている間、吸い付く点のまわりにときどき十字のきらめきが出る</li>
 * <li>吸うのをやめたら新しい粒は出さず、残りはそのまま吸い込まれる</li>
 * </ul>
 *
 * 数値は HUMMING 直下の effect_config.properties で変えられる（R キーで読み直す）。
 *
 * GameScene から使うときは:
 * <pre>
 * // update() の feeding.update(...) のあと
 * feedingEffect.update(state.status == GameState.Status.FEEDING, flower.x, flower.y,
 *         player.getBodyCenter(), player.getBeakTip());
 * // render() のプレイヤーを描く前（花へ向かって飛んでいる粒。ハチドリより奥）
 * feedingEffect.renderBehind(gc);
 * // render() のプレイヤーを描いたあと（くちばしの上を流れる粒と、きらめき。ハチドリより手前）
 * feedingEffect.renderFront(gc);
 * </pre>
 */
public class FeedingEffect {

    /** くちばしの始まり（体の中心からくちばしの先までの線のうち、どこからがくちばしか） */
    private static final double BEAK_START = 0.3;
    /** 粒が飛ぶときのふくらみ（px）の範囲 */
    private static final double CURVE_MIN = 13, CURVE_MAX = 30;
    /** 吸い始めにはじける速さ（px/秒）の範囲と、はじけている秒数 */
    private static final double BURST_SPEED_MIN = 54, BURST_SPEED_MAX = 130;
    private static final double BURST_SECONDS = 0.18;
    /** 十字のきらめきが出ている秒数と、吸い付く点からの散らばり（px） */
    private static final double CROSS_SECONDS = 0.45;
    private static final double CROSS_SPREAD_X = 30, CROSS_SPREAD_Y = 22;

    private static final Color SPARKLE_CORE = Color.web("#fffbe6");
    private static final Color SPARKLE_GLOW = Color.rgb(255, 230, 120, 0.45);
    private static final Color HIGHLIGHT = Color.rgb(255, 255, 255, 0.7);

    /** 蜜の粒1つ */
    static final class Particle {
        double offsetX, offsetY;       // 生まれた場所（花の入り口からのずれ）
        double burstVx, burstVy;       // はじける速さ（はじけない粒は 0）
        double burstX, burstY;         // はじけて動いた量
        double age, travel, bend, size;
        boolean white;
        Color color;
        // このステップの位置と大きさ（update で決めて、render で使う）
        double x, y, drawSize;
        boolean onBeak;
    }

    /** 十字のきらめき1つ */
    private static final class Cross {
        double x, y, age;
    }

    private final FeedingEffectConfig config;
    private final Random random;

    final List<Particle> particles = new ArrayList<>();
    private final List<Cross> crosses = new ArrayList<>();

    private boolean wasFeeding;
    private double spawnCarry;

    // このステップの位置
    private double entranceX, entranceY;
    double baseX, baseY;
    private double tipX, tipY;

    public FeedingEffect() {
        this(FeedingEffectConfig.load(), new Random());
    }

    /** 設定と乱数を決めて作る（テスト用）。 */
    FeedingEffect(FeedingEffectConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    /**
     * 1ステップ進める。
     * feeding は今吸っているか、(flowerX, flowerY) は花の入り口、bodyCenter はハチドリの体の中心、beakTip はくちばしの先。
     */
    public void update(boolean feeding, double flowerX, double flowerY, Point2D bodyCenter, Point2D beakTip) {
        if (DevMode.ENABLED && Input.isPressed(KeyCode.R)) config.reload(); // 開発用

        entranceX = flowerX;
        entranceY = flowerY;
        tipX = beakTip.getX();
        tipY = beakTip.getY();
        baseX = bodyCenter.getX() + (tipX - bodyCenter.getX()) * BEAK_START;
        baseY = bodyCenter.getY() + (tipY - bodyCenter.getY()) * BEAK_START;

        if (feeding && !wasFeeding) {
            for (int i = 0; i < config.burstCount; i++) spawn(true);
        }
        if (feeding) {
            spawnCarry += config.rate * Game.STEP;
            while (spawnCarry >= 1) {
                spawn(false);
                spawnCarry -= 1;
            }
        } else {
            spawnCarry = 0;
        }
        wasFeeding = feeding;

        double contact = contactRatio();
        for (Particle p : particles) {
            p.age += Game.STEP;
            move(p, contact);
        }
        particles.removeIf(p -> p.age >= p.travel + config.slideSeconds);

        updateCrosses(feeding, contact);
    }

    /** 花へ向かって飛んでいる粒を描く（ハチドリより奥）。 */
    public void renderBehind(GraphicsContext gc) {
        for (Particle p : particles) {
            if (!p.onBeak) drawParticle(gc, p);
        }
    }

    /** くちばしの上を流れる粒と、十字のきらめきを描く（ハチドリより手前）。 */
    public void renderFront(GraphicsContext gc) {
        for (Particle p : particles) {
            if (p.onBeak) drawParticle(gc, p);
        }
        gc.save();
        gc.setLineWidth(1.7);
        for (Cross c : crosses) {
            double k = c.age / CROSS_SECONDS;
            double r = 7.5 * (1 - Math.abs(k * 2 - 1)) + 1.1; // 大きくなってから小さくなる
            gc.setStroke(Color.rgb(255, 255, 240, 1 - k));
            gc.strokeLine(c.x - r, c.y, c.x + r, c.y);
            gc.strokeLine(c.x, c.y - r, c.x, c.y + r);
        }
        gc.restore();
    }

    // ============================================================
    // 粒
    // ============================================================

    private void spawn(boolean burst) {
        Particle p = new Particle();
        double angle = random.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(random.nextDouble()) * config.spawnRadius; // 円の中に均等に散らす
        p.offsetX = Math.cos(angle) * r;
        p.offsetY = Math.sin(angle) * r;
        if (burst) {
            double speed = between(BURST_SPEED_MIN, BURST_SPEED_MAX);
            p.burstVx = Math.cos(angle) * speed;
            p.burstVy = Math.sin(angle) * speed;
        }
        p.travel = config.travelSeconds * between(0.8, 1.2);
        p.bend = (random.nextBoolean() ? 1 : -1) * between(CURVE_MIN, CURVE_MAX);
        p.size = between(config.sizeMin, config.sizeMax);
        p.white = random.nextDouble() < config.sparkleRatio;
        p.color = config.colors.get(random.nextInt(config.colors.size()));
        particles.add(p);
    }

    /** 粒の位置を決める。花 → くちばしの一番近い点へ飛び、そのあとくちばしの上を根元へ流れる。 */
    private void move(Particle p, double contact) {
        if (p.age < BURST_SECONDS) {
            p.burstX += p.burstVx * Game.STEP;
            p.burstY += p.burstVy * Game.STEP;
        }

        if (p.age < p.travel) {
            // 飛んでいる間: 吸い付く点へ向かってカーブしながら、だんだん速く
            double k = p.age / p.travel;
            double e = k * k;
            double sx = entranceX + p.offsetX + p.burstX;
            double sy = entranceY + p.offsetY + p.burstY;
            double ex = beakX(contact), ey = beakY(contact);
            double mx = (sx + ex) / 2, my = (sy + ey) / 2 + p.bend;
            p.x = (1 - e) * (1 - e) * sx + 2 * (1 - e) * e * mx + e * e * ex;
            p.y = (1 - e) * (1 - e) * sy + 2 * (1 - e) * e * my + e * e * ey;
            p.drawSize = p.size * Math.min(1, k / 0.1); // 生まれた直後はふくらむ
            p.onBeak = false;
        } else {
            // くちばしの上: 吸い付いた点から根元へ流れて、最後に小さくなって消える
            double u = Math.min(1, (p.age - p.travel) / config.slideSeconds);
            double s = contact * (1 - u);
            p.x = beakX(s);
            p.y = beakY(s);
            p.drawSize = p.size * 0.8 * (u > 0.6 ? (1 - u) / 0.4 : 1);
            p.onBeak = true;
        }
    }

    private void drawParticle(GraphicsContext gc, Particle p) {
        double r = p.drawSize;
        if (r <= 0) return;
        if (p.white) {
            gc.setFill(SPARKLE_GLOW);
            gc.fillOval(p.x - r * 2, p.y - r * 2, r * 4, r * 4);
            gc.setFill(SPARKLE_CORE);
            gc.fillOval(p.x - r * 0.9, p.y - r * 0.9, r * 1.8, r * 1.8);
        } else {
            gc.setFill(p.color);
            gc.fillOval(p.x - r, p.y - r, r * 2, r * 2);
            gc.setFill(HIGHLIGHT); // 左上の小さな光で、しずくらしく見せる
            gc.fillOval(p.x - r * 0.7, p.y - r * 0.7, r * 0.7, r * 0.7);
        }
    }

    // ============================================================
    // きらめき
    // ============================================================

    private void updateCrosses(boolean feeding, double contact) {
        for (Cross c : crosses) c.age += Game.STEP;
        crosses.removeIf(c -> c.age >= CROSS_SECONDS);

        if (feeding && random.nextDouble() < config.crossPerSecond * Game.STEP) {
            Cross c = new Cross();
            c.x = beakX(contact) + between(-CROSS_SPREAD_X, CROSS_SPREAD_X);
            c.y = beakY(contact) + between(-CROSS_SPREAD_Y, CROSS_SPREAD_Y);
            crosses.add(c);
        }
    }

    // ============================================================
    // くちばしの線
    // ============================================================

    /** くちばしの線（根元 0 〜 先 1）の中で、花の入り口に一番近い点の位置。 */
    double contactRatio() {
        double vx = tipX - baseX, vy = tipY - baseY;
        double len2 = vx * vx + vy * vy;
        if (len2 < 1e-9) return 1;
        double s = ((entranceX - baseX) * vx + (entranceY - baseY) * vy) / len2;
        return Math.clamp(s, 0, 1);
    }

    private double beakX(double s) {
        return baseX + (tipX - baseX) * s;
    }

    private double beakY(double s) {
        return baseY + (tipY - baseY) * s;
    }

    private double between(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
