package org.example.game.event;

import java.util.Map;

import javafx.scene.canvas.GraphicsContext;

/**
 * 新しいイベントを作るときのひな型（コピー用。どのステージにも登録されていない）。
 *
 * 使い方:
 * 1. このファイルを自分のステージのフォルダ（java/org/example/stages/stageN/）にコピーする
 * 2. package を org.example.stages.stageN に、クラス名を好きな名前（例: EnemyEvent）に変える
 *    （コピー先では import org.example.game.event.EventContext; と import org.example.game.event.StageEvent; を足す）
 * 3. 下の「ここを書く」を埋める。使わないメソッドは消してよい
 * 4. StageN.registerEvents で登録する:  types.register("enemy", EnemyEvent::new);
 * 5. stage.json の events に書く:        { "type": "enemy", "when": { "rest": 5 }, "duration": 3, "speed": 200 }
 *
 * 触れるもの（ctx から）: ctx.flower（花）・ctx.flowerConfig（花の設定）・ctx.wind（風）・ctx.player（プレイヤー）・
 * ctx.state（満腹度など）・ctx.time（ステージ開始からの秒数）・ctx.blockFeeding()（このステップは蜜を吸えなくする）
 *
 * 使える道具（親クラス StageEvent）: number("名前", 初期値)・string("名前", 初期値)（JSON の値を読む）、
 * activeTime()（始まってからの秒数）、duration（続く秒数）、fade(秒)（始まり・終わりを滑らかにする 0〜1 の値）
 */
public class EventTemplate extends StageEvent {

    // ここを書く: JSON から読む数値（stage.json で調整できるようにする）
    private final double speed;

    public EventTemplate(Map<String, Object> params) {
        super(params);
        speed = number("speed", 100); // stage.json に "speed" が無ければ 100
    }

    @Override
    protected void onStart(EventContext ctx) {
        // ここを書く: 始まったときに1回（準備・位置の初期化など）
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        // ここを書く: 続いている間、毎ステップ（1/60秒）。動かす・判定する
    }

    @Override
    protected void onEnd(EventContext ctx) {
        // ここを書く: 終わったときに1回。onStart・onUpdate で変えたものを元に戻す
    }

    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        // ここを書く: 続いている間、毎フレーム描く（見た目が無いイベントなら消してよい）
    }
}
