package org.example.stages.stage2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

import org.example.media.Assets;
import org.example.core.Game;
import org.example.media.Sound;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.Easing;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;

/**
 * 突風のくり返し（ステージ2）。ステージが終わるまで動き続け、
 * 「アイドル → 突風」をくり返す。1周 = idleTime + gustTime 秒。
 *
 * 花の動かし方（Flower.java には setSwayGain() だけを足してある）:
 * 突風の間だけ花の通常の揺れの力を消し（swayGain を 0 にする。swayPeriod は変えないので、揺れの時間は進み続ける）、
 * 毎ステップ push() で「次のステップに目標の位置へ着く力」を計算して加える。
 * これで、ばねが柔らかくても行き過ぎや揺れ戻しがなく、突風の間はずっと左へ傾いたままになる。
 * この計算は Flower.update() のばねの式に合わせてある。Flower のばねの式を変えたら、forceToReach() も直すこと。
 *
 * 突風が終わる flowerRelease 秒前からは、位置を決めるのをやめて、ステージ1のそよ風と同じように
 * push() の力（落ち着く位置）をなめらかに抜くだけにする。すると、ばねの反動で反対側（右）へしなって戻る。
 * しなりが足りなければ rebound、戻りが弱ければ counter で押す力を足して調整する。
 *
 * 反動のあとは、揺れの力の倍率（swayGain）を 0 から 1 までなめらかに上げて、通常の揺れへ戻す。
 * 揺れの時間は止まっていないので、リズムは途切れず、力も連続して強まるだけ。だから動きが止まって見えない。
 *
 * stage.json の events に書く値（省略すると右の初期値）:
 * idleTime 7 / gustTime 4 / warnTime 2 / windSpeed 19.4 / accelScale 2.0 /
 * maxSpeedScale 1.5
 * tilt 1.5 / memory 86 / tiltIn 0.35 / bounceTime 1.5 / windFade 0.4
 * swayDelay 1.0 / swayFadeIn 1.5 / rebound 0.6 / reboundTime 0.4 / counter 0.4 / counterTime 0.5 / flowerRelease 0.5
 * streakRate 45 / curlChance 0.15 / streakColor "#ffffff"
 * se "tailwind" / warnSe "SE_Alert" / attentionImage "attention"
 */
public class TailwindEvent extends StageEvent {

    private final double idleTime = Math.max(0, number("idleTime", 7));
    private final double gustTime = Math.max(0.1, number("gustTime", 4));
    private final double warnTime = Math.max(0, number("warnTime", 2));
    private final double windSpeed = number("windSpeed", 19.4);
    private final double accelScale = number("accelScale", 2.0);
    private final double maxSpeedScale = number("maxSpeedScale", 1.5);
    private final double tilt = number("tilt", 1.5);
    private final double memory = number("memory", 86);
    private final double tiltIn = Math.max(0.01, number("tiltIn", 0.35));
    private final double windFade = Math.max(0.01, number("windFade", 0.4));
    private final double streakRate = number("streakRate", 45);
    private final double curlChance = number("curlChance", 0.15);
    private final Color streakColor = Color.web(string("streakColor", "#ffffff"));
    private final String se = string("se", "tailwind");
    private final String warnSe = string("warnSe", "SE_Alert"); // 予告が始まったときの警告音。"" なら鳴らさない
    private final String attentionImage = string("attentionImage", "attention"); // 右端に並べる危険マーク
    private final double bounceTime;

    // ---- 突風の間の倒れ方 ----
    /** 倒す力の強さの倍率（大きいほど深く倒れる） */
    private static final double TILT_INTENSITY = 2.8;
    /** 風が強まる秒数（吹き始め） */
    private static final double RAMP_IN = 0.5;
    /**
     * 突風が終わる何秒前から、花を倒す力を抜き始めるか。
     * 短いほど急に力が抜けて、反対側へ大きくしなる。長いほどゆっくり戻る。
     */
    private final double flowerRelease = Math.max(0.01, number("flowerRelease", 0.5));

    // ---- 突風が終わったあとの「反対側へしなって戻る」動き ----
    /**
     * 反対側（右）へ押し返す力の強さ（倒した力に対する比）。0 ならばねの反動だけでしなる。
     * 大きいほど右へ深くしなる（0.3〜1.0）。
     */
    private final double rebound = number("rebound", 0.6);
    /** 右へ押し返す時間（秒）。rebound が 0 のときは使われない */
    private final double reboundTime = Math.max(0.01, number("reboundTime", 0.4));
    /**
     * 右へしなったあと、左へ戻すときに押す力の強さ（倒した力に対する比）。
     * 0 ならばねの力だけで戻る。戻りが弱いときに上げる（0.2〜0.8）。
     */
    private final double counter = number("counter", 0.4);
    /** 左へ押し戻す時間（秒）。右へ押し返し終わったあとに続けて働く */
    private final double counterTime = Math.max(0.01, number("counterTime", 0.5));
    /**
     * 突風が終わったあと、通常の揺れを消したまま、反動と押し戻しだけで動かす秒数。
     * rebound・counter の時間（reboundTime + counterTime）のほうが長ければ、そちらが使われる。
     */
    private final double swayDelay = Math.max(0, number("swayDelay", 1.0));
    /**
     * そのあと、通常の揺れの力の倍率を 0 から 1 までなめらかに戻す秒数。長いほどゆっくり揺れが戻る。
     * 突風が終わってから次の突風が始まるまでの時間（idleTime）より、
     * （swayDelay と合わせて）短くすること。0 ならすぐ再開する。
     */
    private final double swayFadeIn = Math.max(0, number("swayFadeIn", 1.5));

    private final Random random = new Random();
    private final List<Streak> streaks = new ArrayList<>();

    private boolean gusting;
    private double gustElapsed;
    private double warnElapsed = -1; // 0以上なら予告中（危険マークを表示中）

    // 突風が終わったあとの戻り動作の状態
    private boolean returning;
    private double returnElapsed;

    // 花の速さを調べるための、ひとつ前のステップの位置
    private double prevRatio;
    // 突風が始まったときの花の位置（ここから目標の位置へ、なめらかに倒していく）
    private double gustStartRatio;

    /** 白い直線1本。先端（左端）を先頭に、右へ尾を引いて左へ流れる。 */
    private static final class Streak {
        double x, y, length, speed, width, alpha;
        boolean curl;
    }

    public TailwindEvent(Map<String, Object> params) {
        super(params);
        // バウンドの時間は、突風の長さから傾く時間を引いた残りを超えない
        this.bounceTime = Math.min(number("bounceTime", 1.5), Math.max(0, gustTime - tiltIn));
    }

    // ============================================================
    // 進行
    // ============================================================
    @Override
    protected void onStart(EventContext ctx) {
        gusting = false;
        returning = false;
        returnElapsed = 0;
        warnElapsed = -1;
        streaks.clear();
        prevRatio = ctx.flower.swayRatio();
        ctx.flower.setSwayGain(1.0);
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        // 花の今の位置と速さ（前のステップからの変化から求める）
        double ratio = ctx.flower.swayRatio();
        double velocity = (ratio - prevRatio) / Game.STEP;
        prevRatio = ratio;

        // 今が1周のどこか（アイドル → 突風）
        double cycle = idleTime + gustTime;
        double c = activeTime() % cycle;
        boolean nowGusting = c >= idleTime;

        gustElapsed = nowGusting ? c - idleTime : 0;
        boolean wasWarning = warnElapsed >= 0;
        warnElapsed = (!nowGusting && warnTime > 0 && c >= idleTime - warnTime)
                ? c - (idleTime - warnTime)
                : -1;
        if (!wasWarning && warnElapsed >= 0 && !warnSe.isEmpty())
            Sound.longSe(warnSe); // 予告が始まったら警告音

        if (nowGusting && !gusting)
            startGust(ctx);
        if (!nowGusting && gusting)
            endGust(ctx);
        gusting = nowGusting;

        if (gusting)
            applyGust(ctx, ratio, velocity);
        else if (returning)
            applyReturn(ctx);
        updateStreaks();
    }

    @Override
    protected void onEnd(EventContext ctx) {
        // ステージを抜けるときなど。変えたものを元に戻す
        if (gusting)
            endGust(ctx);
        ctx.flower.setSwayGain(1.0);
        returning = false;
        gusting = false;
        warnElapsed = -1;
        streaks.clear();
    }

    private void startGust(EventContext ctx) {
        Sound.longSe(se); // ポーズで一時停止できる鳴らし方（突風の音は数秒続くため）
        returning = false;
        gustStartRatio = ctx.flower.swayRatio();
        if (ctx.player != null)
            ctx.player.setGainScale(accelScale, maxSpeedScale);
    }

    private void endGust(EventContext ctx) {
        // 揺れの力はまだ戻さない。反対側へしなって落ち着くまで applyReturn() で動かす
        returning = true;
        returnElapsed = 0;
        if (ctx.player != null) {
            ctx.player.setGainScale(1.0, 1.0);
            ctx.player.setWindVelocity(0);
        }
    }

    /** 突風の間、毎ステップ：ハチドリを流し、花を左へ傾け、直線を出す。 */
    private void applyGust(EventContext ctx, double ratio, double velocity) {
        double strength = windStrength(gustElapsed);

        if (ctx.player != null)
            ctx.player.setWindVelocity(-windSpeed * strength);

        // 花の通常の揺れの力を消す（揺れの時間は進み続ける）
        ctx.flower.setSwayGain(0);
        if (gustElapsed < gustTime - flowerRelease) {
            // 吹いている間：目標の位置へ着く力で、左へ傾いたまま保つ
            ctx.flower.push(trackingForce(ctx, ratio, velocity, gustElapsed));
        } else {
            // 終わる直前：位置を決めるのをやめて、力をなめらかに抜く（あとはばねの反動に任せる）
            ctx.flower.push(gustForce(ctx, gustElapsed));
        }

        double expected = streakRate * Game.STEP * strength;
        int n = (int) expected;
        if (random.nextDouble() < expected - n)
            n++;
        for (int i = 0; i < n; i++)
            spawnStreak();
    }

    /**
     * 突風のあと、毎ステップ：
     * 1) まず、揺れの力を消したまま、右へ押し返して（rebound）、左へ押し戻す（counter）。
     * 2) そのあと swayFadeIn 秒かけて、揺れの力の倍率を 0 → 1 へなめらかに戻す。
     *    揺れの時間は進み続けているので、リズムは途切れない。
     * 3) 倍率が 1 に戻ったら終わる。
     */
    private void applyReturn(EventContext ctx) {
        double hold = Math.max(swayDelay, reboundTime + counterTime);

        // 1) 揺れの力を消したまま、押し返し・押し戻し
        if (returnElapsed < hold) {
            ctx.flower.setSwayGain(0);
            ctx.flower.push(reboundForce(ctx, returnElapsed));
            returnElapsed += Game.STEP;
            return;
        }

        // 揺れのない設定（swayPeriod が 0）や、戻す時間が 0 のときは、そのまま再開する
        if (ctx.flowerConfig.swayPeriod <= 0 || swayFadeIn <= 0) {
            finishReturn(ctx);
            return;
        }

        // 2) 揺れの力を 0 → 1 へなめらかに上げる
        double s = Math.clamp((returnElapsed - hold) / swayFadeIn, 0, 1);
        ctx.flower.setSwayGain(Easing.inOutSine(s));
        returnElapsed += Game.STEP;

        // 3) 元の強さに戻った
        if (s >= 1)
            finishReturn(ctx);
    }

    private void finishReturn(EventContext ctx) {
        ctx.flower.setSwayGain(1.0);
        returning = false;
    }

    /** 風の強さ（0〜1）。始まりと終わりをなめらかにする。 */
    private double windStrength(double e) {
        double in = e / windFade;
        double out = (gustTime - e) / windFade;
        return Math.clamp(Math.min(in, out), 0, 1);
    }

    // ============================================================
    // 花を押す力（push に渡す値。「ずっと押し続けたときに落ち着く位置」。左がマイナス）
    // ============================================================

    /**
     * 次のステップで花が目標の位置に着くための push の力。
     *
     * Flower.update() は、次の式で花を動かしている（揺れの力を消しているので drive = 0。
     * swayPeriod が 0 の設定なら drive = k × 風）:
     * 加速度 = −k×位置 − c×速さ + drive + k×力
     * これを力について解くと、目標へちょうど着く力が求まる。
     */
    private double trackingForce(EventContext ctx, double ratio, double velocity, double e) {
        return forceToReach(ctx, ratio, velocity, gustPosition(ctx, e + Game.STEP));
    }

    /** 次のステップで花が goal の位置に着くための push の力（上の式を力について解いたもの）。 */
    private double forceToReach(EventContext ctx, double ratio, double velocity, double goal) {
        FlowerConfig cfg = ctx.flowerConfig;
        double k = cfg.stiffness;
        double c = cfg.damping;
        double dt = Game.STEP;
        // 一定のリズムの揺れは消してあるので風は無関係。不規則な風の設定のときだけ、風の分を引く
        double wind = cfg.swayPeriod > 0 ? 0 : ctx.wind.value();

        double accel = ((goal - ratio) / dt - velocity) / dt;
        return accel / k + ratio + (c / k) * velocity - wind;
    }

    /**
     * 突風の間の花の目標の位置（通常の揺れの大きさを 1 とした値。左がマイナス）。
     * 突風が始まったときの位置から、RAMP_IN 秒かけてなめらかに倒れ、そのあとは細かくバタつく。
     */
    private double gustPosition(EventContext ctx, double e) {
        double target = gustTarget(ctx);
        double in = Easing.inOutSine(Math.clamp(e / RAMP_IN, 0, 1));
        double flutter = (Math.sin(e * 13.0) + Math.sin(e * 8.3)) * 0.025 * target * in;
        return gustStartRatio + (target - gustStartRatio) * in + flutter;
    }

    /** 突風で倒れたときの落ち着く位置（左がマイナス）。 */
    private double gustTarget(EventContext ctx) {
        double half = Math.max(1, ctx.flowerConfig.swayWidth / 2);
        return -(tilt * memory) / half * TILT_INTENSITY;
    }

    /** 突風が終わる直前の力。倒した位置を保つ力から、なめらかに 0 まで抜いていく。 */
    private double gustForce(EventContext ctx, double e) {
        double target = gustTarget(ctx);

        double in = Easing.inOutSine(Math.clamp(e / RAMP_IN, 0, 1));
        double out = Easing.inOutSine(Math.clamp((gustTime - e) / flowerRelease, 0, 1));
        double env = Math.min(in, out);

        // 風が終わる直前(0.2秒前)でバタつきを消す
        double flutterFade = Math.clamp((gustTime - e) / 0.2, 0, 1);
        double flutter = (Math.sin(e * 13.0) + Math.sin(e * 8.3)) * 0.025 * target * env * flutterFade;

        return target * env + flutter;
    }

    /**
     * 突風のあとの力。まず反対側（右）へ押し返し、続けて左へ押し戻す。
     * どちらも、なめらかに強まって、なめらかに消える。
     */
    private double reboundForce(EventContext ctx, double r) {
        double target = gustTarget(ctx); // 左がマイナス
        if (r < reboundTime) {
            double pulse = Math.sin(Math.PI * r / reboundTime); // 0 → 1 → 0
            return -target * rebound * pulse;
        }
        double r2 = r - reboundTime;
        if (r2 < counterTime) {
            double pulse = Math.sin(Math.PI * r2 / counterTime);
            return target * counter * pulse;
        }
        return 0;
    }

    // ============================================================
    // 白い直線
    // ============================================================
    private void spawnStreak() {
        Streak s = new Streak();
        s.x = Game.WIDTH + random.nextDouble() * 130;
        s.y = random.nextDouble() * Game.HEIGHT;
        s.length = 130 + random.nextDouble() * 346;
        s.speed = 43 + random.nextDouble() * 35;
        s.width = 3.2 + random.nextDouble() * 3.2;
        s.alpha = 0.4 + random.nextDouble() * 0.5;
        s.curl = random.nextDouble() < curlChance;
        streaks.add(s);
    }

    private void updateStreaks() {
        for (Iterator<Streak> it = streaks.iterator(); it.hasNext();) {
            Streak s = it.next();
            s.x -= s.speed;
            if (s.x + s.length < -86)
                it.remove(); // 尾まで画面の左に出たら消す
        }
    }

    // ============================================================
    // 描画
    // ============================================================
    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(streakColor);
        for (Streak s : streaks) {
            gc.setGlobalAlpha(s.alpha);
            gc.setLineWidth(s.width);
            gc.strokeLine(s.x, s.y, s.x + s.length, s.y);
            if (s.curl) {
                // 先端に、くるんと丸まる輪
                double r = 17 + s.width * 2;
                gc.strokeArc(s.x - r, s.y - 2 * r, 2 * r, 2 * r, 270, 320, ArcType.OPEN);
            }
        }
        gc.restore();

        drawAttentionMarks(gc);
    }

    // 風が来る方向（右）の端に縦に並べる危険マーク（images/attention.png）
    /** 危険マークの画像の中で、絵のある範囲（左・上・幅・高さ。画像は 1080x1080 で、まわりが透明） */
    private static final double[] ATTENTION_SRC = { 120, 169, 840, 742 };
    /** 危険マークの幅（px）。高さは画像の比で決まる */
    private static final double ATTENTION_WIDTH = 120;
    /** 画面の右端からのすき間（px） */
    private static final double ATTENTION_MARGIN = 30;
    /** 縦の位置（マークの真ん中の高さ。上から順に） */
    private static final double[] ATTENTION_Y = { 270, 540, 810 };

    /** 予告の間、右端に危険マークを縦に3つ出す。右から滑り込んできて、点滅する。 */
    private void drawAttentionMarks(GraphicsContext gc) {
        if (warnElapsed < 0)
            return;
        Image img = Assets.image(attentionImage);
        if (img == null)
            return;

        double w = ATTENTION_WIDTH;
        double h = w * ATTENTION_SRC[3] / ATTENTION_SRC[2];
        double slide = easeOutCubic(Math.clamp(warnElapsed / 0.25, 0, 1));
        double x = Game.WIDTH - ATTENTION_MARGIN - w + (w + ATTENTION_MARGIN) * (1 - slide);
        double blink = 0.8 + 0.2 * Math.sin(warnElapsed * 16);

        gc.save();
        gc.setGlobalAlpha(blink);
        for (double cy : ATTENTION_Y) {
            gc.drawImage(img, ATTENTION_SRC[0], ATTENTION_SRC[1], ATTENTION_SRC[2], ATTENTION_SRC[3],
                    x, cy - h / 2, w, h);
        }
        gc.restore();
    }

    // ============================================================
    // イージング
    // ============================================================
    private static double easeOutCubic(double p) {
        return 1.0 - Math.pow(1.0 - p, 3);
    }
}