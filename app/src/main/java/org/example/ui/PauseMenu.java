package org.example.ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import org.example.core.Game;
import org.example.core.Input;
import org.example.media.Assets;
import org.example.media.Sound;
import org.example.ui.options.OptionPanel;

/**
 * 一時停止メニュー。ゲーム画面の上に、暗い幕と選択肢を重ねて描く。
 *
 * <pre>
 * 操作: W/S（↑/↓）で選ぶ ／ ENTER で決定 ／ ESC で再開
 * 「オプション」を選ぶと、ゲームを止めたままオプションのパネル（OptionPanel）を重ねて出す。ESC でこのメニューに戻る
 * </pre>
 */
public final class PauseMenu {

    public enum Action { NONE, RESUME, RETRY, STAGE_SELECT }

    private static final String[] LABELS = {"再開", "オプション", "リトライ", "リタイア"};
    // オプションは GameScene に知らせず、このメニューの中でパネルを開く（NONE を返す）
    private static final Action[] ACTIONS = {Action.RESUME, Action.NONE, Action.RETRY, Action.STAGE_SELECT};
    
    // 画像（images/UI_Pause_*.png）。どれも 1080x1080 の中に絵があり、まわりは透明なので、絵のある所だけを切り出して描く
    private static final String PANEL_KEY = "UI_Pause_BG";
    private static final String[] BUTTON_KEYS = {"UI_Pause_Back", "UI_Pause_Option", "UI_Pause_Retry", "UI_Pause_Retire"};
    /** 画像の中の、絵のある範囲（左・上・幅・高さ。画像の px）。パネルは「〜ポーズ〜」の文字入り */
    private static final double[] PANEL_SRC = {172, 181, 736, 718};
    private static final double[] BUTTON_SRC = {308, 486, 464, 108};
    /** 画像を画面に描く倍率 */
    private static final double IMAGE_SCALE = 1.2;
    private static final int OPTION_INDEX = 1;
    private static final int COUNT = LABELS.length;

    private static final Color INK = Color.web("#2b2d42");
    private static final Color BORDER_GRAY = Color.web("#888888"); // ボタンのグレー枠
    private static final Color RED_CURSOR = Color.web("#ff0000");  // 赤いカーソル枠

    private static final double PANEL_W = PANEL_SRC[2] * IMAGE_SCALE;
    private static final double PANEL_H = PANEL_SRC[3] * IMAGE_SCALE;
    private static final double BUTTON_W = BUTTON_SRC[2] * IMAGE_SCALE;
    private static final double BUTTON_H = BUTTON_SRC[3] * IMAGE_SCALE;
    /** 1つ目のボタンの真ん中の高さ（パネルの上から）と、ボタンの間隔 */
    private static final double BUTTON_TOP = 255;
    private static final double BUTTON_SPACING = 150;

    private boolean open;
    private int selected;
    private double time;

    /** 「オプション」を選んだときに重ねて出すパネル */
    private final OptionPanel optionPanel = new OptionPanel();

    private Image panelImage;
    private final Image[] buttonImage = new Image[COUNT];

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        selected = 0;
        time = 0;

        panelImage = Assets.image(PANEL_KEY);
        for (int i = 0; i < COUNT; i++) {
            buttonImage[i] = Assets.image(BUTTON_KEYS[i]);
            // カーソルを完全に独立させたため、_sel 画像の読み込みは廃止しました
        }
    }

    public void close() {
        open = false;
        optionPanel.close();
    }

    public Action update() {
        if (!open) return Action.NONE;
        time += Game.STEP;

        // オプションを開いている間は、オプションの操作だけを受け付ける（ESC でこのメニューに戻る）
        if (optionPanel.isOpen()) {
            optionPanel.update();
            return Action.NONE;
        }

        if (Input.isPressed(KeyCode.ESCAPE)) return Action.RESUME;

        if (Input.isPressed(KeyCode.W) || Input.isPressed(KeyCode.UP)) {
            selected = (selected + COUNT - 1) % COUNT;
            Sound.se("SE_Cursor");
        }
        if (Input.isPressed(KeyCode.S) || Input.isPressed(KeyCode.DOWN)) {
            selected = (selected + 1) % COUNT;
            Sound.se("SE_Cursor");
        }

        if (Input.isPressed(KeyCode.ENTER)) {
            Sound.se("SE_Decide");
            if (selected == OPTION_INDEX) optionPanel.open();
            return ACTIONS[selected];
        }
        return Action.NONE;
    }

    public void render(GraphicsContext gc) {
        if (!open) return;

        double w = Game.WIDTH;
        double h = Game.HEIGHT;
        double px = (w - PANEL_W) / 2;
        double py = (h - PANEL_H) / 2;

        gc.save();

        // 背景の暗幕
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, 0, w, h);

        // オプションを開いている間は、メニューの代わりにオプションのパネルを出す
        if (optionPanel.isOpen()) {
            gc.restore();
            optionPanel.render(gc);
            return;
        }

        // パネル背景
        if (panelImage != null) {
            // 画像に「〜ポーズ〜」の見出しが入っているので、文字は描かない
            drawCropped(gc, panelImage, PANEL_SRC, px, py, PANEL_W, PANEL_H);
        } else {
            gc.setFill(Color.web("#ffffff", 0.92));
            gc.fillRect(px, py, PANEL_W, PANEL_H); // ここも四角に変更しました
            gc.setStroke(BORDER_GRAY);
            gc.setLineWidth(8.6);
            gc.strokeRect(px, py, PANEL_W, PANEL_H);
            text(gc, "一時停止", w / 2, py + 104, 69, INK);
        }

        // 1. まず全てのボタンを「通常状態」で描画する
        for (int i = 0; i < COUNT; i++) {
            drawButton(gc, i, w / 2, py + BUTTON_TOP + i * BUTTON_SPACING);
        }

        // 2. 選択されているボタンの位置に、赤いカーソル枠を重ねて描画する
        drawCursor(gc, w / 2, py + BUTTON_TOP + selected * BUTTON_SPACING);

        gc.restore();
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    /** ボタン本体の描画（選択状態はここでは考慮せず、常に同じ見た目で描画） */
    private void drawButton(GraphicsContext gc, int index, double cx, double cy) {
        gc.save();
        gc.translate(cx, cy);

        Image image = buttonImage[index];

        if (image != null) {
            // 画像がある場合
            drawCropped(gc, image, BUTTON_SRC, -BUTTON_W / 2, -BUTTON_H / 2, BUTTON_W, BUTTON_H);
        } else {
            // 画像がない場合の代替図形ボタン（白背景＋グレー枠）
            gc.setFill(Color.web("#ffffff"));
            gc.fillRect(-BUTTON_W / 2, -BUTTON_H / 2, BUTTON_W, BUTTON_H);
            
            gc.setStroke(BORDER_GRAY);
            gc.setLineWidth(4.3);
            gc.strokeRect(-BUTTON_W / 2, -BUTTON_H / 2, BUTTON_W, BUTTON_H);

            text(gc, LABELS[index], 0, 0, 52, INK);
        }

        gc.restore();
    }

    /** 選択中のボタンを囲う赤いカーソル枠の描画 */
    private void drawCursor(GraphicsContext gc, double cx, double cy) {
        gc.save();
        gc.translate(cx, cy);

        gc.setStroke(RED_CURSOR);
        gc.setLineWidth(6.5);
        
        // ボタンより少し大きいサイズで枠を描画（余白）
        double padding = 13; 
        double cw = BUTTON_W + padding * 2;
        double ch = BUTTON_H + padding * 2;
        
        gc.strokeRect(-cw / 2, -ch / 2, cw, ch);

        gc.restore();
    }

    /** 画像の src（左・上・幅・高さ）の範囲だけを、(x, y) から w x h の大きさで描く。 */
    private static void drawCropped(GraphicsContext gc, Image image, double[] src, double x, double y, double w, double h) {
        gc.drawImage(image, src[0], src[1], src[2], src[3], x, y, w, h);
    }

    private void text(GraphicsContext gc, String s, double x, double y, double size, Color color) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Assets.font("main", size));
        gc.setFill(color);
        gc.fillText(s, x, y);
    }
}