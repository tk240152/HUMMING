package org.example.stages.stage4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.example.core.Game;
import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;
import org.example.game.event.StageEvents;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.flower.Wind;
import org.example.game.stage.StageConfig;
import org.example.state.GameState;
import org.junit.jupiter.api.Test;

/** ステージ4の火山（休む → ため → 噴火 をくり返す）。 */
class EruptionEventTest {

    private final FlowerConfig config = new FlowerConfig();
    private final EventContext ctx = new EventContext(new GameState(), null, new Flower(config), config, new Wind());

    @Test
    void stage4ConfigHasEruption() {
        EventTypes types = EventTypes.common();
        new Stage4().registerEvents(types);
        StageEvents events = StageEvents.create(StageConfig.load(4).events, types);
        assertEquals(1, events.size()); // stage.json の eruption が読めた
    }

    @Test
    void eruptionRepeatsWaitChargeErupt() {
        EruptionEvent[] made = new EruptionEvent[1];
        EventTypes types = EventTypes.common();
        types.register("eruption", p -> made[0] = new EruptionEvent(p));
        StageEvents events = StageEvents.create(List.of(Map.of(
                "type", "eruption",
                "firstWait", 2.0, "restSeconds", 5.0,
                "chargeSeconds", 1.0, "eruptSeconds", 3.0)), types);
        EruptionEvent eruption = made[0];
        assertNotNull(eruption);

        // 最初は 2 秒休む → 1 秒ため → 3 秒噴火 → 5 秒休む → またため
        double t = 0;
        t = run(events, t, 1.9);
        assertFalse(eruption.isCharging() || eruption.isErupting());
        t = run(events, t, 0.2);
        assertTrue(eruption.isCharging());
        t = run(events, t, 1.0);
        assertTrue(eruption.isErupting());
        t = run(events, t, 3.0);
        assertFalse(eruption.isCharging() || eruption.isErupting());
        t = run(events, t, 4.8);
        assertFalse(eruption.isCharging());
        run(events, t, 0.3);
        assertTrue(eruption.isCharging());
    }

    private double run(StageEvents events, double t, double seconds) {
        for (int i = 0; i < Math.round(seconds / Game.STEP); i++) {
            events.update(ctx, t);
            t += Game.STEP;
        }
        return t;
    }
}
