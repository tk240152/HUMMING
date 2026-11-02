package org.example.game.event;

import org.example.state.GameState;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.flower.Wind;
import org.example.player.Player;

/**
 * イベントが触ってよいもの一式。イベントはここから風・花・プレイヤーなどを操作する。
 * 新しい種類のイベントで別のものを触りたくなったら、ここに足す。
 */
public class EventContext {

    public final GameState state;
    /** プレイヤー（テストでは null のことがある） */
    public final Player player;
    public final Flower flower;
    /** 花の設定。イベントで一時的に書き換えてよい（終わったら元に戻すこと） */
    public final FlowerConfig flowerConfig;
    public final Wind wind;

    /** ステージ開始からの経過時間（秒） */
    public double time;

    private boolean feedingBlocked;

    public EventContext(GameState state, Player player, Flower flower, FlowerConfig flowerConfig, Wind wind) {
        this.state = state;
        this.player = player;
        this.flower = flower;
        this.flowerConfig = flowerConfig;
        this.wind = wind;
    }

    /** このステップは蜜を吸えなくする（吸蜜を邪魔するエネミーなどが使う）。毎ステップ呼ぶ必要がある。 */
    public void blockFeeding() {
        feedingBlocked = true;
    }

    public boolean isFeedingBlocked() {
        return feedingBlocked;
    }

    /** 満腹度の割合（0〜100）。 */
    public double nectarPercent() {
        return state.maxNectar <= 0 ? 0 : state.nectar / state.maxNectar * 100;
    }

    /** StageEvents が毎ステップの最初に呼ぶ。 */
    void beginStep(double time) {
        this.time = time;
        this.feedingBlocked = false;
    }
}
