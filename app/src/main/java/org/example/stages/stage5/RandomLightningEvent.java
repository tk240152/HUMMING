package org.example.stages.stage5;

import java.util.Map;
import java.util.Random;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.example.media.Assets;
import org.example.core.Game;
import org.example.media.Sound;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;

/**
 * ランダムな落雷（ステージ5）。
 * 落ちる位置も、落ちるまでの待ち時間も、毎回ランダムに変わる。
 *
 * <pre>
 * 待機（idleMin〜idleMax 秒のランダム）→ 警告（warnTime）→ 落雷（strikeTime）→ また待機 …
 * </pre>
 *
 * 警告が始まる瞬間に、落ちる位置を決める。警告のあいだは範囲が動かないので、外へ逃げれば避けられる。
 *
 * ステージが終わるまで動き続けるので、stage.json に "duration" や "when" は書かない。
 * 
 * <pre>
 * { "type": "randomLightning", "idleMin": 3, "idleMax": 8 }
 * angle : 雷の帯の傾き（度）。0 で真っすぐ縦、プラスで右下がり「\」、マイナスで左下がり「/」。-60〜60。省略すると -10.2
 * width : 雷の帯の太さ（px）。平行四辺形の横の幅で、当たり判定もこの幅。省略すると 302
 * </pre>
 */
public class RandomLightningEvent extends StageEvent {

    private enum Phase {
        IDLE, WARN, STRIKE
    }

    private final double idleMin = Math.max(0, number("idleMin", 3));
    private final double idleMax = Math.max(idleMin, number("idleMax", 8));
    private final double warnTime = Math.max(0, number("warnTime", 0.5));
    private final double strikeTime = Math.max(0.1, number("strikeTime", 1.5));
    private final double margin = Math.max(0, number("margin", 240));
    private final String se = string("se", "thunder");
    private final String warnSe = string("warnSe", "SE_Alert"); // 警告が出たときの警告音。"" なら鳴らさない
    private final String boltImage = string("boltImage", "lightning_bolt"); // 稲妻の画像
    /** 雷に当たったときの「ビリビリ」（ハチドリのまわりの電気のトゲ） */
    private final ElectricShock shock = new ElectricShock();

    /** 警告中に点滅させる稲妻の濃さ */
    private static final double WARN_ALPHA = 0.3;
    /** 落雷中の稲妻の濃さ（1.0 で不透明。小さいほど透ける） */
    private static final double STRIKE_ALPHA = 1.0;
    /** 落雷中のチカチカで、薄くなったときの濃さと、切り替える間隔（秒） */
    private static final double FLICKER_ALPHA = 0.35;
    private static final double FLICKER_SECONDS = 0.1;

    private final Random random = new Random();

    private Phase phase = Phase.IDLE;
    private double phaseStart; // 今の段階が始まった時刻（イベントが始まってからの秒数）
    private double phaseEnd; // 今の段階が終わる時刻

    /** 雷の範囲の中心線の、画面の上端での X 座標 */
    private double strikeX;

    // 平行四辺形の形状パラメータ
    /** 帯の太さ（px。平行四辺形の横の幅で、当たり判定もこの幅） */
    private final double width = Math.max(1, number("width", 302));
    /** 帯の傾き（度）。0 で真っすぐ縦、プラスで右下がり「\」、マイナスで左下がり「/」 */
    private final double angle = Math.clamp(number("angle", -10.2), -60, 60);
    private final double skew = Game.HEIGHT * Math.tan(Math.toRadians(angle)); // 画面の上端から下端までで、中心線が横にずれる量

    // カラーコード: #e83a37 (不透明度50% -> Color.rgb(232, 58, 55, 0.5))
    private static final Color STRIKE_COLOR = Color.rgb(232, 58, 55, 0.5);

    public RandomLightningEvent(Map<String, Object> params) {
        super(params);
    }

    @Override
    protected void onStart(EventContext ctx) {
        setPhase(Phase.IDLE, activeTime(), randomIdle());
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        shock.update(ctx.player);
        double now = activeTime();

        // 今の段階が続いている間
        if (now < phaseEnd) {
            if (phase == Phase.STRIKE) {
                applyLightningEffect(ctx);
            }
            return;
        }

        // 次の段階へ進む
        switch (phase) {
            case IDLE -> {
                pickStrikePosition();
                setPhase(Phase.WARN, now, warnTime);
                if (warnTime > 0 && !warnSe.isEmpty()) Sound.longSe(warnSe); // 警告が出たら警告音
            }
            case WARN -> {
                setPhase(Phase.STRIKE, now, strikeTime);
                Sound.se(se);
            }
            case STRIKE -> setPhase(Phase.IDLE, now, randomIdle());
        }
    }

    @Override
    protected void onEnd(EventContext ctx) {
        phase = Phase.IDLE;
    }

    private void setPhase(Phase next, double now, double length) {
        phase = next;
        phaseStart = now;
        phaseEnd = now + length;
    }

    /** 次の待機の長さ（idleMin〜idleMax のランダム）。 */
    private double randomIdle() {
        return idleMin + random.nextDouble() * (idleMax - idleMin);
    }

    /**
     * 落ちる位置をランダムに決める。
     * 帯の中心が、画面の縦の真ん中で、左右の端から margin 以上内側に来るようにする。
     */
    private void pickStrikePosition() {
        double midX = margin + random.nextDouble() * Math.max(1, Game.WIDTH - margin * 2);
        strikeX = midX - skew * 0.5;
    }

    /** 落雷発生中（本番フェーズ）、範囲内チェックとプレイヤーへの効果適用 */
    private void applyLightningEffect(EventContext ctx) {
        if (ctx.player == null)
            return;

        Point2D body = ctx.player.getBodyCenter();
        double px = body.getX();
        double py = body.getY();

        // 平行四辺形の範囲内に入っているかチェック
        if (isInsideLightning(px, py)) {
            if (ctx.player instanceof org.example.player.Player) {
                org.example.player.Player p = (org.example.player.Player) ctx.player;
                boolean wasKnocked = p.isKnockedBack();
                p.knockback(0.0, 0.0, 2.0, 3.0);
                if (!wasKnocked && p.isKnockedBack()) shock.start(); // 当たった瞬間にビリビリを出す
            }
        }
    }

    /** ハチドリが平行四辺形の範囲内にいるか判定 */
    private boolean isInsideLightning(double px, double py) {
        double topX = strikeX;
        double bottomX = strikeX + skew;

        // Y座標に応じた現在のエリアのX範囲を算出
        double progress = Math.clamp(py / Game.HEIGHT, 0, 1);
        double currentCenterX = topX + (bottomX - topX) * progress;

        return px >= currentCenterX - width / 2 && px <= currentCenterX + width / 2;
    }

    // ============================================================
    // 描画
    // ============================================================
    /**
     * 稲妻と、当たったときのビリビリの吹き出しは、花・プレイヤーより奥に描く（プレイヤーが隠れないように）。
     * 吹き出しは稲妻より手前。
     */
    @Override
    public void renderBack(GraphicsContext gc, EventContext ctx) {
        renderBolt(gc);
        shock.render(gc, ctx.player);
    }

    private void renderBolt(GraphicsContext gc) {
        if (phase == Phase.IDLE)
            return;

        double elapsed = activeTime() - phaseStart;

        gc.save();
        if (phase == Phase.WARN) {
            // 落雷直前（予告）：0.125秒ごとに点滅させて、どこに落ちるかを知らせる
            boolean blink = ((int) (elapsed / 0.125) % 2) == 0;
            if (blink) {
                drawBolt(gc, WARN_ALPHA);
            }
        } else {
            // 落雷中：濃い・薄いを細かく切り替えて、チカチカさせる
            boolean bright = ((int) (elapsed / FLICKER_SECONDS) % 2) == 0;
            drawBolt(gc, bright ? STRIKE_ALPHA : FLICKER_ALPHA);
        }
        gc.restore();
    }

    /**
     * 平行四辺形を描画するヘルパー。
     * strikeX は中心線の位置なので、左右に width の半分ずつ広げて描く（当たり判定と同じ範囲）。
     */
    private void drawParallelogram(GraphicsContext gc, Color color) {
        gc.setFill(color);
        double topX = strikeX - width / 2;
        double bottomX = strikeX + skew - width / 2;

        double[] xPoints = {
                topX,
                topX + width,
                bottomX + width,
                bottomX
        };
        double[] yPoints = {
                0,
                0,
                Game.HEIGHT,
                Game.HEIGHT
        };

        gc.fillPolygon(xPoints, yPoints, 4);
    }

    /**
     * 稲妻の画像を、当たり判定の帯の真ん中に、画面の高さいっぱいで描く（画像の縦横の比は変えない）。
     * 画像が無ければ、今までどおり帯を塗る。alpha は濃さ（警告中は薄く）。
     * 画像は左下がり「/」に描かれているので、帯が右下がり（skew がプラス）のときは左右を反転して描く。
     */
    private void drawBolt(GraphicsContext gc, double alpha) {
        Image img = Assets.image(boltImage);
        if (img == null) {
            drawParallelogram(gc, alpha < 1 ? STRIKE_COLOR : Color.rgb(255, 255, 255, 0.7));
            return;
        }
        double h = Game.HEIGHT;
        double w = h * img.getWidth() / img.getHeight();
        double centerX = strikeX + skew / 2; // 帯の真ん中（上端と下端の中心線の真ん中）
        gc.setGlobalAlpha(alpha);
        if (skew > 0) {
            gc.drawImage(img, centerX + w / 2, 0, -w, h); // 左右を反転
        } else {
            gc.drawImage(img, centerX - w / 2, 0, w, h);
        }
        gc.setGlobalAlpha(1.0);
    }
}