package org.example.game.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import org.example.state.GameState;
import org.example.core.Input;
import org.example.media.Sound;

/**
 * GameState の中身を文字で表示する開発用の画面（F3 で切り替え）。
 * 表示中は音量も操作できる: 1/2 全体、3/4 BGM、5/6 効果音（下げる/上げる）、M ミュート。
 */
public class DebugOverlay {

    private static final double VOLUME_STEP = 0.1;

    private boolean visible;

    public void toggle() {
        visible = !visible;
    }

    public void update() {
        if (!visible) return;

        if (Input.isPressed(KeyCode.DIGIT1)) Sound.setMasterVolume(Sound.getMasterVolume() - VOLUME_STEP);
        if (Input.isPressed(KeyCode.DIGIT2)) Sound.setMasterVolume(Sound.getMasterVolume() + VOLUME_STEP);
        if (Input.isPressed(KeyCode.DIGIT3)) Sound.setBgmVolume(Sound.getBgmVolume() - VOLUME_STEP);
        if (Input.isPressed(KeyCode.DIGIT4)) Sound.setBgmVolume(Sound.getBgmVolume() + VOLUME_STEP);
        if (Input.isPressed(KeyCode.DIGIT5) || Input.isPressed(KeyCode.DIGIT6)) {
            double d = Input.isPressed(KeyCode.DIGIT5) ? -VOLUME_STEP : VOLUME_STEP;
            Sound.setSeVolume(Sound.getSeVolume() + d);
            Sound.se("debug"); // 変えた音量を試しに鳴らす
        }
        if (Input.isPressed(KeyCode.M)) Sound.setMuted(!Sound.isMuted());
    }

    public void render(GraphicsContext gc, GameState s) {
        if (!visible) return;

        String[] lines = {
            String.format("bird     : (%.1f, %.1f)", s.birdX, s.birdY),
            String.format("velocity : (%.2f, %.2f)", s.birdVx, s.birdVy),
            String.format("nectar   : %.1f / %.0f", s.nectar, s.maxNectar),
            String.format("distance : %.1f", s.distance),
            String.format("relSpeed : %.2f", s.relSpeed),
            "status   : " + s.status,
            String.format("volume   : all %.1f  bgm %.1f  se %.1f%s",
                    Sound.getMasterVolume(), Sound.getBgmVolume(), Sound.getSeVolume(),
                    Sound.isMuted() ? "  MUTE" : ""),
            "  [1/2] all  [3/4] bgm  [5/6] se  [M] mute",
        };

        gc.setFill(Color.color(0, 0, 0, 0.6));
        // 左上の満腹度ゲージと重ならないよう、その下に出す
        gc.fillRect(17, 86, 778, 35 * lines.length + 26);
        gc.setFill(Color.LIME);
        gc.setFont(Font.font("Monospaced", 28));
        for (int i = 0; i < lines.length; i++) {
            gc.fillText(lines[i], 35, 125 + i * 35);
        }
    }
}
