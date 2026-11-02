package org.example.stages.stage4;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import org.example.media.Assets;
import org.example.core.Game;

/** 噴火前（ため動作中）に、画面上部へ危険マークを横に3つ並べる（EruptionEvent の部品）。 */
public class EruptionWarning {

    // 危険マークの画像（images/ のファイル名）。拡張子はAssets側で付ける。
    private final String imageKey;

    /** 危険マークの画像の中で、絵のある範囲（左・上・幅・高さ。画像は 1080x1080 で、まわりが透明） */
    private static final double[] SRC = { 120, 169, 840, 742 };
    /** 危険マークの幅（px）。高さは画像の比で決まる */
    private static final double WIDTH = 120;
    /** マークの真ん中の高さ（px）。左上のゲージ（下端 y=190）・右上のステージ名（下端 y≈196）と重ならないよう、その下に出す */
    private static final double CENTER_Y = 290;
    /** 横の位置（マークの真ん中。画面を4等分した位置） */
    private static final double[] CENTER_X = { Game.WIDTH / 4.0, Game.WIDTH / 2.0, Game.WIDTH * 3 / 4.0 };

    // ため動作が始まってからの秒数。-1 なら表示していない。
    private double elapsed = -1;

    public EruptionWarning(String imageKey) {
        this.imageKey = imageKey;
    }

    /** 毎ステップ呼ぶ。ため動作中だけ時間を進める。 */
    public void update(boolean charging) {
        elapsed = charging ? Math.max(0, elapsed) + Game.STEP : -1;
    }

    /** ため動作中だけ、上からすっと降りてきて点滅する危険マークを描く。 */
    public void render(GraphicsContext gc) {
        if (elapsed < 0) {
            return;
        }
        // 画像は描くときに読む（画面のないテストでも作れるように）
        Image img = Assets.image(imageKey);
        if (img == null) {
            return;
        }

        double w = WIDTH;
        double h = w * SRC[3] / SRC[2];
        double slide = 1.0 - Math.pow(1.0 - Math.clamp(elapsed / 0.25, 0, 1), 3);
        double cy = CENTER_Y - (CENTER_Y + h / 2) * (1 - slide);
        double blink = 0.8 + 0.2 * Math.sin(elapsed * 16);

        gc.save();
        gc.setGlobalAlpha(blink);
        for (double cx : CENTER_X) {
            gc.drawImage(img, SRC[0], SRC[1], SRC[2], SRC[3], cx - w / 2, cy - h / 2, w, h);
        }
        gc.restore();
    }
}
