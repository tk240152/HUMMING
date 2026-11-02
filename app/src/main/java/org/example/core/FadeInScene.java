package org.example.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * ほかの画面を包んで、始まりだけ color の幕から seconds 秒かけてだんだん見えてくるようにする。
 * 包まれた画面のクラスは変えなくてよい（動き・描画・P キーの次の画面はそのまま任せる）。
 * <pre>
 * Game.change(new FadeInScene(new ResultScene(state), Color.WHITE, 0.5)); // 白からリザルトへ
 * </pre>
 */
public class FadeInScene implements BaseScene {

    private final BaseScene inner;
    private final Color color;
    private final double seconds;
    private double elapsed;

    public FadeInScene(BaseScene inner, Color color, double seconds) {
        this.inner = inner;
        this.color = color;
        this.seconds = Math.max(Game.STEP, seconds);
    }

    @Override
    public void onEnter() {
        inner.onEnter();
    }

    @Override
    public void onExit() {
        inner.onExit();
    }

    @Override
    public String bgm() {
        return inner.bgm();
    }

    @Override
    public void update() {
        elapsed += Game.STEP;
        inner.update();
    }

    @Override
    public void render(GraphicsContext gc) {
        inner.render(gc);
        double a = 1 - Math.clamp(elapsed / seconds, 0, 1);
        if (a <= 0) return;
        gc.save();
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), a));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        gc.restore();
    }

    @Override
    public BaseScene next() {
        return inner.next();
    }
}
