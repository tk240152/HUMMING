package org.example.ui.stageselect;

import java.net.URL;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import org.example.core.Game;
import org.example.media.Assets;
import org.example.media.Sound;
import org.example.ui.KeyGuide;
import org.example.ui.stageselect.detail.StageDetailPanel;

/** 花・選択枠・詳細パネルの配置と表示を担当する。 */
public final class StageSelectView extends Pane {

    private static final double[] FLOWER_X = {216, 540, 912, 1320, 1704};
    private static final double[] STEM_LENGTHS = {389, 648, 432, 670, 432};
    private static final double SELECTION_SIZE = 285;
    /** 選択枠を花の中心からずらす量。画像の中で花が真ん中にないもの（ステージ2・3・5）だけ、見た目の真ん中に合わせる */
    private static final double[] FRAME_OFFSET_X = {0, 0, 0, 0, 11};
    private static final double[] FRAME_OFFSET_Y = {0, -10, 12, 0, -8};
    /** 左上の見出し（images/selectText.png）の位置と横幅。高さは画像の縦横比で決まる */
    private static final double HEADING_X = 40, HEADING_Y = 30, HEADING_WIDTH = 600;

    private final FlowerView[] flowers = new FlowerView[FLOWER_X.length];
    private final Rectangle selectionFrame =
            new Rectangle(SELECTION_SIZE, SELECTION_SIZE);
    private final StageDetailPanel detail = new StageDetailPanel();
    private int selectedIndex;
    private boolean entering = true;
    // 選んでいる花の脈打つ拡大（リザルトの選んでいるボタンと同じ動きで、大きさだけ大きめ: 1.05〜1.15 倍）
    private static final double PULSE_BASE = 1.10, PULSE_AMOUNT = 0.05, PULSE_SPEED = 6.0;
    private double pulseTime;

    public StageSelectView() {
        setMinSize(Game.WIDTH, Game.HEIGHT);
        setPrefSize(Game.WIDTH, Game.HEIGHT);
        setMaxSize(Game.WIDTH, Game.HEIGHT);
        setFocusTraversable(true);
        getStyleClass().add("stage-select");

        ImageView heading = new ImageView(Assets.image("selectText"));
        heading.setFitWidth(HEADING_WIDTH);
        heading.setPreserveRatio(true);
        heading.setLayoutX(HEADING_X);
        heading.setLayoutY(HEADING_Y);
        heading.setMouseTransparent(true);
        getChildren().add(heading);

        for (int i = 0; i < flowers.length; i++) {
            final int index = i;
            FlowerView flower = new FlowerView(i + 1, STEM_LENGTHS[i]);
            flower.setLayoutX(FLOWER_X[i]);
            flower.setLayoutY(Game.HEIGHT - STEM_LENGTHS[i]);
            flower.getStyleClass().add("stage-flower");
            flower.setOnMouseClicked(event -> {
                if (entering || detail.isVisible()) {
                    return;
                }
                select(index);
                requestFocus();
            });
            flowers[i] = flower;
            getChildren().add(flower);
        }

        selectionFrame.getStyleClass().add("selection-frame");
        selectionFrame.setMouseTransparent(true);
        getChildren().add(selectionFrame);

        URL css = getClass().getResource("/styles/stage-select.css");
        if (css == null) {
            throw new IllegalStateException("styles/stage-select.css が見つかりません。");
        }
        getStylesheets().add(css.toExternalForm());
        select(0);
        getChildren().add(new KeyGuide().createNode()); // 右下のキー案内（詳細パネルより下に置く）
        getChildren().add(detail);
        selectionFrame.visibleProperty().bind(detail.visibleProperty().not());
    }

    /** 1ステップ分、選んでいる花を脈打たせる。リザルトの選んでいるボタンと同じ速さ（sin(6t)）で、1.10 を中心に ±0.05。 */
    public void updatePulse() {
        pulseTime += Game.STEP;
        flowers[selectedIndex].setHeadScale(PULSE_BASE + PULSE_AMOUNT * Math.sin(pulseTime * PULSE_SPEED));
    }

    public void selectRelative(int offset) {
        select(selectedIndex + offset);
    }

    public int selectedStage() {
        return selectedIndex + 1;
    }

    public StageDetailPanel detail() {
        return detail;
    }

    public void applyEntrance(StageSelectEntrance entrance) {
        entering = !entrance.isFinished();
        setOpacity(entrance.opacity());
        selectionFrame.setOpacity(entering ? 0 : 1);
        for (FlowerView flower : flowers) {
            flower.setGrowth(entrance.flowerGrowth());
        }
    }

    private void select(int index) {
        int nextIndex = Math.floorMod(index, flowers.length);
        // ★ 選択インデックスが変わったときだけカーソル移動音を再生し、カーソルが来た花を軽く揺らす
        boolean moved = nextIndex != selectedIndex;
        if (moved) {
            Sound.se("SE_Cursor");
        }
        // 前に選んでいた花は元の大きさに戻す（今の花は updatePulse で脈打つ）
        flowers[selectedIndex].setHeadScale(1);
        selectedIndex = nextIndex;
        FlowerView flower = flowers[selectedIndex];
        if (moved) {
            flower.wiggle();
        }
        selectionFrame.setLayoutX(flower.getLayoutX() - SELECTION_SIZE / 2 + FRAME_OFFSET_X[selectedIndex]);
        selectionFrame.setLayoutY(flower.getLayoutY() - SELECTION_SIZE / 2 + FRAME_OFFSET_Y[selectedIndex]);
        // 揺れている間も、選択枠が花についていくようにする
        selectionFrame.translateXProperty().bind(flower.headShiftX());
        // 選択枠も花と一緒に脈打つ（リザルトの赤い枠と同じ）
        selectionFrame.scaleXProperty().bind(flower.headScale());
        selectionFrame.scaleYProperty().bind(flower.headScale());
    }
}