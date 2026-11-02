package org.example.game.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.Json;
import org.example.game.flower.Wind;
import org.junit.jupiter.api.Test;

class StageEventsTest {

    private final GameState state = new GameState();
    private final FlowerConfig config = new FlowerConfig();
    private final Wind wind = new Wind();
    // Player は設定ファイルを作ってしまうので、テストでは null にする
    private final EventContext ctx = new EventContext(state, null, new Flower(config), config, wind);

    /** 突風・霧はステージ固有のイベントなので、ステージが登録するのと同じように登録してから作る。 */
    private StageEvents events(String json) {
        EventTypes types = EventTypes.common();
        types.register("gust", GustEvent::new);
        types.register("fog", FogEvent::new);
        return StageEvents.create(Json.array(Json.parseObject("{\"events\": " + json + "}"), "events"), types);
    }

    /** seconds 秒ぶん進める。 */
    private double run(StageEvents e, double from, double seconds) {
        double t = from;
        for (int i = 0; i < Math.round(seconds / Game.STEP); i++) {
            e.update(ctx, t);
            t += Game.STEP;
        }
        return t;
    }

    @Test
    void gustStartsAtTimeAndRestoresWindAfterDuration() {
        StageEvents e = events("[{\"type\": \"gust\", \"when\": {\"time\": 1}, \"duration\": 2, \"strength\": 0.5}]");

        double t = run(e, 0, 0.5);
        assertEquals(0, wind.extra);           // まだ始まっていない
        t = run(e, t, 1.0);
        assertEquals(0.5, wind.extra, 1e-9);   // 始まった
        run(e, t, 3.0);
        assertEquals(0, wind.extra, 1e-9);     // 終わって元に戻った
    }

    @Test
    void flowerChangeIsRestoredAtEnd() {
        StageEvents e = events("[{\"type\": \"flower\", \"duration\": 1, \"set\": {\"swayWidth\": 160}}]");
        double before = config.swayWidth;

        double t = run(e, 0, 0.1);
        assertEquals(160, config.swayWidth);
        run(e, t, 2);
        assertEquals(before, config.swayWidth);
    }

    @Test
    void nectarTriggerWaitsForPercent() {
        StageEvents e = events("[{\"type\": \"fog\", \"when\": {\"nectar\": 50}}]");
        state.maxNectar = 100;

        state.nectar = 49;
        e.update(ctx, 0);
        assertEquals(0, e.activeCount());

        state.nectar = 50;
        e.update(ctx, 0);
        assertEquals(1, e.activeCount());
    }

    @Test
    void endAllStopsEverythingAndRestores() {
        StageEvents e = events("[{\"type\": \"gust\", \"strength\": 0.7}]"); // duration なし＝ずっと続く
        run(e, 0, 1);
        assertEquals(0.7, wind.extra, 1e-9);
        e.endAll(ctx);
        assertEquals(0, wind.extra, 1e-9);
        assertEquals(0, e.activeCount());
    }

    @Test
    void everyRepeatsFromStartToStart() {
        // every: 始まりから始まりまで5秒（3秒続くので休みは2秒）
        StageEvents e = events("[{\"type\": \"fog\", \"when\": {\"every\": 5}, \"duration\": 3}]");
        double t = run(e, 0, 5.1);
        assertEquals(1, e.activeCount());   // 5秒
        t = run(e, t, 3.0);
        assertEquals(0, e.activeCount());   // 8秒で終わる
        run(e, t, 2.0);
        assertEquals(1, e.activeCount());   // 10秒でまた始まる
    }

    @Test
    void unknownTypeIsSkipped() {
        StageEvents e = events("[{\"type\": \"???\"}, {\"type\": \"fog\"}]");
        assertEquals(1, e.size());
    }

    @Test
    void blockFeedingLastsOnlyForThatStep() {
        StageEvent blocker = new StageEvent(Map.of("type", "test")) {
            @Override
            protected void onUpdate(EventContext c) {
                c.blockFeeding();
            }
        };
        StageEvents e = new StageEvents();
        e.add(blocker);

        e.update(ctx, 0);
        assertTrue(ctx.isFeedingBlocked());
        e.endAll(ctx);
        e.update(ctx, 0.1);
        assertFalse(ctx.isFeedingBlocked());
    }

    @Test
    void triggerWithoutWhenStartsImmediately() {
        assertTrue(StageEvent.trigger(null).test(ctx));
        assertFalse(StageEvent.trigger(Map.of("random", 1.0)).test(ctx)); // 知らない when は起きない
    }
}
