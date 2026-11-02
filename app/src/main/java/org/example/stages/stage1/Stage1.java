package org.example.stages.stage1;

import org.example.game.stage.StageScript;
import org.example.game.event.EventTypes;

/**
 * ステージ1「そよ風の平原」だけの処理（新しいステージを作るときの見本）。
 *
 * 設定（花の揺れ・そよ風のタイミングや強さなど）は resources/stages/stage1/stage.json に書いてある。
 * ここでは「設定だけでは作れないもの」として、ステージ1だけのイベント「そよ風」を登録している。
 *
 * ステージ1のファイル:
 * - resources/stages/stage1/stage.json … 名前・説明・花（4秒で1往復の揺れ）・イベント（通常5秒 →
 * そよ風3秒のくり返し）
 * - stages/stage1/Stage1.java … このクラス。そよ風を "breeze" という名前で登録する
 * - stages/stage1/BreezeEvent.java … そよ風の中身（花を向かい風に押す・白い「～」の線を描く）
 */
public class Stage1 implements StageScript {

    @Override
    public void registerEvents(EventTypes types) {
        // stage.json の events で "type": "breeze" と書けるようにする（ステージ1でだけ使える）
        types.register("breeze", BreezeEvent::new);
    }
}
