package org.example.stages.stage3;

import org.example.game.stage.StageScript;
import org.example.game.event.EventTypes;

/**
 * ステージ3「花の丘」だけの処理。
 * 設定（花・イベント・BGM・背景・ランクの基準）は resources/stages/stage3/stage.json に書く。
 *
 * ステージ3のファイル:
 * - resources/stages/stage3/stage.json               … イベント（通常10秒 → 渡り鳥3秒のくり返し）など
 * - resources/images/kari_wataridori.png              … 渡り鳥の画像（仮）
 * - stages/stage3/Stage3.java                        … このクラス。渡り鳥を "birds" という名前で登録する
 * - stages/stage3/MigratingBirdsEvent.java           … 渡り鳥の中身（群れが右端から左端へ横切る）
 */
public class Stage3 implements StageScript {

    @Override
    public void registerEvents(EventTypes types) {
        // stage.json の events で "type": "birds" と書けるようにする（ステージ3でだけ使える）
        types.register("birds", MigratingBirdsEvent::new);
    }
}
