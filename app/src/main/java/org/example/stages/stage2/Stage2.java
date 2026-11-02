package org.example.stages.stage2;

import org.example.game.stage.StageScript;
import org.example.game.event.EventTypes;

/**
 * ステージ2「とっぷう」。
 * このステージだけで使えるイベント "tailwind"（突風のくり返し）を登録する。
 * 中身は TailwindEvent、設定は stages/stage2/stage.json の events に書く。
 */
public class Stage2 implements StageScript {

    @Override
    public void registerEvents(EventTypes types) {
        types.register("tailwind", TailwindEvent::new);
    }
}