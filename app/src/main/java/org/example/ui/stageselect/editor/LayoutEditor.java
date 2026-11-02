package org.example.ui.stageselect.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

import org.example.core.Input;

/**
 * 詳細パネル内のレイアウトを調整する編集機能。
 *
 * ・マウスによる選択と移動
 * ・グリッドへの吸着
 * ・ほかのモジュールとの位置合わせ
 * ・画面中央への配置
 *
 * 位置はメモリ内に保持する。
 * チーム共通レイアウトの読込・出力はStageDetailEditorとTeamLayoutが担当する。
 */
public final class LayoutEditor {

    // グリッドの間隔。単位はゲーム画面内のpx。
    private static final double GRID_STEP = 20;

    // この距離以内まで近づいたらガイドに吸着する。
    private static final double SNAP_DISTANCE = 9;

    // 座標の比較に使用する小さな誤差。
    private static final double EPSILON = 0.001;

    private final Pane owner;

    // 登録名は、後でレイアウトを保存するときの識別にも使う。
    private final Map<String, Region> modules = new LinkedHashMap<>();

    // 編集用の図形をまとめるレイヤー。
    private final Pane overlay = new Pane();

    private final Path grid = new Path();
    private final Line verticalGuide = new Line();
    private final Line horizontalGuide = new Line();
    private final Rectangle selectionFrame = new Rectangle();

    // ガイドが画面の外へ描画されないようにする。
    private final Rectangle overlayClip = new Rectangle();

    private boolean enabled;
    private boolean gridEnabled = true;

    private Region selected;
    private Region dragging;

    // モジュール左上から、マウスを押した位置までの距離。
    private double dragOffsetX;
    private double dragOffsetY;

    /**
     * 吸着後の座標とガイド位置。
     * guideがNaNの場合は、ガイドに吸着していない。
     */
    private record Snap(double position, double guide) {
    }

    public LayoutEditor(Pane owner) {
        this.owner = owner;

        // 編集用レイヤーは自動レイアウトの対象にしない。
        overlay.setManaged(false);

        // ガイドや選択枠の下にあるモジュールを操作できるようにする。
        overlay.setMouseTransparent(true);
        overlay.setVisible(false);
        overlay.setClip(overlayClip);

        // 色や線の太さはCSSで管理する。
        grid.getStyleClass().add("layout-grid");
        verticalGuide.getStyleClass().add("layout-alignment-guide");
        horizontalGuide.getStyleClass().add("layout-alignment-guide");
        selectionFrame.getStyleClass().add("layout-selection-frame");

        verticalGuide.setVisible(false);
        horizontalGuide.setVisible(false);
        selectionFrame.setVisible(false);

        overlay.getChildren().addAll(
                grid,
                verticalGuide,
                horizontalGuide,
                selectionFrame
        );

        owner.getChildren().add(overlay);

        // 最初のレイアウトや画面サイズの変更に合わせて更新する。
        owner.widthProperty().addListener(
                (observable, before, after) -> refreshOverlay());

        owner.heightProperty().addListener(
                (observable, before, after) -> refreshOverlay());

        refreshOverlay();

        // 空白部分をクリックした場合は選択を解除する。
        // モジュール上のクリックは、各モジュール側で処理する。
        owner.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (enabled && event.getButton() == MouseButton.PRIMARY) {
                selected = null;
                dragging = null;

                selectionFrame.setVisible(false);
                hideGuides();
                owner.requestFocus();
            }
        });
    }

    /**
     * 編集対象を登録する。
     * モジュールはownerの直接の子として追加しておく。
     */
    public void register(String id, Region module) {
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException(
                    "モジュール名が重複しています: " + id);
        }

        if (module.getParent() != owner) {
            throw new IllegalArgumentException(
                    "モジュールは詳細パネルの直接の子にしてください: " + id);
        }

        modules.put(id, module);

        // モジュール内の空白部分でも選択できるようにする。
        module.setPickOnBounds(true);

        module.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!enabled || event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            selected = module;
            dragging = module;

            // マウス座標を詳細パネル内の座標へ変換する。
            Point2D point = owner.sceneToLocal(
                    event.getSceneX(), event.getSceneY());

            // 押した位置が急に左上へ移動しないよう、差分を保存する。
            dragOffsetX = point.getX() - module.getLayoutX();
            dragOffsetY = point.getY() - module.getLayoutY();

            owner.requestFocus();
            hideGuides();
            updateSelectionFrame();
            overlay.toFront();

            // 親パネルの空白クリック処理へ伝えない。
            event.consume();
        });

        module.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!enabled || dragging != module
                    || !event.isPrimaryButtonDown()) {
                return;
            }

            Point2D point = owner.sceneToLocal(
                    event.getSceneX(), event.getSceneY());

            double width = moduleWidth(module);
            double height = moduleHeight(module);

            double maxX = Math.max(0, canvasWidth() - width);
            double maxY = Math.max(0, canvasHeight() - height);

            double x = clamp(point.getX() - dragOffsetX, maxX);
            double y = clamp(point.getY() - dragOffsetY, maxY);

            hideGuides();

            // Shiftを押している間は、すべての吸着を一時停止する。
            // 画面外への移動制限は引き続き有効にする。
            if (!event.isShiftDown()) {
                Snap snapX = findSnap(module, x, width, true);
                Snap snapY = findSnap(module, y, height, false);

                // ガイドへの吸着を優先する。
                // ガイドがない方向だけグリッドに合わせる。
                x = Double.isNaN(snapX.guide())
                        ? snapToGrid(x)
                        : snapX.position();

                y = Double.isNaN(snapY.guide())
                        ? snapToGrid(y)
                        : snapY.position();

                x = clamp(x, maxX);
                y = clamp(y, maxY);

                if (!Double.isNaN(snapX.guide())
                        && Math.abs(x - snapX.position()) < EPSILON) {
                    showVerticalGuide(snapX.guide());
                }

                if (!Double.isNaN(snapY.guide())
                        && Math.abs(y - snapY.position()) < EPSILON) {
                    showHorizontalGuide(snapY.guide());
                }
            }

            module.relocate(x, y);
            updateSelectionFrame();
            event.consume();
        });

        module.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (dragging != module
                    || event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            dragging = null;
            hideGuides();
            event.consume();
        });
    }

    /** 編集モードを切り替える。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        selected = null;
        dragging = null;

        selectionFrame.setVisible(false);
        hideGuides();
        refreshOverlay();

        overlay.setVisible(enabled);
        overlay.toFront();

        for (Region module : modules.values()) {
            module.setCursor(enabled ? Cursor.MOVE : null);
        }
    }

    public boolean isGridEnabled() {
        return gridEnabled;
    }

    /** 詳細パネルのupdateから、毎ステップ呼び出す。 */
    public void update() {
        // ドラッグ中はキーボードによる位置変更を行わない。
        if (!enabled || dragging != null) {
            return;
        }

        // Gでグリッド表示とグリッド吸着を同時に切り替える。
        // モジュール間のガイド吸着はそのまま使用できる。
        if (Input.isPressed(KeyCode.G)) {
            gridEnabled = !gridEnabled;
            grid.setVisible(gridEnabled);
            hideGuides();
        }

        if (selected == null) {
            return;
        }

        // Hで画面の左右中央に配置する。
        // 正確な中央になるよう、グリッドへの丸めは行わない。
        if (Input.isPressed(KeyCode.H)) {
            selected.relocate(
                    Math.max(
                            0,
                            (canvasWidth() - moduleWidth(selected)) / 2),
                    selected.getLayoutY()
            );

            hideGuides();
            showVerticalGuide(canvasWidth() / 2);
        }

        // Vで画面の上下中央に配置する。
        if (Input.isPressed(KeyCode.V)) {
            selected.relocate(
                    selected.getLayoutX(),
                    Math.max(
                            0,
                            (canvasHeight() - moduleHeight(selected)) / 2)
            );

            // HとVを同時に押した場合は両方の中心線を表示する。
            if (!Input.isPressed(KeyCode.H)) {
                hideGuides();
            }

            showHorizontalGuide(canvasHeight() / 2);
        }

        updateSelectionFrame();
    }

    /**
     * 画面やほかのモジュールから、最も近い吸着先を探す。
     * horizontalがtrueなら横方向、falseなら縦方向を調べる。
     */
    private Snap findSnap(
            Region module, double raw, double size, boolean horizontal) {

        double extent = horizontal ? canvasWidth() : canvasHeight();
        double maximum = Math.max(0, extent - size);

        Snap best = new Snap(raw, Double.NaN);

        // 画面の左・中央・右、または上・中央・下への吸着。
        for (int anchor = 0; anchor < 3; anchor++) {
            double guide = extent * anchor / 2;

            best = chooseSnap(
                    best,
                    raw,
                    guide - size * anchor / 2,
                    guide,
                    maximum
            );
        }

        for (Region other : modules.values()) {
            if (other == module || !other.isVisible()) {
                continue;
            }

            double origin = horizontal
                    ? other.getLayoutX()
                    : other.getLayoutY();

            double otherSize = horizontal
                    ? moduleWidth(other)
                    : moduleHeight(other);

            for (int target = 0; target < 3; target++) {
                double guide = origin + otherSize * target / 2;

                for (int anchor = 0; anchor < 3; anchor++) {
                    // 中心同士、または端同士を合わせる。
                    // モジュールの中心を別のモジュールの端には合わせない。
                    if ((target == 1) != (anchor == 1)) {
                        continue;
                    }

                    best = chooseSnap(
                            best,
                            raw,
                            guide - size * anchor / 2,
                            guide,
                            maximum
                    );
                }
            }
        }

        return best;
    }

    /** 吸着候補が現在の候補より近ければ採用する。 */
    private Snap chooseSnap(
            Snap best,
            double raw,
            double position,
            double guide,
            double maximum) {

        double distance = Math.abs(position - raw);

        if (position < 0
                || position > maximum
                || distance > SNAP_DISTANCE) {
            return best;
        }

        if (Double.isNaN(best.guide())
                || distance
                < Math.abs(best.position() - raw) - EPSILON) {
            return new Snap(position, guide);
        }

        return best;
    }

    /** グリッドが有効な場合だけ、最も近い交点に丸める。 */
    private double snapToGrid(double position) {
        return gridEnabled
                ? Math.round(position / GRID_STEP) * GRID_STEP
                : position;
    }

    /** 画面サイズに合わせて編集用レイヤーを更新する。 */
    private void refreshOverlay() {
        double width = canvasWidth();
        double height = canvasHeight();

        overlay.resize(width, height);
        overlayClip.setWidth(width);
        overlayClip.setHeight(height);

        grid.getElements().clear();

        for (double x = GRID_STEP; x < width; x += GRID_STEP) {
            grid.getElements().addAll(
                    new MoveTo(x, 0),
                    new LineTo(x, height)
            );
        }

        for (double y = GRID_STEP; y < height; y += GRID_STEP) {
            grid.getElements().addAll(
                    new MoveTo(0, y),
                    new LineTo(width, y)
            );
        }

        grid.setVisible(gridEnabled);
        updateSelectionFrame();
    }

    /** 選択枠を選択中のモジュールに合わせる。 */
    private void updateSelectionFrame() {
        if (!enabled || selected == null) {
            selectionFrame.setVisible(false);
            return;
        }

        selectionFrame.setX(selected.getLayoutX());
        selectionFrame.setY(selected.getLayoutY());
        selectionFrame.setWidth(moduleWidth(selected));
        selectionFrame.setHeight(moduleHeight(selected));
        selectionFrame.setVisible(true);
    }

    private void showVerticalGuide(double x) {
        verticalGuide.setStartX(x);
        verticalGuide.setEndX(x);
        verticalGuide.setStartY(0);
        verticalGuide.setEndY(canvasHeight());
        verticalGuide.setVisible(true);
    }

    private void showHorizontalGuide(double y) {
        horizontalGuide.setStartX(0);
        horizontalGuide.setEndX(canvasWidth());
        horizontalGuide.setStartY(y);
        horizontalGuide.setEndY(y);
        horizontalGuide.setVisible(true);
    }

    private void hideGuides() {
        verticalGuide.setVisible(false);
        horizontalGuide.setVisible(false);
    }

    // 初回レイアウト前は実寸が0のため、推奨サイズを使用する。
    private double canvasWidth() {
        return owner.getWidth() > 0
                ? owner.getWidth()
                : owner.getPrefWidth();
    }

    private double canvasHeight() {
        return owner.getHeight() > 0
                ? owner.getHeight()
                : owner.getPrefHeight();
    }

    private static double moduleWidth(Region module) {
        return module.getWidth() > 0
                ? module.getWidth()
                : module.prefWidth(-1);
    }

    private static double moduleHeight(Region module) {
        return module.getHeight() > 0
                ? module.getHeight()
                : module.prefHeight(-1);
    }

    /** 座標を0からmaximumまでの範囲に収める。 */
    private static double clamp(double value, double maximum) {
        return Math.max(0, Math.min(value, maximum));
    }
}
