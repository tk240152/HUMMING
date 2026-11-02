package org.example.game.event;

import java.util.HashMap;
import java.util.Map;

import org.example.game.util.Json;

/**
 * 花の変化。続いている間、花の設定を書き換え、終わったら元に戻す。
 * <pre>
 * {"type": "flower", "when": {"nectar": 50}, "duration": 5, "set": {"swayWidth": 346, "hitRadius": 35}}
 * set : 変える項目と値（花の設定と同じ名前）
 * </pre>
 *
 * カスタマイズ: onStart(ctx) を上書きして super.onStart(ctx) のあとに処理を足すなど。終わったときの戻し方を変えるなら onEnd(ctx)。
 */
public class FlowerChangeEvent extends StageEvent {

    /** JSON の set */
    protected final Map<String, Object> changes;
    private final Map<String, Double> saved = new HashMap<>();

    public FlowerChangeEvent(Map<String, Object> params) {
        super(params);
        Map<String, Object> set = Json.object(params, "set");
        changes = set == null ? Map.of() : set;
    }

    @Override
    protected void onStart(EventContext ctx) {
        for (Map.Entry<String, Object> e : changes.entrySet()) {
            if (e.getKey().startsWith("_") || !(e.getValue() instanceof Double v)) continue;
            double old = ctx.flowerConfig.get(e.getKey());
            if (ctx.flowerConfig.set(e.getKey(), v)) {
                saved.put(e.getKey(), old);
            } else {
                System.err.println("[Event] flower: 知らない項目です: " + e.getKey());
            }
        }
    }

    @Override
    protected void onEnd(EventContext ctx) {
        saved.forEach(ctx.flowerConfig::set);
        saved.clear();
    }
}
