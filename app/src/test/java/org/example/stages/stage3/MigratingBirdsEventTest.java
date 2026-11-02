package org.example.stages.stage3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.stage.StageConfig;
import org.example.game.flower.Wind;
import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;
import org.example.game.event.StageEvents;
import org.junit.jupiter.api.Test;

/** ステージ3の渡り鳥（通常の時間のあと、決まった秒数で右端から左端へ横切る）。 */
class MigratingBirdsEventTest {

    private final FlowerConfig config = new FlowerConfig();
    private final EventContext ctx = new EventContext(new GameState(), null, new Flower(config), config, new Wind());

    @Test
    @SuppressWarnings("unchecked")
    void stage3ConfigRepeatsBirdsWithRestAndDuration() {
        StageConfig stage = StageConfig.load(3);
        EventTypes types = EventTypes.common();
        new Stage3().registerEvents(types);
        StageEvents events = StageEvents.create(stage.events, types);
        assertEquals(1, events.size()); // stage.json の birds が読めた

        // 秒数は stage.json の値を使う（調整で数字を変えてもテストが壊れないように）
        Map<String, Object> birds = (Map<String, Object>) stage.events.get(0);
        double rest = org.example.game.util.Json.number(org.example.game.util.Json.object(birds, "when"), "rest", -1);
        double duration = org.example.game.util.Json.number(birds, "duration", -1);
        assertTrue(rest > 0 && duration > 0, "birds に when.rest と duration が必要");

        // 通常 rest 秒 → 渡り鳥 duration 秒 → 通常 rest 秒 → …
        double t = 0;
        t = run(events, t, rest - 0.1);
        assertEquals(0, events.activeCount());   // rest 秒まではいない
        t = run(events, t, 0.2);
        assertEquals(1, events.activeCount());   // rest 秒で現れる
        t = run(events, t, duration);
        assertEquals(0, events.activeCount());   // duration 秒でいなくなる
        t = run(events, t, rest - 0.2);
        assertEquals(0, events.activeCount());   // まだ休み
        run(events, t, 0.2);
        assertEquals(1, events.activeCount());   // rest 秒休んだらまた現れる
    }

    private double run(StageEvents events, double t, double seconds) {
        for (int i = 0; i < Math.round(seconds / Game.STEP); i++) {
            events.update(ctx, t);
            t += Game.STEP;
        }
        return t;
    }

    @Test
    void flockCrossesFromRightToLeftInDuration() {
        // ゲームと同じように StageEvents を通して動かす（経過時間が進む）
        MigratingBirdsEvent[] created = new MigratingBirdsEvent[1];
        EventTypes types = EventTypes.common();
        types.register("birds", p -> created[0] = new MigratingBirdsEvent(p));
        StageEvents events = StageEvents.create(
                java.util.List.of(Map.of("type", "birds", "duration", 3.0)), types);
        MigratingBirdsEvent birds = created[0];

        double t = run(events, 0, Game.STEP);       // 始まった直後
        assertTrue(birds.centerX + birds.flockSize / 2 > Game.WIDTH, "右の画面の端から入ってくる");
        double first = birds.centerX;
        t = run(events, t, 1.5 - Game.STEP);         // 1.5秒
        assertTrue(birds.centerX < first, "左へ進む");
        assertEquals(Game.WIDTH / 2.0, birds.centerX, 15);  // 半分の時間で画面の真ん中あたり
        run(events, t, 1.5);                          // 3秒で横切り終わる
        assertTrue(birds.centerX + birds.flockSize / 2 < 0, "左の画面の外まで行った");
        assertEquals(0, events.activeCount());
    }

    /** 花の高さを1羽の鳥が横切ったときの、花のいちばん左の位置（通常の揺れの大きさ＝1）。 */
    private double leftmostFlowerWhenBirdPasses(double hitForce) {
        FlowerConfig c = new FlowerConfig(); // 風（wind）を 0 にして動かすので、押されたときだけ動く花
        c.swayWidth = 40;
        Flower f = new Flower(c);
        EventContext ec = new EventContext(new GameState(), null, f, c, new Wind());
        double flowerY = f.y;

        EventTypes types = EventTypes.common();
        new Stage3().registerEvents(types);
        // 1羽だけ、花の高さをちょうど通るようにする
        StageEvents events = StageEvents.create(java.util.List.of(Map.of(
                "type", "birds", "duration", 3.0, "count", 1.0, "flockScale", 1.2,
                "minY", flowerY, "maxY", flowerY, "hitForce", hitForce)), types);

        double t = 0, leftmost = Double.MAX_VALUE;
        for (int i = 0; i < 60 * 4; i++, t += Game.STEP) {
            events.update(ec, t);
            f.update(0);
            leftmost = Math.min(leftmost, f.swayRatio());
        }
        return leftmost;
    }

    @Test
    void birdHittingFlowerPushesItLeft() {
        double withoutHit = leftmostFlowerWhenBirdPasses(0);
        double withHit = leftmostFlowerWhenBirdPasses(3);
        assertTrue(withHit < withoutHit - 0.3,
                "ぶつかったときに花が左へ押されていない: " + withHit + "（ぶつからないとき " + withoutHit + "）");
    }

    @Test
    void birdFarFromFlowerDoesNotHit() {
        MigratingBirdsEvent birds = new MigratingBirdsEvent(Map.of("type", "birds", "count", 1.0));
        birds.onStart(ctx);
        birds.centerY = ctx.flower.y + 300; // 花よりずっと下
        assertTrue(!birds.hitsFlower(ctx, 0));
    }

    @Test
    void birdsAreSlightlyBiggerThanPlayerAndFlockIs4TimesPlayer() {
        MigratingBirdsEvent birds = new MigratingBirdsEvent(Map.of("type", "birds", "count", 7.0));
        birds.onStart(ctx); // プレイヤーがいない（テスト）ときは横幅 86 を基準にする
        assertEquals(103.2, birds.birdWidth, 1e-9); // 86 × 1.2
        assertEquals(344, birds.flockSize, 1e-9);   // 86 × 4
        for (int i = 0; i < 7; i++) {
            // 鳥が群れの四角からはみ出さない
            assertTrue(Math.abs(birds.offsetX[i]) + birds.birdWidth / 2 <= birds.flockSize / 2 + 1e-9);
            assertTrue(Math.abs(birds.offsetY[i]) + birds.birdHeight / 2 <= birds.flockSize / 2 + 1e-9);
        }
    }
}
