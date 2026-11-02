package org.example.core;

import java.util.HashSet;
import java.util.Set;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

/**
 * キー入力。
 * isDown : 押している間ずっと true（移動など）
 * isPressed : 押した瞬間の1ステップだけ true（決定・切り替えなど）
 */
public final class Input {

    private static final Set<KeyCode> down = new HashSet<>();
    private static final Set<KeyCode> pressed = new HashSet<>();

    /** true の間は、どのキーも押されていないことにする（クリア演出中など） */
    private static boolean blocked;

    private Input() {
    }

    /** true にすると、false に戻すまでキー入力を受け付けない。 */
    public static void setBlocked(boolean value) {
        blocked = value;
        pressed.clear();
    }

    /** App から1回だけ呼ぶ。 */
    public static void attach(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (down.add(e.getCode()))
                pressed.add(e.getCode());
        });
        scene.setOnKeyReleased(e -> down.remove(e.getCode()));
    }

    public static boolean isDown(KeyCode key) {
        return !blocked && down.contains(key);
    }

    public static boolean isPressed(KeyCode key) {
        return !blocked && pressed.contains(key);
    }

    /** Game が毎ステップの最後に呼ぶ。 */
    static void endStep() {
        pressed.clear();
    }
}
