package org.example.game.stage;

import javafx.scene.canvas.GraphicsContext;

import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;

/**
 * そのステージだけの特別な処理。org.example.stages.stage{番号}.Stage{番号} というクラスを作ると自動で使われる
 * （どこにも登録しなくてよい。無ければ何もしない）。
 * 必要なメソッドだけ書けばよい。風・花・プレイヤー・GameState は引数の EventContext から触る。
 *
 * <pre>
 * package org.example.stages.stage3;
 * public class Stage3 implements StageScript {
 *     public void registerEvents(EventTypes types) { types.register("enemy", EnemyEvent::new); }
 *     public void update(EventContext ctx) { ... }
 * }
 * </pre>
 */
public interface StageScript {

    /** このステージだけのイベントを登録する（stage.json の events で "type" に書ける名前が増える）。 */
    default void registerEvents(EventTypes types) {}

    /** ステージが始まったときに1回。 */
    default void onStart(EventContext ctx) {}

    /** 毎ステップ（1/60秒）。プレイヤーとイベントのあと、花より前に呼ばれる。 */
    default void update(EventContext ctx) {}

    /** 背景のすぐ上（花・プレイヤーより下）に描く。 */
    default void renderBack(GraphicsContext gc, EventContext ctx) {}

    /** 花・プレイヤー・イベントより上（UI より下）に描く。 */
    default void renderFront(GraphicsContext gc, EventContext ctx) {}

    /** ステージを抜けるときに1回（変えたものを元に戻す）。 */
    default void onExit(EventContext ctx) {}

    /** 何もしない処理（StageN クラスが無いステージで使う）。 */
    StageScript NONE = new StageScript() {};

    /** org.example.stages.stage{n}.Stage{n} を探して作る。無ければ NONE。 */
    static StageScript load(int stage) {
        String name = "org.example.stages.stage" + stage + ".Stage" + stage;
        try {
            Class<?> type = Class.forName(name);
            if (!StageScript.class.isAssignableFrom(type)) {
                System.err.println("[StageScript] " + name + " が StageScript を実装していません");
                return NONE;
            }
            return (StageScript) type.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            return NONE;
        } catch (ReflectiveOperationException e) {
            System.err.println("[StageScript] " + name + " を作れません（引数なしの public コンストラクタが必要）: " + e);
            return NONE;
        }
    }
}
