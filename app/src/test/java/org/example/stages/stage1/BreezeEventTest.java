package org.example.stages.stage1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.game.util.Easing;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.Json;
import org.example.game.flower.Wind;
import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;
import org.example.game.event.StageEvents;
import org.junit.jupiter.api.Test;

/**
 * ステージ1の揺れとそよ風（通常5秒 → 3秒の向かい風をくり返す）。
 * 花は茎のばねで動き、そよ風は花を押す力として当たる。
 */
class BreezeEventTest {

    private final FlowerConfig config = new FlowerConfig();
    private final Flower flower;
    private final EventContext ctx;
    private final StageEvents events;

    private double time;
    private double min, max, maxStep;

    BreezeEventTest() {
        config.swayWidth = 100;
        config.swayPeriod = 4; // 1往復4秒の一定のリズム
        flower = new Flower(config);
        ctx = new EventContext(new GameState(), null, flower, config, new Wind());
        EventTypes types = EventTypes.common();
        new Stage1().registerEvents(types); // ゲームと同じように、ステージ1のイベントを登録する
        events = StageEvents.create(Json.array(Json.parseObject("""
                {"events": [{"type": "breeze", "when": {"rest": 5}, "duration": 3,
                             "strength": 2, "rampSeconds": 0.5}]}
                """), "events"), types);
    }

    /** seconds 秒ぶん進めて、その間の位置の最小・最大と、1ステップで動いた最大量を記録する。 */
    private void run(double seconds) {
        min = Double.MAX_VALUE;
        max = -Double.MAX_VALUE;
        maxStep = 0;
        double prev = flower.swayRatio();
        for (int i = 0; i < Math.round(seconds / Game.STEP); i++) {
            events.update(ctx, time);
            flower.update(0);
            time += Game.STEP;
            double r = flower.swayRatio();
            min = Math.min(min, r);
            max = Math.max(max, r);
            maxStep = Math.max(maxStep, Math.abs(r - prev));
            prev = r;
        }
    }

    @Test
    void normalSwayIsRegularWithFourSecondRoundTrip() {
        run(4.0);  // 最初から1往復ぶん
        assertEquals(1.0, max, 0.03);    // 右へ通常の揺れ幅
        assertEquals(-1.0, min, 0.03);   // 左へ通常の揺れ幅
    }

    @Test
    void breezeBendsToAboutTwiceTowardThePlayer() {
        run(5.0);
        run(3.0);  // 5〜8秒: そよ風
        // プレイヤーがいない（テスト）ときは左へ。通常の揺れに重なるので、だいたい −2 を中心に揺れる
        assertTrue(min < -1.8, "あまり倒れていない: " + min);
        assertTrue(min > -3.5, "倒れすぎ: " + min);
    }

    @Test
    void movesSmoothlyWithoutJumps() {
        run(30.0);  // そよ風を何回かはさむ
        // 1ステップ（1/60秒）で通常の揺れ幅の 15% 以上動かない＝位置が飛ばない
        assertTrue(maxStep < 0.15, "位置が飛んだ: " + maxStep);
    }

    @Test
    void returnsToNormalSwayAfterBreeze() {
        run(8.0);
        run(2.5);  // 反動の揺れが落ち着く
        run(2.4);  // 10.5〜12.9秒（次のそよ風の前）
        assertTrue(max < 1.3 && min > -1.3, "通常の揺れに戻っていない: " + min + " 〜 " + max);
    }

    @Test
    void windStrengthRampsUpAndDown() {
        BreezeEvent breeze = new BreezeEvent(Map.of("type", "breeze", "duration", 3.0, "rampSeconds", 0.5));
        assertEquals(0.0, breeze.envelope(0.0), 1e-9);   // 吹き始めは 0
        assertEquals(1.0, breeze.envelope(0.5), 1e-9);   // 0.5秒で最大
        assertEquals(1.0, breeze.envelope(2.0), 1e-9);
        assertEquals(0.0, breeze.envelope(3.0), 1e-9);   // やむときに 0
    }

    @Test
    void windLineIsATildeShape() {
        BreezeEvent breeze = new BreezeEvent(Map.of("type", "breeze", "lineLength", 80.0, "waveHeight", 10.0));
        breeze.wave(100, 200);
        int last = breeze.pointX.length - 1;
        assertEquals(100, breeze.pointX[0], 1e-9);           // 始まり
        assertEquals(180, breeze.pointX[last], 1e-9);        // 長さ 80
        assertEquals(200, breeze.pointY[0], 1e-9);
        assertEquals(190, breeze.pointY[last / 4], 1e-9);    // 1/4 で山（上へ 10）
        assertEquals(210, breeze.pointY[last * 3 / 4], 1e-9); // 3/4 で谷（下へ 10）
        assertEquals(200, breeze.pointY[last], 1e-9);        // 終わり
    }

    @Test
    void windLinesFlowInTheWindDirection() {
        BreezeEvent breeze = new BreezeEvent(Map.of("type", "breeze", "lineCount", 3.0, "lineSpeed", 600.0));
        breeze.onStart(ctx);      // プレイヤーがいない（テスト）ときは左向き
        double before = breeze.lineX[0];
        breeze.onUpdate(ctx);
        double after = breeze.lineX[0];
        // 左へ 600px/秒 × 1/60秒 = 10px（画面の端で入り直した場合を除く）
        // 小数の計算では -10 ちょうどにならないことがあるので、ごくわずかなずれは許す
        assertTrue(Math.abs(after - before + 10) < 1e-9 || after > before, "左へ流れていない: " + before + " → " + after);
        assertEquals(3, breeze.lineX.length);
    }

    @Test
    void restsFiveSecondsBetweenBreezes() {
        // 通常5秒 → そよ風3秒 → 通常5秒 → … : 5〜8秒、13〜16秒、21〜24秒に吹く
        run(5.1);
        assertEquals(1, events.activeCount());   // 5秒で吹く
        run(3.0);
        assertEquals(0, events.activeCount());   // 8秒で止む
        run(4.8);
        assertEquals(0, events.activeCount());   // 12.9秒: まだ休み
        run(0.2);
        assertEquals(1, events.activeCount());   // 13秒でまた吹く
        run(3.0);
        assertEquals(0, events.activeCount());   // 16秒で止む
        run(5.0);
        assertEquals(1, events.activeCount());   // 21秒でまた吹く
    }

    @Test
    void easingsMatchEasingsNet() {
        assertEquals(0.0, Easing.inOutSine(0), 1e-9);
        assertEquals(0.5, Easing.inOutSine(0.5), 1e-9);
        assertEquals(1.0, Easing.inOutSine(1), 1e-9);
        assertEquals(1.0, Easing.outBounce(1), 1e-9);
        assertEquals(0.75, Easing.outBounce(1.5 / 2.75), 1e-9);
    }
}
