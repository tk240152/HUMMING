package org.example.ui.options;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.core.Input;
import org.example.media.Assets;
import org.example.media.Sound;

/**
 * オプションのパネル（サウンドの音量と、操作の説明）。いまの画面の上に重ねて描く部品。
 * ポーズメニュー（ゲームは止めたまま）とタイトル画面から開く。後ろを暗くするのは開く側が行う。
 *
 * <pre>
 * 操作: W/S（↑/↓）で BGM / SE の選択切替 ／ A/D（←/→）で音量調整 ／ ESC か ENTER で閉じる
 * </pre>
 */
public final class OptionPanel {

    /** A/D を押している間に音量が変わる速さ（1秒あたり） */
    private static final double VOLUME_SPEED = 0.8;
    /** 音量を変えている間、試しの音を鳴らす間隔（秒） */
    private static final double PREVIEW_INTERVAL = 0.2;

    /** パネルの画像（画面全体の大きさ。見出し・ラベル・操作説明は画像に描いてある） */
    private static final String BG_KEY = "UI_Option_BG";

    private static final Color SLIDER_ON = Color.web("#ffb000");
    private static final Color SLIDER_OFF = Color.web("#787474");
    private static final Color SELECT_BORDER = Color.web("#ff5500"); // 選択中ハイライト色

    // スライダーの位置（画面の座標。画像の「BGM 音量」「SE 音量」の文字の右に並べる）
    private static final double BGM_Y = 358;
    private static final double SE_Y = 520;
    private static final double SLIDER_X = 690, SLIDER_W = 880, SLIDER_H = 36, KNOB_R = 38;

    /** 0: BGM調整, 1: SE調整 */
    private int selectedIndex = 0;

    private boolean open;
    /** 次に試しの音を鳴らすまでの残り秒数 */
    private double previewWait;

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        selectedIndex = 0; // 開いたときはBGMにフォーカス
    }

    public void close() {
        open = false;
    }

    /** 1ステップ分の操作を受け付ける。ESC か ENTER で閉じる。 */
    public void update() {
        if (!open) return;

        if (Input.isPressed(KeyCode.ESCAPE) || Input.isPressed(KeyCode.ENTER)) {
            Sound.se("SE_Cancel");
            close();
            return;
        }

        // W/S または ↑/↓ で BGM / SE の選択切り替え
        if (Input.isPressed(KeyCode.W) || Input.isPressed(KeyCode.UP)) {
            if (selectedIndex > 0) {
                selectedIndex--;
                Sound.se("SE_Cursor");
            }
        }
        if (Input.isPressed(KeyCode.S) || Input.isPressed(KeyCode.DOWN)) {
            if (selectedIndex < 1) {
                selectedIndex++;
                Sound.se("SE_Cursor");
            }
        }

        // A（←）で下げる、D（→）で上げる
        boolean down = Input.isDown(KeyCode.A) || Input.isDown(KeyCode.LEFT);
        boolean up = Input.isDown(KeyCode.D) || Input.isDown(KeyCode.RIGHT);
        int direction = (up ? 1 : 0) - (down ? 1 : 0);
        if (direction == 0) {
            previewWait = 0;
            return;
        }

        if (selectedIndex == 0) {
            // --- BGM 音量調整 ---
            double before = Sound.getBgmVolume();
            double v = Math.clamp(before + direction * VOLUME_SPEED * Game.STEP, 0, 1);
            if (v != before) {
                Sound.setBgmVolume(v);
            }
        } else {
            // --- SE 音量調整 ---
            double before = Sound.getSeVolume();
            double v = Math.clamp(before + direction * VOLUME_SPEED * Game.STEP, 0, 1);
            if (v != before) {
                Sound.setSeVolume(v);

                // SE変更中のみ、音量確認のサンプル効果音を鳴らす
                previewWait -= Game.STEP;
                if (previewWait <= 0) {
                    Sound.se("SE_Cursor");
                    previewWait = PREVIEW_INTERVAL;
                }
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (!open) return;

        gc.save();

        // パネルの画像（見出し・ラベル・操作説明）
        Assets.draw(gc, BG_KEY, 0, 0, Game.WIDTH, Game.HEIGHT, Color.web("#c9c9c9"));

        drawSlider(gc, BGM_Y, SLIDER_X, Sound.getBgmVolume(), selectedIndex == 0);
        drawSlider(gc, SE_Y, SLIDER_X, Sound.getSeVolume(), selectedIndex == 1);

        gc.restore();
    }

    /** スライダー1つを描画するヘルパーメソッド */
    private void drawSlider(GraphicsContext gc, double sy, double sx, double volume, boolean selected) {
        double knobX = sx + SLIDER_W * volume;

        // バーの背景
        gc.setFill(SLIDER_OFF);
        gc.fillRoundRect(sx, sy - SLIDER_H / 2, SLIDER_W, SLIDER_H, SLIDER_H, SLIDER_H);

        // アクティブ部分（オレンジ）
        gc.setFill(SLIDER_ON);
        gc.fillRoundRect(sx, sy - SLIDER_H / 2, knobX - sx, SLIDER_H, SLIDER_H, SLIDER_H);

        // つまみ（円）
        gc.fillOval(knobX - KNOB_R, sy - KNOB_R, KNOB_R * 2, KNOB_R * 2);
        
        // 選択中ならつまみの枠線を太く強調（橙色）、非選択なら薄い黄色
        gc.setStroke(selected ? SELECT_BORDER : Color.web("#e09a00"));
        gc.setLineWidth(selected ? 6 : 3);
        gc.strokeOval(knobX - KNOB_R, sy - KNOB_R, KNOB_R * 2, KNOB_R * 2);
    }
}