package org.example.stages.stage3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.example.media.Assets;
import org.example.media.Sound;
import org.example.core.Game;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;

/**
 * ステージ3だけのイベント「渡り鳥」（Stage3.registerEvents で "birds" として登録している）。
 * 現れている間（duration 秒）に、鳥の群れが画面の右端から左端まで横切る（bothSides なら、左端から右端のこともある）。
 * 鳥が花にぶつかると、花が飛んでいる向きへ押される（花の動きは茎のばねに任せる）。
 * 鳥がハチドリにぶつかると、ハチドリが飛んでいる向きの下へ弾き飛ばされる（回転・操作不能・目が×。1回横切るあいだに1回だけ）。鳥は影響を受けない。
 * 鳥の大きさと群れの広さは、プレイヤーの大きさを基準に決める。
 * <pre>
 * {"type": "birds", "when": {"rest": 10}, "duration": 3, "count": 5, "birdScale": 1.2, "flockScale": 4}
 * count      : 鳥の数。群れは先頭を左にした V 字に並ぶ。省略すると 5
 * birdScale  : 鳥1羽の横幅（プレイヤーの横幅の何倍か）。省略すると 1.2（少し大きい）
 * flockScale : 群れ（V 字）が広がる範囲（プレイヤーの横幅の何倍の四角か）。省略すると 4
 * followPlayer : 1 なら、群れの高さを現れた瞬間のプレイヤーの高さに合わせる（プレイヤーに向かって飛んでくる）。省略すると 1
 * yOffset    : followPlayer のとき、プレイヤーの高さからずらす量（px。下がプラス）。省略すると 0
 * minY, maxY : followPlayer が 0 のとき、群れの中心の高さの範囲（px）
 * lanes      : followPlayer が 0 のとき、minY〜maxY を上から何区分に分けるか。ランダムに選んだ区分の真ん中の高さから入り、
 *              上下が反対の区分へ斜めに飛んでいく（左上から入ったら右下へ。真ん中の区分なら真っすぐ横へ）。省略すると 3
 * bothSides  : 1 なら、群れが右から来るか左から来るかを毎回ランダムに決める。0 なら右からだけ。省略すると 0
 * texture    : 鳥の画像（images/ のファイル名・拡張子なし）。省略すると kari_wataridori。無ければ仮の形を描く
 * hitForce       : 鳥1羽が花にぶつかっているときに、花を押す強さ（通常の揺れの大きさを 1 とした位置まで押す強さ）。省略すると 3
 * hitScale       : 鳥の当たり判定の半径（鳥の横幅の何倍か）。省略すると 0.35
 * flowerHitScale : 花（花びら）の当たり判定の半径（花の大きさ flowerSize の何倍か）。省略すると 0.4
 * knockbackX     : ハチドリにぶつかったとき、飛んでいる向きへ弾き飛ばす距離（プレイヤーの横幅の何倍か）。省略すると 3
 * knockbackY     : 同じく、下へ弾き飛ばす距離（プレイヤーの横幅の何倍か）。省略すると 3
 * knockbackSeconds : 弾き飛ばされている秒数（この間は操作できず、目が×になる）。省略すると 0.8
 * knockbackSpins : 弾き飛ばされている間に回る回数。省略すると 3
 * playerHitScale : ハチドリの当たり判定の半径（プレイヤーの横幅の何倍か）。省略すると 0.5
 * showHitbox     : 1 にすると当たり判定の円を描く（調整用）。省略すると 0
 * se             : 群れが現れたときに鳴らす音（Sound.se の名前）。省略すると "SE_BirdWings"。"" なら鳴らさない
 * warnTime       : 群れが飛び始める前の予告の秒数（duration に含む）。この間は群れが来る高さの、来る側の端に危険マークを出す。省略すると 0（予告なし）
 * attentionImage : 予告で出す危険マークの画像（images/ のファイル名）。省略すると "attention"
 * </pre>
 * 画像は左向き（右から左へ飛ぶ向き）で描く。左から来るときは左右を反転して描く。
 */
public class MigratingBirdsEvent extends StageEvent {

    /** プレイヤーがいないとき（テストなど）に使う横幅 */
    private static final double DEFAULT_PLAYER_WIDTH = 86;

    private final int count;
    private final double birdScale;
    private final double flockScale;
    private final double minY;
    private final double maxY;
    private final int lanes;
    private final boolean bothSides;
    private final boolean followPlayer;
    private final double yOffset;
    private final String textureKey;
    private final double hitForce;
    private final double hitScale;
    private final double flowerHitScale;
    private final boolean showHitbox;
    private final double knockbackX;
    private final double knockbackY;
    private final double knockbackSeconds;
    private final double knockbackSpins;
    private final double playerHitScale;
    private final String se;
    private final double warnTime;
    private final String attentionImage;

    private final Random random = new Random();

    // 群れの形（群れの中心からのずれ）と、羽ばたきのずれ
    final double[] offsetX;
    final double[] offsetY;
    private final double[] phase;

    double birdWidth;
    double birdHeight;
    double flockSize;
    double centerX;
    double centerY;
    /** 飛ぶ向き。-1 なら右から左へ、1 なら左から右へ */
    int dir = -1;
    /** 群れが入ってくる高さと、出ていく高さ（この間をまっすぐ斜めに飛ぶ） */
    double startY;
    double endY;
    /** 飛ぶ道すじの傾き（ラジアン）。鳥の画像と V 字の並びを、この角度だけ回す（まっすぐ横に飛ぶなら 0） */
    double tilt;
    /** このステップで花にぶつかっている鳥の数 */
    int hits;
    /** この群れがもうハチドリにぶつかったか（1回横切るあいだに1回だけ弾き飛ばす） */
    boolean hitPlayer;

    public MigratingBirdsEvent(Map<String, Object> params) {
        super(params);
        count = (int) Math.max(0, number("count", 5));
        birdScale = Math.max(0.1, number("birdScale", 1.2));
        flockScale = Math.max(0.1, number("flockScale", 4));
        minY = number("minY", Game.HEIGHT * 0.2);
        maxY = number("maxY", Game.HEIGHT * 0.8);
        lanes = (int) Math.max(1, number("lanes", 3));
        bothSides = number("bothSides", 0) > 0;
        followPlayer = number("followPlayer", 1) > 0;
        yOffset = number("yOffset", 0);
        textureKey = string("texture", "kari_wataridori");
        aspect(textureKey); // ステージを読み込むときに先に測っておく（最初の群れが現れた瞬間に引っかからないように）
        hitForce = Math.max(0, number("hitForce", 3));
        hitScale = Math.max(0, number("hitScale", 0.35));
        flowerHitScale = Math.max(0, number("flowerHitScale", 0.4));
        showHitbox = number("showHitbox", 0) > 0;
        knockbackX = number("knockbackX", 3);
        knockbackY = number("knockbackY", 3);
        knockbackSeconds = Math.max(0, number("knockbackSeconds", 0.8));
        knockbackSpins = number("knockbackSpins", 3);
        playerHitScale = Math.max(0, number("playerHitScale", 0.5));
        se = string("se", "SE_BirdWings");
        warnTime = Math.clamp(number("warnTime", 0), 0, Math.max(0, duration));
        attentionImage = string("attentionImage", "attention");
        offsetX = new double[count];
        offsetY = new double[count];
        phase = new double[count];
    }

    @Override
    protected void onStart(EventContext ctx) {
        double playerWidth = ctx.player != null ? ctx.player.width : DEFAULT_PLAYER_WIDTH;
        birdWidth = playerWidth * birdScale;
        birdHeight = birdWidth * aspect(textureKey);
        flockSize = playerWidth * flockScale;

        hitPlayer = false;
        dir = bothSides && random.nextBoolean() ? 1 : -1;
        arrangeFlock();
        chooseY(ctx);
        centerY = startY;
        centerX = startX();
        tilt = Math.atan((endY - startY) / (endX() - startX()));
        if (!se.isEmpty()) Sound.longSe(se); // 群れが飛んでくる音（数秒続くので、ポーズで一時停止できる鳴らし方）
    }

    /** 一度測った画像の「縦 ÷ 横」（画像ごと）。群れが現れるたびに読み直すと重いので覚えておく */
    private static final Map<String, Double> ASPECTS = new ConcurrentHashMap<>();

    /**
     * 画像の「縦 ÷ 横」。画像が無ければ 0.8。
     * 並べるときに使うだけなので、JavaFX を使わずにファイルから大きさだけ読む（テストでも動くように）。
     * 画像の中身（絵）は読まず、ファイルの先頭に書いてある大きさだけを読む。結果は覚えておき、2回目からは読まない。
     */
    private static double aspect(String key) {
        return ASPECTS.computeIfAbsent(key, MigratingBirdsEvent::readAspect);
    }

    private static double readAspect(String key) {
        URL url = MigratingBirdsEvent.class.getResource("/" + key + ".png");
        if (url == null) return 0.8;
        try (InputStream raw = url.openStream(); ImageInputStream in = ImageIO.createImageInputStream(raw)) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) return 0.8;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return (double) reader.getHeight(0) / reader.getWidth(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return 0.8;
        }
    }

    /**
     * 群れが入ってくる高さ（startY）と、出ていく高さ（endY）を決める。
     * followPlayer なら、どちらも現れた瞬間のプレイヤーの高さ（＋ yOffset）にする（プレイヤーに向かって真っすぐ飛んでくる）。
     * そうでなければ minY〜maxY を上から lanes 個の区分に分け、ランダムに選んだ区分の真ん中から入り、
     * 上下が反対の区分の真ん中へ出ていく（左上から入ったら右下へ。真ん中の区分なら真っすぐ横へ）。
     * どちらの場合も、群れが画面からはみ出さない高さに収める。
     */
    private void chooseY(EventContext ctx) {
        if (followPlayer && ctx.player != null) {
            startY = endY = clampY(ctx.player.y + yOffset);
            return;
        }
        int lane = random.nextInt(lanes);
        startY = clampY(laneY(lane));
        endY = clampY(laneY(lanes - 1 - lane));
    }

    /** 上から k 番目（0 から数える）の区分の真ん中の高さ。 */
    private double laneY(int k) {
        return minY + Math.max(0, maxY - minY) * (k + 0.5) / lanes;
    }

    /** 群れが画面からはみ出さない高さに収める。 */
    private double clampY(double y) {
        double half = flockSize / 2;
        return Math.clamp(y, Math.min(half, Game.HEIGHT / 2.0), Math.max(Game.HEIGHT - half, Game.HEIGHT / 2.0));
    }

    /**
     * 群れを V 字に並べる。先頭の1羽が飛ぶ向きの側の真ん中にいて、残りは上・下に交互に、
     * 1羽ずつ斜め後ろへ並ぶ。V 字の大きさは群れの範囲（flockSize の四角）いっぱいにする。
     * <pre>
     * 右から来るとき          左から来るとき
     *        3            3
     *      1                1
     *    0  ← 飛ぶ向き   飛ぶ向き →  0
     *      2                2
     *        4            4
     * </pre>
     */
    private void arrangeFlock() {
        double half = Math.max(0, (flockSize - birdWidth) / 2);
        double halfY = Math.max(0, (flockSize - birdHeight) / 2);
        int rows = count / 2; // 先頭から後ろへ何列あるか（5羽なら 2）
        for (int i = 0; i < count; i++) {
            int row = (i + 1) / 2;
            int side = i % 2 == 1 ? -1 : 1; // 奇数番は上、偶数番は下
            offsetX[i] = rows > 0 ? dir * (half - 2 * half * row / rows) : 0;
            offsetY[i] = rows > 0 ? side * halfY * row / rows : 0;
            phase[i] = random.nextDouble() * Math.PI * 2;
        }
    }

    /** 群れが来る側の画面の外にいる位置（ここから入ってくる）。 */
    private double startX() {
        return Game.WIDTH / 2.0 - dir * (Game.WIDTH / 2.0 + flockSize / 2 + birdWidth / 2);
    }

    /** 群れが反対側の画面の外に出た位置。 */
    private double endX() {
        return Game.WIDTH / 2.0 + dir * (Game.WIDTH / 2.0 + flockSize / 2 + birdWidth / 2);
    }

    /** 群れが飛び始めるまでの、予告の残り時間があるか（予告中は群れは右の外で待ち、危険マークを出す） */
    private boolean warning() {
        return activeTime() < warnTime;
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        // 予告のあと、残りの (duration - warnTime) 秒でちょうど端から端まで横切る
        double flyTime = duration - warnTime;
        double p = flyTime > 0 ? Math.clamp((activeTime() - warnTime) / flyTime, 0, 1) : 0;
        centerX = startX() + (endX() - startX()) * p;
        centerY = startY + (endY - startY) * p;

        // 花にぶつかっている鳥の数だけ、花を飛んでいる向きへ押す
        hits = 0;
        for (int i = 0; i < count; i++) {
            if (hitsFlower(ctx, i)) hits++;
        }
        if (hits > 0) ctx.flower.push(dir * hitForce * hits);

        // ハチドリにぶつかったら、ハチドリだけが弾き飛ばされる（鳥は影響を受けない）
        if (!hitPlayer && ctx.player != null && !ctx.player.isKnockedBack() && hitsPlayer(ctx)) {
            hitPlayer = true;
            double w = ctx.player.width;
            ctx.player.knockback(dir * knockbackX * w, knockbackY * w, knockbackSeconds, knockbackSpins);
        }
    }

    /** i 番目の鳥の中心の x。V 字の並びは、飛ぶ道すじの傾きに合わせて回す。 */
    private double birdX(int i) {
        return centerX + offsetX[i] * Math.cos(tilt) - offsetY[i] * Math.sin(tilt);
    }

    /** i 番目の鳥の中心の y（羽ばたきで少し上下する）。 */
    private double birdY(int i) {
        return centerY + offsetX[i] * Math.sin(tilt) + offsetY[i] * Math.cos(tilt)
                + Math.sin(activeTime() * 8 + phase[i]) * birdHeight * 0.08;
    }

    /** 鳥の当たり判定の半径。 */
    private double birdRadius() {
        return birdWidth * hitScale;
    }

    /**
     * 花（花びらの部分）の当たり判定の中心と半径。
     * 花の画像（stage.json の flower.texture）を使うときは、正面向きの花の真ん中が入り口（flower.x, y）なので、そこを中心にする。
     * 画像を使わない横向きの花は、花びらが入り口から茎の傾きの向きに flowerSize の長さで伸びているので、その真ん中を中心にする。
     */
    private double[] flowerHitCircle(EventContext ctx) {
        double size = ctx.flowerConfig.flowerSize;
        if (!ctx.flowerConfig.texture.isEmpty()) {
            return new double[] {ctx.flower.x, ctx.flower.y, size * flowerHitScale};
        }
        double a = ctx.flower.angle;
        return new double[] {
                ctx.flower.x + Math.cos(a) * size / 2,
                ctx.flower.y + Math.sin(a) * size / 2,
                size * flowerHitScale};
    }

    /** i 番目の鳥が花にぶつかっているか（円どうしが重なっているか）。 */
    boolean hitsFlower(EventContext ctx, int i) {
        double[] f = flowerHitCircle(ctx);
        return Math.hypot(birdX(i) - f[0], birdY(i) - f[1]) < birdRadius() + f[2];
    }

    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        Image img = Assets.image(textureKey);
        for (int i = 0; i < count; i++) {
            // 鳥の真ん中を中心に、飛ぶ道すじの傾きに合わせて回して描く
            gc.save();
            gc.translate(birdX(i), birdY(i));
            gc.rotate(Math.toDegrees(tilt));
            double x = -birdWidth / 2;
            double y = -birdHeight / 2;
            if (img != null && dir > 0) {
                gc.drawImage(img, x + birdWidth, y, -birdWidth, birdHeight); // 左右を反転（右向きにする）
            } else if (img != null) {
                gc.drawImage(img, x, y, birdWidth, birdHeight);
            } else {
                drawFallback(gc, x, y);
            }
            gc.restore();
        }
        if (showHitbox) drawHitboxes(gc, ctx);
        if (warning()) drawAttentionMark(gc);
    }

    // 群れが来る高さの、来る側の端に出す危険マーク（images/attention.png）
    /** 危険マークの画像の中で、絵のある範囲（左・上・幅・高さ。画像は 1080x1080 で、まわりが透明） */
    private static final double[] ATTENTION_SRC = {120, 169, 840, 742};
    /** 危険マークの幅（px）。高さは画像の比で決まる */
    private static final double ATTENTION_WIDTH = 150;
    /** 画面の端からのすき間（px） */
    private static final double ATTENTION_MARGIN = 30;

    /** 予告の間、群れの中心の高さの、来る側の端に危険マークを1つ出す。画面の外から滑り込んできて、点滅する。 */
    private void drawAttentionMark(GraphicsContext gc) {
        Image img = Assets.image(attentionImage);
        if (img == null) return;

        double t = activeTime();
        double w = ATTENTION_WIDTH;
        double h = w * ATTENTION_SRC[3] / ATTENTION_SRC[2];
        double slide = 1.0 - Math.pow(1.0 - Math.clamp(t / 0.25, 0, 1), 3);
        double hidden = (w + ATTENTION_MARGIN) * (1 - slide); // まだ画面の外に隠れている量
        double x = dir < 0
                ? Game.WIDTH - ATTENTION_MARGIN - w + hidden
                : ATTENTION_MARGIN - hidden;

        gc.save();
        gc.setGlobalAlpha(0.8 + 0.2 * Math.sin(t * 16));
        gc.drawImage(img, ATTENTION_SRC[0], ATTENTION_SRC[1], ATTENTION_SRC[2], ATTENTION_SRC[3],
                x, centerY - h / 2, w, h);
        gc.restore();
    }

    /** どれかの鳥がハチドリにぶつかっているか（見えているハチドリの体の中心から playerHitScale × 横幅 の円）。 */
    boolean hitsPlayer(EventContext ctx) {
        double pr = ctx.player.width * playerHitScale;
        Point2D body = ctx.player.getBodyCenter(); // 見た目の位置（アイドル円運動のずれを含む）
        for (int i = 0; i < count; i++) {
            if (Math.hypot(birdX(i) - body.getX(), birdY(i) - body.getY()) < birdRadius() + pr) return true;
        }
        return false;
    }

    /** 当たり判定の円を描く（調整用。stage.json の showHitbox を 1 にすると出る。鳥は赤、花は黄色、ハチドリは青。ぶつかると赤）。 */
    private void drawHitboxes(GraphicsContext gc, EventContext ctx) {
        gc.setLineWidth(4.3);
        gc.setStroke(Color.RED);
        double r = birdRadius();
        for (int i = 0; i < count; i++) {
            gc.strokeOval(birdX(i) - r, birdY(i) - r, r * 2, r * 2);
        }
        double[] f = flowerHitCircle(ctx);
        gc.setStroke(hits > 0 ? Color.RED : Color.YELLOW);
        gc.strokeOval(f[0] - f[2], f[1] - f[2], f[2] * 2, f[2] * 2);
        if (ctx.player != null) {
            double pr = ctx.player.width * playerHitScale;
            gc.setStroke(ctx.player.isKnockedBack() ? Color.RED : Color.BLUE);
            Point2D body = ctx.player.getBodyCenter();
            gc.strokeOval(body.getX() - pr, body.getY() - pr, pr * 2, pr * 2);
        }
    }

    /** 画像が無いときの仮の鳥（左向きの「く」の形）。 */
    private void drawFallback(GraphicsContext gc, double x, double y) {
        gc.setStroke(Color.web("#2b2d42"));
        gc.setLineWidth(Math.max(4.3, birdWidth * 0.06));
        gc.strokePolyline(
                new double[] {x, x + birdWidth * 0.5, x + birdWidth},
                new double[] {y + birdHeight * 0.3, y + birdHeight * 0.7, y + birdHeight * 0.3},
                3);
    }
}
