package org.example.ui.stageselect.detail;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.state.Records;
import org.example.ui.stageselect.StageInfo;

/** 詳細の部品配置、画像、文字の表示を担当する。編集操作や画面遷移は扱わない。 */
public final class StageDetailView extends Pane {

    private final Label stageNameLabel = new Label();
    private final ImageView stageNameImage = new ImageView();
    private final HBox stars = new HBox(19);
    private final Label description = new Label();
    private final ImageView rankImage = new ImageView();
    private final Label bestTimeLabel = new Label("--.--");
    private final StackPane previewModule = new StackPane();
    private final ImageView previewImage = new ImageView();

    private final Label previewPlaceholder = new Label("ステージ画像\n準備中");

    private final Label editModeLabel = new Label();

    /** ステージごとの詳細ウィンドウの画像と、それが無いときに使う共通の背景。開始案内はどのステージでも同じボタンの画像 */
    private final ImageView windowImage = new ImageView();
    private final Pane shade = new Pane();
    private final Pane startGuide = new Pane();

    // 編集と共有レイアウトで使う登録名。順番も保存する。
    private final Map<String, Region> layoutModules = new LinkedHashMap<>();

    public StageDetailView() {
        // 選択画面全体を覆う大きさにする。
        setMinSize(Game.WIDTH, Game.HEIGHT);
        setPrefSize(Game.WIDTH, Game.HEIGHT);
        setMaxSize(Game.WIDTH, Game.HEIGHT);

        getStyleClass().add("stage-detail-overlay");

        // 透明な部分でも背後の花をクリックさせない。
        setPickOnBounds(true);
        setFocusTraversable(true);
        setOnMousePressed(event -> requestFocus());

        // ステージごとの詳細ウィンドウの画像（Tex_SelectWin_StageNN_BG。ステージ名・難易度の星・説明まで描いてある）。
        // 画像があるステージでは、下の共通の背景と、画像と重なる部品（ステージ名・星）を隠す。
        windowImage.setFitWidth(Game.WIDTH);
        windowImage.setFitHeight(Game.HEIGHT);
        windowImage.setSmooth(true);
        windowImage.setMouseTransparent(true);

        // 上部を残し、中央から下に半透明の背景を重ねる（ステージごとの画像が無いとき）。
        shade.getStyleClass().add("detail-shade");
        place(shade, 0, 205, Game.WIDTH, Game.HEIGHT - 231);

        // ① ステージ名。
        stageNameLabel.getStyleClass().add("detail-stage-name");
        place(stageNameLabel, 612, 125, 696, 130);
        stageNameImage.setPreserveRatio(true);
        stageNameImage.setSmooth(true);
        stageNameImage.setFitWidth(696);
        stageNameImage.setFitHeight(130);

        stageNameLabel.setGraphic(stageNameImage);

        // ② ステージ画像。
        previewModule.getStyleClass().add("detail-preview");
        place(previewModule, 11, 324, 966, 582); // ステージごとの画像の「ステージ写真」の枠の内側

        // 元の縦横比を保ち、表示領域に収まる大きさにする。
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);
        previewImage.fitWidthProperty().bind(
                previewModule.widthProperty());
        previewImage.fitHeightProperty().bind(
                previewModule.heightProperty());

        previewPlaceholder.getStyleClass().add(
                "detail-preview-placeholder");

        previewModule.getChildren().addAll(
                previewImage, previewPlaceholder);

        // ③ ベストタイム。
        // 保存されたベストタイムとランクを表示する。
        Pane recordModule = new Pane();
        place(recordModule, 1008, 270, 888, 335);

        Label recordTitle = new Label("ベストタイム");
        recordTitle.getStyleClass().add("detail-record-title");
        place(recordTitle, 0, 0, 888, 78);

        // 表示する画像は、詳細を開くときに設定する。
        rankImage.setPreserveRatio(true);
        rankImage.setFitWidth(170);
        rankImage.setFitHeight(170);
        rankImage.setSmooth(true);

        StackPane rank = new StackPane(rankImage);
        rank.getStyleClass().add("detail-rank");
        place(rank, 0, 123, 170, 170); // 下の「難易度」の文字にリボンがかからない大きさ

        bestTimeLabel.getStyleClass().add("detail-time");
        place(bestTimeLabel, 264, 91, 624, 216);
        // タイトル・ランク・時間をひとつの移動単位にする。
        recordModule.getChildren().addAll(
                recordTitle, rank, bestTimeLabel);
        // ④ 難易度。
        Pane difficultyModule = new Pane();
        place(difficultyModule, 1008, 626, 888, 130);

        Label difficultyTitle = new Label("難易度：");
        difficultyTitle.getStyleClass().add(
                "detail-difficulty-title");
        place(difficultyTitle, 0, 0, 324, 130);

        stars.getStyleClass().add("detail-stars");
        place(stars, 336, 0, 552, 130);
        stars.setAlignment(Pos.CENTER);

        // ラベルと星をひとつの移動単位にする。
        difficultyModule.getChildren().addAll(
                difficultyTitle, stars);

        // ⑤ ステージ説明。
        description.getStyleClass().add("detail-description");
        description.setWrapText(true);
        place(description, 1008, 767, 888, 140);

        // 背景画像に含まれている文字は重ねて表示しない。
        recordTitle.setVisible(false);
        difficultyTitle.setVisible(false);
        description.setVisible(false);

        // ⑥ 開始案内。
        // Enterキーの判定はStageSelectSceneが担当する。
        startGuide.getStyleClass().add("detail-start-guide");
        // ボタンの画像（Tex_SelectWin_Start。645x108、影の分も含む）を、ボタンの本体が横の真ん中に来るように、画面の下端にそろえて置く
        place(startGuide, (Game.WIDTH - 645) / 2.0, Game.HEIGHT - 108, 645, 108);

        // 編集中だけ表示する二行の操作案内。
        editModeLabel.getStyleClass().add("layout-edit-banner");
        place(editModeLabel, 29, 13, Game.WIDTH - 58, 99);
        editModeLabel.setMouseTransparent(true);
        editModeLabel.setVisible(false);

        // ⑦ ステージ名の左右の三角。A/D（←/→）で前・次のステージに切り替えられることを示す。
        Polygon prevArrow = arrow(-1);
        Polygon nextArrow = arrow(1);
        // ステージごとの画像のステージ名の帯（横 468〜1450、縦の真ん中 115）の左右に置く
        prevArrow.relocate(NAME_BAND_LEFT - ARROW_GAP - ARROW_W, NAME_BAND_CENTER_Y - ARROW_H / 2);
        nextArrow.relocate(NAME_BAND_RIGHT + ARROW_GAP, NAME_BAND_CENTER_Y - ARROW_H / 2);
        bob(prevArrow, -1);
        bob(nextArrow, 1);

        // 先に追加した部品ほど奥に表示される。
        getChildren().addAll(
                shade,
                windowImage,
                prevArrow,
                nextArrow,
                stageNameLabel,
                previewModule,
                recordModule,
                difficultyModule,
                description,
                startGuide,
                editModeLabel);


        registerModule("stageName", stageNameLabel);
        registerModule("preview", previewModule);
        registerModule("record", recordModule);
        registerModule("difficulty", difficultyModule);
        registerModule("description", description);
        registerModule("start", startGuide);


        URL css = getClass().getResource("/styles/stage-detail.css");

        if (css == null) {
            throw new IllegalStateException(
                    "styles/stage-detail.css が見つかりません。");
        }

        getStylesheets().add(css.toExternalForm());

    }

    // ステージ名の左右の三角の大きさ・ステージ名との間・外へ動く幅（px）と、1往復の秒数
    private static final double ARROW_W = 66, ARROW_H = 90, ARROW_GAP = 36;
    private static final double ARROW_BOB = 12, ARROW_BOB_SECONDS = 0.9;
    /** ステージごとの画像の、ステージ名の帯の左端・右端・縦の真ん中（px） */
    private static final double NAME_BAND_LEFT = 468, NAME_BAND_RIGHT = 1450, NAME_BAND_CENTER_Y = 115;

    /** 横向きの三角を作る。direction が -1 なら左向き、1 なら右向き。 */
    private static Polygon arrow(int direction) {
        Polygon p = direction < 0
                ? new Polygon(ARROW_W, 0, ARROW_W, ARROW_H, 0, ARROW_H / 2)
                : new Polygon(0, 0, 0, ARROW_H, ARROW_W, ARROW_H / 2);
        p.setFill(Color.web("#ffb000"));
        p.setStroke(Color.web("#5a3a00"));
        p.setStrokeWidth(5);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.setMouseTransparent(true);
        return p;
    }

    /** 三角を、向いている方へ小さく行ったり来たりさせる（押せそうに見せる）。 */
    private static void bob(Polygon arrow, int direction) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(ARROW_BOB_SECONDS / 2), arrow);
        t.setFromX(0);
        t.setToX(direction * ARROW_BOB);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
    }

    /** 部品の位置と固定サイズを設定する。 */
    private static void place(
            Region node,
            double x,
            double y,
            double width,
            double height) {

        node.setMinSize(width, height);
        node.setPrefSize(width, height);
        node.setMaxSize(width, height);
        node.relocate(x, y);
    }

    /** ステージ番号に対応する詳細画像を読み込む。 */
    private Image loadDetailImage(int stageNumber, String part) {
        String path = String.format(
                "/images/Tex_SelectWin_Stage%02d_%s.png",
                stageNumber,
                part);

        URL resource = getClass().getResource(path);

        return resource == null
                ? null
                : new Image(resource.toExternalForm());
    }

    /** 難易度に応じた数の星を表示する。 */
    private void updateStars(int difficulty) {
        URL resource = getClass().getResource(
                "/images/Tex_SelectWin_Star.png");

        if (resource == null) {
            throw new IllegalStateException(
                    "難易度の星画像が見つかりません。");
        }

        Image image = new Image(resource.toExternalForm());

        // 前に表示したステージの星を取り除く。
        stars.getChildren().clear();

        for (int i = 0; i < difficulty; i++) {
            ImageView star = new ImageView(image);

            // 元画像の左端にある星1個だけを表示する。
            star.setViewport(new Rectangle2D(0, 0, 110, 109));
            star.setPreserveRatio(true);
            star.setFitWidth(69);
            star.setFitHeight(69);
            star.setSmooth(true);

            stars.getChildren().add(star);
        }
    }

    /** 選択されたステージのベストタイムとランクを表示する。ランクはそのステージの基準（rankTimes）で決める。 */
    private void updateRecord(boolean hasBest, double bestSeconds, double[] rankTimes) {
        if (!hasBest) {
            bestTimeLabel.setText("--.--");
            rankImage.setImage(null);
            return;
        }

        bestTimeLabel.setText(Records.format(bestSeconds));

        String rankName = GameState.rankFor(bestSeconds, rankTimes);
        URL resource = getClass().getResource(
                "/images/result_rank_"
                        + rankName + ".png");

        if (resource == null) {
            throw new IllegalStateException(
                    "ランク画像が見つかりません: " + rankName);
        }

        rankImage.setImage(
                new Image(resource.toExternalForm()));
    }

    /** 読み込まれたステージ情報と記録を、各部品に反映する。 */
    public void display(int stageNumber, StageInfo info, boolean hasBest, double bestSeconds) {

        // ステージごとの詳細ウィンドウの画像があれば、それを使い、画像に描いてある部品は重ねない
        Image window = loadDetailImage(stageNumber, "BG");
        boolean hasWindow = window != null;
        windowImage.setImage(window);
        shade.setVisible(!hasWindow);
        stageNameLabel.setVisible(!hasWindow);
        stars.setVisible(!hasWindow);

        // ステージ名：画像がなければ文字で表示する。
        Image nameImage = loadDetailImage(stageNumber, "Name");
        stageNameImage.setImage(nameImage);

        if (nameImage != null) {
            stageNameLabel.setText(null);
            stageNameLabel.setStyle(
                    "-fx-background-color: transparent;");
        } else {
            stageNameLabel.setText(
                    "ステージ" + stageNumber + "：" + info.name());
            stageNameLabel.setStyle("");
        }

        description.setText(info.description());
        updateStars(info.difficulty());

        // プレビュー：画像がなければ「準備中」を表示する。
        Image image = loadDetailImage(stageNumber, "Image");
        previewImage.setImage(image);
        previewPlaceholder.setVisible(image == null);

        updateRecord(hasBest, bestSeconds, info.rankTimes());
    }

    public Map<String, Region> layoutModules() {
        return Collections.unmodifiableMap(layoutModules);
    }

    public Label editorBanner() {
        return editModeLabel;
    }

    private void registerModule(String id, Region module) {
        layoutModules.put(id, module);
    }
}
