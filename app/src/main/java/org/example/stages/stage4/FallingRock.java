package org.example.stages.stage4;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.example.media.Assets;
import org.example.core.Entity;
import org.example.core.Game;

/** 画面上方から落下する、一個の燃える岩。 */
public class FallingRock extends Entity {

    // 1ステップ（1/60秒）あたりの落下距離。
    private final double fallSpeed;

    // 接触の開始を音に使う。継続中も押す処理は行う。
    private boolean touchingPlayer = false;
    private boolean newContact = false;

    /**
     * 指定された横位置で、画面のすぐ上に岩を生成する。
     * textureKey の画像があれば、自動的にその画像で描く。
     */
    public FallingRock(double startX, double size, double fallSpeed, String textureKey) {
        super(textureKey, size, size);
        this.fallSpeed = fallSpeed;

        x = startX;
        y = -height / 2;
    }

    /** 岩を下へ移動し、少しずつ回転させる。 */
    @Override
    public void update() {
        y += fallSpeed;
    }

    /** 岩の判定中心と半径。 */
    public double hitCenterX() {
        return x;
    }

    public double hitCenterY() {
        return y + height * 0.06;
    }

    public double hitRadius() {
        return Math.min(width, height) * 0.27;
    }

    /** 接触中は毎回trueを返す。 */
    public boolean tryContact(double birdX, double birdY, double birdRadius) {
        double dx = birdX - hitCenterX();
        double dy = birdY - hitCenterY();
        double radius = hitRadius() + birdRadius;
        boolean touchingNow = !hasLeftScreen()
                && dx * dx + dy * dy <= radius * radius;

        newContact = touchingNow && !touchingPlayer;
        touchingPlayer = touchingNow;

        return touchingNow;
    }

    /** この岩との接触が、今回始まったか。 */
    public boolean isNewContact() {
        return newContact;
    }

    /** 岩全体が画面の下へ出たかどうかを返す。 */
    public boolean hasLeftScreen() {
        return y - height / 2 > Game.HEIGHT;
    }

    /** 画像がなければ、表示確認用の簡単な岩を描く。 */
    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        gc.translate(x, y);

        // 画像は描くときに読む（画面のないテストでも岩を作れるように）
        Image image = Assets.image(textureKey);
        if (image != null && !image.isError()) {
            gc.drawImage(
                    image,
                    -width / 2, -height / 2,
                    width, height);
        } else {
            // 炎を表す仮の外周。
            gc.setFill(Color.web("#E76F35"));
            gc.fillOval(
                    -width / 2 - 8.6, -height / 2 - 8.6,
                    width + 17, height + 17);

            // 岩の本体。
            gc.setFill(Color.web("#ADAAA9"));
            gc.fillOval(
                    -width / 2, -height / 2,
                    width, height);

            // 回転を確認するためのひび。
            gc.setStroke(Color.web("#655F5C"));
            gc.setLineWidth(4.3);
            gc.strokeLine(-17, -22, 4.3, 0);
            gc.strokeLine(4.3, 0, -6.5, 22);
        }

        /* 
                // 緑：実際に使っている岩の判定範囲。
        gc.setLineWidth(2);
        gc.setStroke(Color.LIME);

        double radius = hitRadius();
        double localX = hitCenterX() - x;
        double localY = hitCenterY() - y;

        gc.strokeOval(
                localX - radius, localY - radius,
                radius * 2, radius * 2);
*/

        gc.restore();
    }
}
