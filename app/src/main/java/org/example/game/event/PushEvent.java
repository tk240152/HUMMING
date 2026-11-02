package org.example.game.event;

import java.util.Map;

/**
 * プレイヤーへの影響。続いている間、毎ステップ プレイヤーに力を加える（風で押されるなど）。
 * <pre>
 * {"type": "push", "when": {"time": 8}, "duration": 2, "forceX": -0.1, "forceY": 0}
 * forceX, forceY : 1ステップあたりに加える速度（px）。右・下がプラス
 * </pre>
 *
 * カスタマイズ: forceX(ctx)・forceY(ctx) を上書きすると、時間やプレイヤーの位置で力を変えられる。
 */
public class PushEvent extends StageEvent {

    /** JSON の forceX・forceY */
    protected final double forceX;
    protected final double forceY;

    public PushEvent(Map<String, Object> params) {
        super(params);
        forceX = number("forceX", 0);
        forceY = number("forceY", 0);
    }

    /** このステップの横の力。上書きして変化をつけられる。 */
    protected double forceX(EventContext ctx) {
        return forceX;
    }

    /** このステップの縦の力。上書きして変化をつけられる。 */
    protected double forceY(EventContext ctx) {
        return forceY;
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        if (ctx.player != null) ctx.player.addForce(forceX(ctx), forceY(ctx));
    }
}
