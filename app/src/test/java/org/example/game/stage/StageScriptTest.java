package org.example.game.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.example.game.event.EventTypes;
import org.example.game.event.FogEvent;
import org.example.game.event.StageEvents;
import org.junit.jupiter.api.Test;

class StageScriptTest {

    @Test
    void everyStageHasItsOwnScriptClass() {
        for (int n = 1; n <= StageConfig.STAGE_COUNT; n++) {
            StageScript script = StageScript.load(n);
            assertNotSame(StageScript.NONE, script, "org.example.stages.stage" + n + ".Stage" + n + " が読み込めません");
            assertEquals("Stage" + n, script.getClass().getSimpleName());
        }
    }

    @Test
    void missingScriptDoesNothing() {
        assertSame(StageScript.NONE, StageScript.load(999));
    }

    @Test
    void stageCanRegisterItsOwnEvents() {
        EventTypes types = EventTypes.common();
        StageScript script = new StageScript() {
            @Override
            public void registerEvents(EventTypes t) {
                t.register("mist", FogEvent::new); // このステージだけのイベント（霧のクラスを別の名前で使う）
            }
        };
        script.registerEvents(types);

        assertTrue(types.names().containsAll(List.of("push", "flower", "mist")));
        for (String own : List.of("breeze", "gust", "fog")) {
            assertTrue(!EventTypes.common().names().contains(own), own + " はステージ固有のイベント（共通には入れない）");
        }
        StageEvents events = StageEvents.create(List.of(Map.of("type", "mist")), types);
        assertEquals(1, events.size());
        // 共通のイベントだけのときは使えない（ほかのステージには影響しない）
        assertEquals(0, StageEvents.create(List.of(Map.of("type", "mist"))).size());
    }
}
