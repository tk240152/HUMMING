package org.example.game.flower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.core.ProjectFiles;
import org.junit.jupiter.api.Test;

/** 吸蜜エフェクト（蜜の粒）。 */
class FeedingEffectTest {

    // 花の入り口 (400, 250) に、左からくちばしの先が届いている
    private static final double FLOWER_X = 400, FLOWER_Y = 250;
    private static final Point2D BODY = new Point2D(354, 250);
    private static final Point2D TIP = new Point2D(400, 250);

    private FeedingEffect effect(double rate, int burst) {
        FeedingEffectConfig config = new FeedingEffectConfig();
        config.rate = rate;
        config.burstCount = burst;
        config.crossPerSecond = 0;
        return new FeedingEffect(config, new Random(1));
    }

    private void step(FeedingEffect e, boolean feeding, int steps) {
        for (int i = 0; i < steps; i++) e.update(feeding, FLOWER_X, FLOWER_Y, BODY, TIP);
    }

    @Test
    void noParticlesWhileNotFeeding() {
        FeedingEffect e = effect(25, 14);
        step(e, false, 120);
        assertEquals(0, e.particles.size());
    }

    @Test
    void burstsWhenFeedingStarts() {
        FeedingEffect e = effect(0, 14);
        step(e, true, 1);
        assertEquals(14, e.particles.size()); // 吸い始めにまとめて出る
        step(e, true, 1);
        assertEquals(14, e.particles.size()); // 吸い続けても、はじけるのは最初だけ
    }

    @Test
    void spawnsAboutRatePerSecond() {
        FeedingEffect e = effect(30, 0);
        step(e, true, (int) Math.round(0.2 / Game.STEP));
        assertEquals(6, e.particles.size()); // 0.2 秒で 30 × 0.2 = 6 粒（まだ1つも届いていない）
    }

    @Test
    void particlesSlideToBeakBaseAndDisappear() {
        FeedingEffect e = effect(0, 1);
        step(e, true, 1);
        FeedingEffect.Particle p = e.particles.get(0);

        // 届く秒数と流れる秒数のぎりぎり手前まで進めると、くちばしの根元の近くにいる
        double total = p.travel + new FeedingEffectConfig().slideSeconds;
        step(e, false, (int) Math.floor(total / Game.STEP) - 2);
        assertTrue(p.onBeak, "くちばしの上を流れているはず");
        assertTrue(Math.hypot(p.x - e.baseX, p.y - e.baseY) < 5,"根元の近くにいるはず");

        step(e, false, 5);
        assertEquals(0, e.particles.size()); // 流れ終わったら消える
    }

    @Test
    void contactIsNearestPointOnBeak() {
        FeedingEffect e = effect(0, 0);
        // くちばしが花の上をかすめている（先は入り口より右・上）
        e.update(true, FLOWER_X, FLOWER_Y, new Point2D(370, 232), new Point2D(416, 232));
        double s = e.contactRatio();
        assertTrue(s > 0.3 && s < 1, "先ではなく、くちばしの途中に吸い付くはず: " + s);
    }

    @Test
    void readsConfigAndKeepsDefaultsForMistakes() {
        FeedingEffectConfig config = new FeedingEffectConfig();
        Properties props = new Properties();
        props.setProperty("feeding.rate", "40");
        props.setProperty("feeding.burstCount", "abc");     // 書き間違い → 初期値のまま
        props.setProperty("feeding.colors", "#ff0000, 赤"); // 色でない値は飛ばす
        config.apply(props);

        assertEquals(40, config.rate);
        assertEquals(14, config.burstCount);
        assertEquals(1, config.colors.size());
        assertEquals(Color.web("#ff0000"), config.colors.get(0));
    }

    @Test
    void configFileInProjectIsReadable() {
        Path file = ProjectFiles.resolve(FeedingEffectConfig.FILE_NAME);
        FeedingEffectConfig config = new FeedingEffectConfig();
        config.loadFrom(file);
        assertTrue(config.rate > 0);
        assertTrue(!config.colors.isEmpty());
    }
}
