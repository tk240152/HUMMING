package org.example.stages.stage5;

import java.util.Map;

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
 * 落雷のくり返し（ステージ5）。
 * 「安全な待機時間（idleTime）」のあと、
 * 「警告時間（warnTime / 落雷直前シーン）」を経て、
 * 「落雷発生中（activeTime）」に範囲内にいるとハチドリが被弾する。
 *
 * 雷が落ちる位置は、警告が始まった瞬間のプレイヤーの位置。
 * 警告のあいだは雷の範囲が動かないので、範囲の外へ逃げれば避けられる。
 * 
 * stage.json の events に書く値（省略すると右の初期値）:
 * idleTime 7 / activeTime 2 / warnTime 0.5 / se "thunder" / warnSe "SE_Alert"
 * angle 10.2（雷の帯の傾き・度。0 で真っすぐ縦、プラスで右下がり「\」、マイナスで左下がり「/」。-60〜60）
 * width 302（雷の帯の太さ・px。平行四辺形の横の幅で、当たり判定もこの幅）
 */
public class LightningEvent extends StageEvent {

    private final double idleTime = Math.max(0, number("idleTime", 7));
    private final double activeTime = Math.max(0.1, number("activeTime", 2)); // 落雷イベント全体の長さ（直前0.5秒 + 発生中1.5秒）
    private final double warnTime = Math.max(0, number("warnTime", 0.5)); // 警告（落雷直前シーン）の長さ
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

    private boolean active;
    private boolean thunderPlayed;
    private double eventElapsed;

    /** 雷の範囲の中心線の、画面の上端での X 座標。警告が始まった瞬間のプレイヤーの位置から決まる */
    private double strikeX;

    // 平行四辺形の形状パラメータ
    /** 帯の太さ（px。平行四辺形の横の幅で、当たり判定もこの幅） */
    private final double width = Math.max(1, number("width", 302));
    /** 帯の傾き（度）。0 で真っすぐ縦、プラスで右下がり「\」、マイナスで左下がり「/」 */
    private final double angle = Math.clamp(number("angle", 10.2), -60, 60);
    private final double skew = Game.HEIGHT * Math.tan(Math.toRadians(angle)); // 画面の上端から下端までで、中心線が横にずれる量

    // カラーコード: #e83a37 (不透明度50% -> Color.rgb(232, 58, 55, 0.5))
    private static final Color STRIKE_COLOR = Color.rgb(232, 58, 55, 0.5);

    public LightningEvent(Map<String, Object> params) {
        super(params);
    }

    // ============================================================
    // 進行
    // ============================================================
    @Override
    protected void onStart(EventContext ctx) {
        active = false;
        eventElapsed = 0;
        pickStrikePosition(ctx);
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        shock.update(ctx.player);
        // ステージ1の風イベントと同様に、1周 = idleTime + activeTime でループさせる
        double cycle = idleTime + activeTime;
        double c = activeTime() % cycle;
        boolean nowActive = c >= idleTime;

        eventElapsed = nowActive ? c - idleTime : 0;

        if (nowActive && !active) {
            startLightning(ctx);
        }
        if (!nowActive && active) {
            endLightning(ctx);
        }
        active = nowActive;

        // 落雷発生中（警告時間を除いた、実際に当たり判定がある本番フェーズ）の処理
        if (active && eventElapsed >= warnTime) {
            if (!thunderPlayed) {
                Sound.se(se);
                thunderPlayed = true;
            }
            applyLightningEffect(ctx);
        }
    }

    @Override
    protected void onEnd(EventContext ctx) {
        active = false;
        eventElapsed = 0;
    }

    private void startLightning(EventContext ctx) {
        pickStrikePosition(ctx);
        thunderPlayed = false;
        if (warnTime > 0 && !warnSe.isEmpty()) Sound.longSe(warnSe); // 警告が出たら警告音
    }

    private void endLightning(EventContext ctx) {
        // 終了時の処理（必要に応じて）
    }

    /**
     * 雷の位置を、今のプレイヤーの位置に決める。
     * 範囲は斜めの帯なので、中心線がプレイヤーのいる高さでちょうどプレイヤーの X を通るように、
     * 画面の上端での中心 X（strikeX）を逆算する。
     */
    private void pickStrikePosition(EventContext ctx) {
        if (ctx.player == null) {
            strikeX = Game.WIDTH / 2.0;
            return;
        }

        Point2D body = ctx.player.getBodyCenter();
        double progress = Math.clamp(body.getY() / Game.HEIGHT, 0, 1);
        strikeX = body.getX() - skew * progress;
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
        if (!active)
            return;

        gc.save();
        if (eventElapsed < warnTime) {
            // 落雷直前（予告）：0.125秒ごとに点滅させて、どこに落ちるかを知らせる
            boolean blink = ((int) (eventElapsed / 0.125) % 2) == 0;
            if (blink) {
                drawBolt(gc, WARN_ALPHA);
            }
        } else {
            // 落雷中：濃い・薄いを細かく切り替えて、チカチカさせる
            boolean bright = ((int) ((eventElapsed - warnTime) / FLICKER_SECONDS) % 2) == 0;
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