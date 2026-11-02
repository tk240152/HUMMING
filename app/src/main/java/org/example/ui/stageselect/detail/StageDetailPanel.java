package org.example.ui.stageselect.detail;

import javafx.scene.layout.Pane;

import org.example.core.Game;
import org.example.state.Records;
import org.example.ui.stageselect.StageInfo;
import org.example.ui.stageselect.editor.StageDetailEditor;

/** 詳細の開閉と情報の読み込みを担当し、表示と編集は各部品へ任せる。 */
public final class StageDetailPanel extends Pane {

    private final StageDetailView view = new StageDetailView();
    private final StageDetailEditor editor = new StageDetailEditor(
            view, view.layoutModules(), view.editorBanner());

    public StageDetailPanel() {
        setMinSize(Game.WIDTH, Game.HEIGHT);
        setPrefSize(Game.WIDTH, Game.HEIGHT);
        setMaxSize(Game.WIDTH, Game.HEIGHT);
        setPickOnBounds(true);
        setFocusTraversable(true);
        setOnMousePressed(event -> requestFocus());
        getChildren().add(view);
        setVisible(false);
    }

    public void open(int stageNumber) {
        StageInfo info = StageInfo.load(stageNumber);
        boolean hasBest = Records.hasBest(stageNumber);
        double bestSeconds = hasBest ? Records.best(stageNumber) : 0;
        view.display(stageNumber, info, hasBest, bestSeconds);
        editor.setEditing(false);
        setVisible(true);
        toFront();
        requestFocus();
    }

    public void close() {
        editor.setEditing(false);
        setVisible(false);
    }

    public boolean isLayoutEditing() {
        return editor.isEditing();
    }

    public void toggleLayoutEditing() {
        editor.setEditing(!editor.isEditing());
    }

    public void updateEditor() {
        editor.update();
    }
}
