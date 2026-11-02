package org.example.game.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesNestedObjectsAndArrays() {
        Map<String, Object> m = Json.parseObject("""
                {
                  "name": "STAGE 1",
                  "flower": { "swayWidth": 100, "hitRadius": 24.5 },
                  "events": [ { "type": "gust", "when": { "time": 5 } }, true, null ],
                  "_説明": "日本語と \\"引用符\\" も読める"
                }
                """);
        assertEquals("STAGE 1", Json.string(m, "name", null));
        assertEquals(100.0, Json.number(Json.object(m, "flower"), "swayWidth", 0));
        List<Object> events = Json.array(m, "events");
        assertEquals(3, events.size());
        assertEquals(Boolean.TRUE, events.get(1));
        assertNull(events.get(2));
        assertEquals("日本語と \"引用符\" も読める", m.get("_説明"));
    }

    @Test
    void missingValuesFallBackToDefaults() {
        Map<String, Object> m = Json.parseObject("{ \"a\": \"text\" }");
        assertEquals(7.0, Json.number(m, "a", 7));       // 数値ではない
        assertEquals(7.0, Json.number(m, "none", 7));    // 無い
        assertTrue(Json.array(m, "none").isEmpty());
        assertNull(Json.object(m, "none"));
    }

    @Test
    void reportsLineNumberOnMistake() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Json.parseObject("{\n  \"a\": 1\n  \"b\": 2\n}"));
        assertTrue(e.getMessage().startsWith("3行目"), e.getMessage()); // カンマの付け忘れ
    }
}
