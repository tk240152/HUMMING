package org.example.stages.stage5;

import org.example.game.stage.StageScript;
import org.example.game.event.EventTypes;
import org.example.stages.stage2.TailwindEvent;

/**
 * ステージ5「嵐の向こう」の処理。
 * 落雷（lightning）と突風（tailwind）の両方を登録する。
 */
public class Stage5 implements StageScript {

    @Override
    public void registerEvents(EventTypes types) {
        // 雨イベントの登録
        types.register("rain", RainEvent::new);
        // 落雷イベントの登録
        types.register("lightning", LightningEvent::new);
        
        // 突風イベントの登録
        types.register("tailwind", TailwindEvent::new);
        // ランダム落雷イベントの登録
        types.register("randomLightning", RandomLightningEvent::new);
    }
}