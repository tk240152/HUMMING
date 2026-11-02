package org.example.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.media.Assets;

/**
 * 画面に出るもの（プレイヤー・花など）の共通の親クラス。
 * x, y は「中心」の座標。angle はラジアン。
 * render を上書きしなければ textureKey の画像（無ければ仮の四角）が描かれる。
 */
public abstract class Entity {

    public double x, y;
    public double width, height;
    public double angle;
    public String textureKey;
    public Color fallbackColor = Color.GRAY;

    protected Entity(String textureKey, double width, double height) {
        this.textureKey = textureKey;
        this.width = width;
        this.height = height;
    }

    /** 1ステップごとの処理。 */
    public void update() {}

    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angle)); // JavaFX の rotate は「度」なので変換する
        Assets.draw(gc, textureKey, -width / 2, -height / 2, width, height, fallbackColor);
        gc.restore();
    }
}
