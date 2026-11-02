package org.example.game.event;

import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.core.Game;

/**
 * 視界不良（霧）。続いている間、画面に霧をかける。始まりと終わりは fadeSeconds 秒かけて濃く・薄くなる。
 * <pre>
 * {"type": "fog", "when": {"time": 15}, "duration": 6, "density": 0.7, "color": "#ffffff", "fadeSeconds": 0.5}
 * density     : 霧の濃さ 0.0〜1.0。省略すると 0.7
 * color       : 霧の色。省略すると白
 * fadeSeconds : 濃くなる・薄くなる秒数。省略すると 0.5
 * </pre>
 *
 * カスタマイズ: color(ctx)・alpha(ctx) を上書きすると色や濃さを変えられる。
 * 霧の形を変えたいとき（プレイヤーの周りだけ晴れるなど）は drawFog(gc, ctx, color) を上書きする。
 */
public class FogEvent extends StageEvent {

    /** JSON の density・color・fadeSeconds */
    protected final double density;
    protected final Color color;
    protected final double fadeSeconds;

    public FogEvent(Map<String, Object> params) {
        super(params);
        density = Math.clamp(number("density", 0.7), 0, 1);
        color = parseColor(string("color", "#ffffff"));
        fadeSeconds = Math.max(0, number("fadeSeconds", 0.5));
    }

    /** 霧の色。上書きして変えられる。 */
    protected Color color(EventContext ctx) {
        return color;
    }

    /** 霧の濃さ（0.0〜1.0）。初期は density に、始まりと終わりのフェードを掛けたもの。 */
    protected double alpha(EventContext ctx) {
        return density * fade(fadeSeconds);
    }

    /** 霧を描く。初期は画面全体を塗る。 */
    protected void drawFog(GraphicsContext gc, EventContext ctx, Color fogColor) {
        gc.setFill(fogColor);
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
    }

    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        Color c = color(ctx);
        double a = Math.clamp(alpha(ctx), 0, 1);
        if (a <= 0) return;
        drawFog(gc, ctx, Color.color(c.getRed(), c.getGreen(), c.getBlue(), a));
    }

    private static Color parseColor(String text) {
        try {
            return Color.web(text);
        } catch (IllegalArgumentException e) {
            System.err.println("[Event] fog: 色の書き方が間違っています: " + text);
            return Color.WHITE;
        }
    }
}
