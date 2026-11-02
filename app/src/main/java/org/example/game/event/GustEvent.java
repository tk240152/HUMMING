package org.example.game.event;

import java.util.Map;

/**
 * 風の変化（突風）。続いている間、風に strength を足す（不規則な風で揺れるステージ＝swayPeriod が 0 で効く）。
 * <pre>
 * {"type": "gust", "when": {"time": 10}, "duration": 3, "strength": 0.8}
 * strength : -1.0（左向き）〜 1.0（右向き）。省略すると 0.8
 * </pre>
 *
 * カスタマイズ: strength(ctx) を上書きすると、時間とともに強さを変えられる（activeTime() で吹き始めからの秒数）。
 */
public class GustEvent extends StageEvent {

    /** JSON の strength */
    protected final double strength;

    /** 今、風に足している量（終わるときに引いて元に戻す） */
    private double applied;

    public GustEvent(Map<String, Object> params) {
        super(params);
        strength = number("strength", 0.8);
    }

    /** このステップの突風の強さ。上書きして変化をつけられる。 */
    protected double strength(EventContext ctx) {
        return strength;
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        double next = strength(ctx);
        ctx.wind.extra += next - applied;
        applied = next;
    }

    @Override
    protected void onEnd(EventContext ctx) {
        ctx.wind.extra -= applied;
        applied = 0;
    }
}
