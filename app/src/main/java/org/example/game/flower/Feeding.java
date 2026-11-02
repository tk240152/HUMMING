package org.example.game.flower;

import javafx.geometry.Point2D;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.state.GameState.Status;
import org.example.media.Sound;

/**
 * 吸蜜の判定。くちばしの先が花の入り口に重なっている間、蜜がたまる。
 * 結果は GameState（nectar・distance・status）に書き込む。
 */
public class Feeding {

    /** 吸っている間に「パッ」（feed_tick）を鳴らす間隔（秒）。小さくすると「ぱぱぱぱ」が速くなる */
    static final double TICK_INTERVAL = 0.1;

    private boolean blocked;
    /** 次に feed_tick を鳴らすまでの残り秒数 */
    private double tickWait;

    /** true の間は、くちばしが重なっていても蜜を吸えない（エネミーなどのイベントが使う）。 */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    /**
     * @param guideDistance この距離より近づいたら APPROACH にする（Target の外側のリングが出る距離と同じ）
     */
    public void update(Point2D beakTip, Flower flower, double nectarRate, double guideDistance, GameState state) {
        state.distance = beakTip.distance(flower.x, flower.y);

        if (state.status == Status.CLEAR) return;

        Status before = state.status;
        if (!blocked && flower.contains(beakTip.getX(), beakTip.getY())) {
            state.nectar = Math.min(state.maxNectar, state.nectar + nectarRate);
            state.status = state.nectar >= state.maxNectar ? Status.CLEAR : Status.FEEDING;
        } else if (state.distance < guideDistance) {
            state.status = Status.APPROACH;
        } else {
            state.status = Status.FLYING;
        }

        if (before != Status.FEEDING && state.status == Status.FEEDING) {
            tickWait = 0; // 吸い始めたらすぐ1回目を鳴らす
        }
        // 吸っている間は TICK_INTERVAL 秒ごとに鳴らし続ける（ぱぱぱぱぱっ）
        if (state.status == Status.FEEDING) {
            tickWait -= Game.STEP;
            if (tickWait <= 0) {
                Sound.se("feed_tick");
                tickWait += TICK_INTERVAL;
            }
        }
    }
}
