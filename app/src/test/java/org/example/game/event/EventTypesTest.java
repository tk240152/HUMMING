package org.example.game.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import javafx.scene.paint.Color;

import org.example.state.GameState;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.Json;
import org.example.game.flower.Wind;
import org.junit.jupiter.api.Test;

class EventTypesTest {

    private final FlowerConfig config = new FlowerConfig();
    private final Wind wind = new Wind();
    private final EventContext ctx = new EventContext(new GameState(), null, new Flower(config), config, wind);

    private static StageEvent create(EventTypes types, String json) {
        Map<String, Object> params = Json.parseObject(json);
        return types.get(Json.string(params, "type", null)).apply(params);
    }

    @Test
    void commonHasOnlyPushAndFlower() {
        assertEquals(List.of("push", "flower"), List.copyOf(EventTypes.common().names()));
        assertTrue(EventTypes.library().containsAll(List.of("push", "flower", "gust", "fog")));
    }

    @Test
    void useEnablesLibraryEventsByName() {
        EventTypes types = EventTypes.common();
        assertFalse(types.names().contains("fog"));
        types.use("fog", "gust", "???"); // 無い名前は警告を出して飛ばす
        assertTrue(types.names().containsAll(List.of("fog", "gust")));
        assertFalse(types.names().contains("???"));
    }

    @Test
    void presetChangesDefaultsButJsonWins() {
        EventTypes types = EventTypes.common();
        types.register("thickFog", EventTypes.preset("fog", Map.of("density", 0.95, "color", "#888888")));

        FogEvent byPreset = (FogEvent) create(types, "{ \"type\": \"thickFog\" }");
        assertEquals(0.95, byPreset.density);
        assertEquals(Color.web("#888888"), byPreset.color);

        FogEvent byJson = (FogEvent) create(types, "{ \"type\": \"thickFog\", \"density\": 0.3 }");
        assertEquals(0.3, byJson.density); // stage.json に書いた値が優先
    }

    @Test
    void subclassCanOverrideBehavior() {
        EventTypes types = EventTypes.common();
        types.register("pinkFog", p -> new FogEvent(p) {
            @Override
            protected Color color(EventContext c) {
                return Color.PINK;
            }
        });
        FogEvent fog = (FogEvent) create(types, "{ \"type\": \"pinkFog\" }");
        assertEquals(Color.PINK, fog.color(ctx));
    }

    @Test
    void gustCanChangeStrengthOverTimeAndRestoresWind() {
        EventTypes types = EventTypes.common();
        types.register("risingGust", p -> new GustEvent(p) {
            @Override
            protected double strength(EventContext c) {
                return Math.min(1, activeTime()); // 1秒かけて強くなる
            }
        });
        StageEvents events = StageEvents.create(Json.array(Json.parseObject(
                "{\"events\": [{\"type\": \"risingGust\", \"duration\": 2}]}"), "events"), types);

        double t = 0;
        for (int i = 0; i < 30; i++, t += org.example.core.Game.STEP) events.update(ctx, t);
        assertTrue(wind.extra > 0.4 && wind.extra < 0.6, "0.5秒で半分くらい: " + wind.extra);
        for (int i = 0; i < 150; i++, t += org.example.core.Game.STEP) events.update(ctx, t);
        assertEquals(0, wind.extra, 1e-9); // 終わったら元に戻る
    }
}
