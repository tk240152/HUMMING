package org.example.ui.stageselect;

import java.net.URL;

import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/** ステージ選択で使う花の画像。 */
public class FlowerView extends Group {

    // 花の画像の表示サイズと、茎の太さ。
    private static final double HEAD_SIZE = 320;
    private static final double STEM_WIDTH = 32;
    private final Scale growthScale;

    // カーソルが来たときの揺れ。根元は動かさず、茎をしならせて花を左右へ WIGGLE_PX だけ動かし、
    // WIGGLE_TIMES 往復しながら小さくなって止まる
    private static final double WIGGLE_PX = 22;
    private static final double WIGGLE_TIMES = 2;
    private static final double WIGGLE_SECONDS = 1.2;
    /** 茎のしなり方。根元から茎の長さのこの割合までは、まっすぐ上に伸びてから曲がる（大きいほど先だけ曲がる） */
    private static final double STEM_STIFF = 0.5;
    /** 花の傾き。花が横に 1px ずれるごとに傾ける角度（度） */
    private static final double HEAD_TILT_PER_PX = 0.25;

    /** 花（茎の先）が横にずれている量（px） */
    private final DoubleProperty bend = new SimpleDoubleProperty(0);
    private final Transition wiggle;
    /** 花の画像の拡大率。選ばれている間だけ StageSelectView が脈打たせる（選ばれていないときは 1） */
    private final DoubleProperty pulseScale = new SimpleDoubleProperty(1);

    // ステージ番号順の花の画像。
    private static final String[] HEAD_FILES = {
            "petunia.png",
            "anemone.png",
            "trumpetvine.png",
            "cornflower.png",
            "golden_columbine.png"
    };

    private static final String HEAD_FOLDER = "/images/";

    /**
     * @param stageNumber ステージ番号（1〜5）
     * @param stemLength  花の中心から根元までの表示上の長さ
     */
    public FlowerView(int stageNumber, double stemLength) {
        String path = HEAD_FOLDER + HEAD_FILES[stageNumber - 1];

        URL resource = FlowerView.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException(
                    "花の画像が見つかりません: " + path);
        }

        Image image = new Image(resource.toExternalForm());

        // 花の中心から、地面まで茎を伸ばす。根元は固定し、先（花）だけが横に動く曲線にする（しなる茎）。
        QuadCurve stem = new QuadCurve(
                0, stemLength,                   // 根元
                0, stemLength * (1 - STEM_STIFF), // 曲がり方を決める点（根元の真上）
                0, 0);                           // 先（花の中心）
        stem.endXProperty().bind(bend);
        stem.setFill(null);
        stem.setStroke(Color.web("#62A346"));
        stem.setStrokeWidth(STEM_WIDTH);
        stem.setStrokeLineCap(StrokeLineCap.BUTT);

        // 花の画像の中心を、このグループの原点に合わせる。揺れの間は茎の先についていき、少し傾く。
        ImageView head = new ImageView(image);
        head.setPreserveRatio(true);
        head.setFitWidth(HEAD_SIZE);
        head.setFitHeight(HEAD_SIZE);
        head.setSmooth(true);
        head.setX(-HEAD_SIZE / 2);
        head.setY(-HEAD_SIZE / 2);
        head.translateXProperty().bind(bend);
        head.rotateProperty().bind(bend.multiply(HEAD_TILT_PER_PX));
        head.scaleXProperty().bind(pulseScale);
        head.scaleYProperty().bind(pulseScale);

        // 茎を先に置き、花の画像を手前に重ねる。
        getChildren().addAll(stem, head);

        // 根元を固定して、花全体を縦方向に伸ばす。
        growthScale = new Scale(1, 1, 0, stemLength);
        getTransforms().add(growthScale);

        wiggle = new Transition() {
            {
                setCycleDuration(Duration.seconds(WIGGLE_SECONDS));
            }

            @Override
            protected void interpolate(double frac) {
                // 往復しながらだんだん小さくなり、最後はまっすぐに戻る
                double fade = (1 - frac) * (1 - frac);
                bend.set(WIGGLE_PX * fade * Math.sin(2 * Math.PI * WIGGLE_TIMES * frac));
            }
        };
    }

    /** 花の画像の拡大率を設定する（茎の太さ・長さは変えない）。 */
    public void setHeadScale(double scale) {
        pulseScale.set(scale);
    }

    /** 花の画像の今の拡大率。選択枠を花と一緒に脈打たせるのに使う。 */
    public ReadOnlyDoubleProperty headScale() {
        return pulseScale;
    }

    /** 成長率を設定する。0で隠れ、1で元の高さになる。 */
    public void setGrowth(double progress) {
        growthScale.setY(progress);
    }

    /** 茎をしならせて「ゆらゆらっ」と軽く揺れて止まる（カーソルが来たとき）。 */
    public void wiggle() {
        wiggle.playFromStart();
    }

    /** 揺れで花が横にずれている量（px）。選択枠を花についていかせるのに使う。 */
    public ReadOnlyDoubleProperty headShiftX() {
        return bend;
    }
}
