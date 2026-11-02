package org.example.ui.title;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.core.Input;
import org.example.media.Assets;
import org.example.media.Sound;

/**
 * 「遊び方」を選んだときに、タイトルの上に重ねて出す説明の画像。
 *
 * <pre>
 * 操作: ENTER か ESC で閉じる
 * </pre>
 */
public final class HowToPanel {

    private static final String IMAGE = "Tex_Title_HowTo_Window";
    /** 画像そのままの大きさ（1920x1080 の画面の px）。画面の真ん中に置く */
    private static final double PANEL_W = 1391, PANEL_H = 783;

    private boolean open;

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
    }

    /** 1ステップ分の操作を受け付ける。ENTER か ESC で閉じる。 */
    public void update() {
        if (!open)
            return;
        if (Input.isPressed(KeyCode.ENTER) || Input.isPressed(KeyCode.ESCAPE)) {
            Sound.se("SE_Cancel");
            close();
        }
    }

    public void render(GraphicsContext gc) {
        if (!open)
            return;
        Assets.draw(gc, IMAGE,
                (Game.WIDTH - PANEL_W) / 2, (Game.HEIGHT - PANEL_H) / 2,
                PANEL_W, PANEL_H, Color.LIGHTGRAY);
    }
}
