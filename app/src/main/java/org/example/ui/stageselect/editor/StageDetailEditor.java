package org.example.ui.stageselect.editor;

import java.io.IOException;
import java.util.Map;

import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import org.example.core.Input;

/** 詳細画面の編集モード、共有レイアウトの読込・コピー、操作案内を担当する。 */
public final class StageDetailEditor {

    private final LayoutEditor layoutEditor;
    private final Map<String, Region> modules;
    private final Label banner;
    private boolean editing;
    private String layoutStatus = "";

    public StageDetailEditor(Pane owner, Map<String, Region> modules, Label banner) {
        this.modules = modules;
        this.banner = banner;
        layoutEditor = new LayoutEditor(owner);
        modules.forEach(layoutEditor::register);

        // 開くたびには読み直さず、編集中の配置を保持する。
        try {
            layoutStatus = TeamLayout.load(modules) ? "設定読込済み" : "初期レイアウト";
        } catch (IOException | IllegalArgumentException e) {
            layoutStatus = "設定読込失敗";
            System.err.println("共通レイアウトを読み込めませんでした: " + e.getMessage());
        }
        setEditing(false);
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        layoutEditor.setEnabled(editing);
        updateBanner();
        banner.setVisible(editing);
        banner.toFront();
    }

    public void update() {
        if (!editing) {
            return;
        }
        layoutEditor.update();
        if (Input.isDown(KeyCode.CONTROL) && Input.isPressed(KeyCode.S)) {
            copyLayout();
        }
        updateBanner();
    }

    private void copyLayout() {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(TeamLayout.exportLayout(modules));
            layoutStatus = Clipboard.getSystemClipboard().setContent(content)
                    ? "コピーしました" : "コピー失敗";
        } catch (IllegalStateException | SecurityException e) {
            layoutStatus = "コピー失敗";
            System.err.println("レイアウトをコピーできませんでした: " + e.getMessage());
        }
    }

    private void updateBanner() {
        String text = "編集中 | F2：終了 | Ctrl+S：コピー | G："
                + (layoutEditor.isGridEnabled() ? "ON" : "OFF")
                + " | " + layoutStatus
                + "\nShift：吸着なし | H：水平中央 | V：垂直中央";
        if (!text.equals(banner.getText())) {
            banner.setText(text);
        }
    }
}
