package org.example.stages.stage4;

import org.example.game.event.EventTypes;
import org.example.game.stage.StageScript;

/**
 * ステージ4「夕暮れの庭」。
 * このステージで使えるイベント "eruption"（火山の噴火）を登録する。
 * 中身は EruptionEvent、設定は stages/stage4/stage.json の events に書く。
 *
 * BGM は stage.json の bgm で決める（sounds/bgm/ に置いた名前を書く）。
 */
public class Stage4 implements StageScript {

    @Override
    public void registerEvents(EventTypes types) {
        types.register("eruption", EruptionEvent::new);
    }
}
