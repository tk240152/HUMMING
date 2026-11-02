package org.example.game.event;

import java.util.Map;
import java.util.function.Predicate;

import javafx.scene.canvas.GraphicsContext;

import org.example.core.Game;
import org.example.game.util.Json;

/**
 * イベント（ギミック）の親クラス。新しいイベントはこれを継承して、必要なメソッドだけ書く。
 *
 * <pre>
 * onStart  : 始まったときに1回
 * onUpdate : 続いている間、毎ステップ
 * onEnd    : 終わったときに1回（変えたものを元に戻す）
 * render     : 続いている間、毎フレーム描く。花・プレイヤーより手前（霧・エネミーなど）
 * renderBack : 続いている間、毎フレーム描く。背景のすぐ上で、花・プレイヤーより奥（火山の山など）
 * </pre>
 *
 * JSON の共通の項目:
 * 
 * <pre>
 * "type"     : イベントの種類（EventTypes に登録した名前）
 * "when"     : いつ始まるか。省略するとすぐに始まる
 *              {"time": 秒}          開始から○秒後に1回
 *              {"nectar": %}         満腹度が○%を超えたら1回
 *              {"every": 秒}         吹き始めから○秒ごとにくり返す（最初は○秒後。"from": 秒 で最初の時刻を変えられる）
 *              {"rest": 秒}          終わってから○秒休んで、またくり返す（最初は○秒後。"from": 秒 で変えられる）
 * "duration" : 続く秒数。0 や省略だとステージが終わるまで続く（くり返すときは必ず書く）
 * </pre>
 */
public abstract class StageEvent {

    private enum Phase {
        WAITING, ACTIVE, DONE
    }

    /** JSON に書かれた項目（種類ごとの値は number() / string() で読む） */
    protected final Map<String, Object> params;
    protected final String type;
    protected final double duration;
    private final Predicate<EventContext> trigger;

    /** 始まりから次の始まりまでの秒数（every）。0 なら使わない */
    private final double every;
    /** 終わってから次の始まりまでの秒数（rest）。0 なら使わない */
    private final double rest;
    /** くり返すとき、次に始まる時刻（秒） */
    private double nextTime;

    private Phase phase = Phase.WAITING;
    private double activeTime;

    protected StageEvent(Map<String, Object> params) {
        this.params = params;
        this.type = Json.string(params, "type", "?");
        this.duration = Math.max(0, Json.number(params, "duration", 0));

        Map<String, Object> when = Json.object(params, "when");
        this.every = Math.max(0, Json.number(when, "every", 0));
        this.rest = Math.max(0, Json.number(when, "rest", 0));
        this.nextTime = Json.number(when, "from", every > 0 ? every : rest);
        this.trigger = repeats() ? ctx -> ctx.time >= nextTime - EPSILON : trigger(when);
    }

    /** 小数の誤差で1ステップ遅れないための余裕 */
    private static final double EPSILON = 1e-6;
    /** 今回始まった時刻（秒） */
    private double startTime;

    private boolean repeats() {
        return every > 0 || rest > 0;
    }

    // ---- 継承したクラスで書くもの（必要なものだけ） ----

    protected void onStart(EventContext ctx) {
    }

    protected void onUpdate(EventContext ctx) {
    }

    protected void onEnd(EventContext ctx) {
    }

    /** ゲームの一時停止を通知する。 */
    public void onPause(EventContext ctx) {
    }

    /** ゲームの再開を通知する。 */
    public void onResume(EventContext ctx) {
    }

    public void render(GraphicsContext gc, EventContext ctx) {
    }

    public void renderBack(GraphicsContext gc, EventContext ctx) {
    }

    /** 描画時の横方向のずれ（px）を返す。通常はずらさない。 */
    public double screenOffsetX() {
        return 0.0;
    }

    /** 描画時の縦方向のずれ（px）を返す。通常はずらさない。 */
    public double screenOffsetY() {
        return 0.0;
    }
    // ---- 継承したクラスで使える道具 ----

    protected double number(String key, double def) {
        return Json.number(params, key, def);
    }

    protected String string(String key, String def) {
        return Json.string(params, key, def);
    }

    /** 始まってからの秒数。 */
    protected double activeTime() {
        return activeTime;
    }

    /**
     * 始まり・終わりを滑らかにするための 0〜1 の値（fade 秒かけて 0→1、終わる前に 1→0）。
     * duration が 0（ずっと続く）なら、始まりだけ滑らかにする。
     */
    protected double fade(double fade) {
        if (fade <= 0)
            return 1;
        double in = activeTime / fade;
        double out = duration > 0 ? (duration - activeTime) / fade : 1;
        return Math.clamp(Math.min(in, out), 0, 1);
    }

    // ---- StageEvents から呼ばれる ----

    public boolean isActive() {
        return phase == Phase.ACTIVE;
    }

    void step(EventContext ctx) {
        if (phase == Phase.WAITING && trigger.test(ctx)) {
            phase = Phase.ACTIVE;
            activeTime = 0;
            startTime = ctx.time;
            if (every > 0)
                nextTime += every;
            System.out.println("[Event] " + type + " 開始 (" + String.format("%.2f", ctx.time) + "秒)");
            onStart(ctx);
        }
        if (phase != Phase.ACTIVE)
            return;

        onUpdate(ctx);
        activeTime += Game.STEP;
        if (duration > 0 && activeTime >= duration - EPSILON)
            finish(ctx);
    }

    void finish(EventContext ctx) {
        if (phase != Phase.ACTIVE)
            return;
        phase = repeats() ? Phase.WAITING : Phase.DONE; // くり返すなら次を待つ
        // rest: 終わった時刻（始まり + duration）から rest 秒後に次を始める。
        // duration が無いまま途中で終わらせたとき（画面を抜けたときなど）は、今の時刻から数える
        if (rest > 0)
            nextTime = (duration > 0 ? startTime + duration : ctx.time) + rest;
        onEnd(ctx);
        System.out.println("[Event] " + type + " 終了");
    }

    /**
     * "when" から始まる条件を作る。きっかけの種類を増やすときはここに case を足す。
     */
    static Predicate<EventContext> trigger(Map<String, Object> when) {
        if (when == null || when.isEmpty())
            return ctx -> true;
        if (when.get("time") instanceof Double t)
            return ctx -> ctx.time >= t;
        if (when.get("nectar") instanceof Double n)
            return ctx -> ctx.nectarPercent() >= n;
        System.err.println("[Event] 知らない when です: " + when.keySet() + "（このイベントは起きません）");
        return ctx -> false;
    }
}
