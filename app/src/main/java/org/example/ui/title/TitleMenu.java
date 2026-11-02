package org.example.ui.title;

import javafx.scene.input.KeyCode;

import org.example.core.Input;
import org.example.media.Sound;

/** W・Sでの選択と、Enterでの決定だけを担当する。ESC で「終了」へカーソルを移し、「終了」の上でもう一度 ESC なら決定と同じ。 */
public final class TitleMenu {

    public enum Action { NONE, START, HOW_TO, OPTION, EXIT }

    private static final int ITEM_COUNT = 4;
    /** 「終了」の項目の番号（ESC でここへカーソルを飛ばす） */
    private static final int EXIT_INDEX = 3;
    private int selectedIndex;

    public void reset() {
        selectedIndex = 0;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    /** 操作可能なときだけTitleSceneから呼び出す。 */
    public Action update() {
        // ★ W / UP（上キー）でカーソル移動
        if (Input.isPressed(KeyCode.W) || Input.isPressed(KeyCode.UP)) {
            selectedIndex = Math.floorMod(selectedIndex - 1, ITEM_COUNT);
            Sound.se("SE_Cursor"); // ★ カーソル移動音
        } 
        // ★ S / DOWN（下キー）でカーソル移動
        else if (Input.isPressed(KeyCode.S) || Input.isPressed(KeyCode.DOWN)) {
            selectedIndex = (selectedIndex + 1) % ITEM_COUNT;
            Sound.se("SE_Cursor"); // ★ カーソル移動音
        }
        // ESC: 「終了」以外にカーソルがあれば「終了」へ一気に移す。すでに「終了」なら EXIT を返す（TitleScene が終了の確認を出す）
        else if (Input.isPressed(KeyCode.ESCAPE)) {
            if (selectedIndex == EXIT_INDEX) {
                Sound.se("SE_Decide");
                return Action.EXIT;
            }
            selectedIndex = EXIT_INDEX;
            Sound.se("SE_Cursor");
        }

        // ENTER キーが押されていない場合は何も返さない
        if (!Input.isPressed(KeyCode.ENTER)) {
            return Action.NONE;
        }

        // ★ ENTER 押下時は決定音を鳴らす
        Sound.se("SE_Decide");

        return switch (selectedIndex) {
            case 0 -> Action.START;
            case 1 -> Action.HOW_TO;
            case 2 -> Action.OPTION;
            case 3 -> Action.EXIT;
            default -> throw new IllegalStateException("選択項目の範囲外です。");
        };
    }
}