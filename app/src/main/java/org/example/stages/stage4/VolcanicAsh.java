package org.example.stages.stage4;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.core.Game;

/** 噴火中に視界を遮る火山灰（EruptionEvent の部品）。 */
public class VolcanicAsh {

    // 火山灰の色。
    private final Color ashColor;
    // 最大不透明度。値を大きくすると、視界が暗くなる。0 なら灰を出さない。
    private final double maxOpacity;
    // フェードにかかる時間（秒）。見た目の調整用。
    private final double fadeInSeconds;
    private final double fadeOutSeconds;

    private double opacity = 0;

    public VolcanicAsh(Color ashColor, double maxOpacity,
            double fadeInSeconds, double fadeOutSeconds) {
        this.ashColor = ashColor;
        this.maxOpacity = Math.clamp(maxOpacity, 0, 1);
        this.fadeInSeconds = Math.max(0.01, fadeInSeconds);
        this.fadeOutSeconds = Math.max(0.01, fadeOutSeconds);
    }

    /** 噴火中は濃くし、噴火が終わったら徐々に薄くする。 */
    public void update(boolean erupting) {
        if (erupting) {
            double amount = maxOpacity
                    * Game.STEP / fadeInSeconds;

            opacity = Math.min(maxOpacity, opacity + amount);
        } else {
            double amount = maxOpacity
                    * Game.STEP / fadeOutSeconds;

            opacity = Math.max(0, opacity - amount);
        }
    }

    /** プレイヤーや花の上に、半透明の火山灰を重ねる。 */
    public void render(GraphicsContext gc) {
        if (opacity <= 0) {
            return;
        }

        gc.save();

        gc.setFill(Color.color(
                ashColor.getRed(),
                ashColor.getGreen(),
                ashColor.getBlue(),
                opacity));

        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        gc.restore();
    }
}