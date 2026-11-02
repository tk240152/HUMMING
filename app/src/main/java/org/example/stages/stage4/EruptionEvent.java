package org.example.stages.stage4;

import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;
import org.example.media.Sound;

/**
 * 火山の噴火（Stage4.registerEvents で "eruption" として登録している）。
 * ステージが終わるまで動き続け、「休む → ため → 噴火」をくり返す。
 * ため動作中は警告を出し、噴火中は岩が左右から落ち、火山灰で画面が暗くなる。
 * 岩がハチドリに当たると、弾き飛ばして燃えた見た目にする。
 *
 * 火山の山は花・プレイヤーより奥（renderBack）、岩・灰・警告は手前（render）に描く。
 * 休んでいる間も山を描くので、when・duration は書かずに使う（ずっと続くイベント）。
 *
 * ほかのステージで使うときは、StageN.registerEvents で
 * types.register("eruption", EruptionEvent::new); と書き、images・sounds
 * で自分の素材の場所を指定する。
 *
 * <pre>
 * {"type": "eruption", "firstWait": 2, "restSeconds": 10, "chargeSeconds": 2, "eruptSeconds": 10}
 * firstWait     : ステージ開始から、最初のため動作までの秒数。省略すると 2
 * restSeconds   : 噴火が終わってから、次のため動作までの秒数。省略すると 10
 * chargeSeconds : ため動作（山が縮んで青くなる）の秒数。省略すると 2
 * eruptSeconds  : 噴火の秒数。省略すると 10
 * chargedScale  : ため終わったときの山の高さの倍率。省略すると 0.65
 * rocksPerSide  : 1回の噴火で左右それぞれに落ちる岩の数。0 で岩なし。省略すると 10
 * rockDelay     : 噴火から最初の岩が落ちるまでの秒数。省略すると 1
 * rockInterval  : 岩が1個ずつ落ちる間隔（秒）。省略すると 0.5
 * rockSize      : 岩の大きさ（px）。省略すると 86
 * fallSpeed     : 岩が落ちる速さ（1ステップあたりの px）。省略すると 6.5
 * 岩の落下X座標は RockFall の定数で設定（左は400以上961未満、右は961以上1520未満）。
 * hitRadius     : 岩がハチドリに当たる判定の、ハチドリ側の半径（px）。省略すると 43
 * knockback     : 岩に当たったときに弾き飛ばす強さ（1ステップあたりの px）。省略すると 6
 * hurtSeconds   : 当たったあと燃えた見た目になり、次の岩に当たらない秒数。省略すると 0.6
 * ashOpacity    : 火山灰のいちばん濃いときの濃さ（0.0〜1.0）。0 で灰なし。省略すると 0.85
 * ashColor      : 火山灰の色。省略すると "#3c3d3d"
 * ashFadeIn     : 灰が濃くなる秒数。省略すると 0.5
 * ashFadeOut    : 噴火が終わってから灰が晴れる秒数。省略すると 2
 * attentionImage: ため動作中に画面上部へ3つ並べる危険マークの画像（images/ のファイル名）。省略すると "attention"
 * warnSe        : 警告が出たとき（ため動作の始まり）に鳴らす音（Sound の名前）。省略すると "SE_Alert"。"" なら鳴らさない
 * sounds        : 音の場所。省略すると "sounds/se/"
 * eruptionSound・fallingSound・hitSound : 音のファイル名（拡張子つき）。
 *                 省略すると eruption.wav・rock_falling.wav・rock_hit.wav。"" でその音を鳴らさない
 * </pre>
 */
public class EruptionEvent extends StageEvent {

    private final double firstWait = Math.max(0, number("firstWait", 2));
    private final double restSeconds = Math.max(0, number("restSeconds", 10));
    private final String warnSe = string("warnSe", "SE_Alert");

    // 噴火時に画面を揺らす時間（秒）。0なら揺らさない。
    private final double shakeSeconds = Math.max(0.0, number("shakeSeconds", 0.4));
    private final ScreenShake screenShake = new ScreenShake();

    // 横方向・縦方向の揺れの最大幅（px）。省略時は8・5を使用する。
    private final double shakeAmplitudeX = Math.max(0.0, number("shakeAmplitudeX", 8.0));
    private final double shakeAmplitudeY = Math.max(0.0, number("shakeAmplitudeY", 5.0));

    private final Volcano volcano;
    private final VolcanicAsh ash;
    private final RockFall rockFall;
    private final Stage4HitEffect hitEffect;
    private final EruptionAudio audio;
    private final EruptionWarning warning;

    // 通常状態での経過時間（秒）。
    private double waitTime = 0.0;

    // まだ最初の噴火を開始していないかどうか。
    private boolean firstEruption = true;

    // Goに合わせた開始音を、すでに鳴らしたか。
    private boolean startSoundPlayed = false;

    public EruptionEvent(Map<String, Object> params) {
        super(params);
        String images = string("images", "");
        String sounds = string("sounds", "sounds/se/");

        volcano = new Volcano(images,
                number("chargeSeconds", 2),
                number("eruptSeconds", 10),
                number("chargedScale", 0.65));
        ash = new VolcanicAsh(
                Color.web(string("ashColor", "#3c3d3d")),
                number("ashOpacity", 0.85),
                number("ashFadeIn", 0.5),
                number("ashFadeOut", 2));
        rockFall = new RockFall(
                number("rockDelay", 1),
                number("rockInterval", 0.5),
                (int) number("rocksPerSide", 10),
                number("hitRadius", 43),
                number("rockSize", 86),
                number("fallSpeed", 6.5),
                images + "falling_rock");
        hitEffect = new Stage4HitEffect(
                number("knockback", 6),
                number("hurtSeconds", 0.6));
        audio = new EruptionAudio(sounds,
                string("eruptionSound", "eruption.wav"),
                string("fallingSound", "rock_falling.wav"),
                string("hitSound", "rock_hit.wav"));
        warning = new EruptionWarning(string("attentionImage", "attention"));
    }

    @Override
    protected void onStart(EventContext ctx) {
        // ステージ開始時から、最初の噴火までの時間を数える。
        waitTime = 0.0;
        firstEruption = true;
        screenShake.reset();
        audio.start();
        audio.prepareStart(string("startSound", ""));
        startSoundPlayed = false; // 岩に当たったときの燃えた見た目は、Player の外見の差し替えで描く。
        if (ctx.player != null) {
            ctx.player.setAppearanceRenderer(hitEffect::render);
        }
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        // Ready中はゲーム時間が0。Goで時間が進み始めたら一度だけ鳴らす。
        if (!startSoundPlayed && ctx.time > 0.0) {
            startSoundPlayed = true;
        }

        hitEffect.update();
        audio.updateVolumes();
        screenShake.update();

        // 通常状態の間だけ、次の噴火までの時間を数える。
        if (volcano.isIdle()) {
            waitTime += Game.STEP;

            // 初回は短く待ち、二回目以降は通常の間隔を使用する。
            double waitSeconds = firstEruption ? firstWait : restSeconds;

            // 待ち時間が過ぎたら、ため動作を始める。
            if (waitTime + 1.0e-9 >= waitSeconds) {
                volcano.startCharging();
                if (!warnSe.isEmpty())
                    Sound.longSe(warnSe); // ため動作（警告が出ている間）の始まりに警告音
                waitTime = 0.0;

                // 次回から通常の待機時間を使用する。
                firstEruption = false;
            }
        } else {
            // ため動作中と噴火中は、待機時間を数えない。
            waitTime = 0.0;
        }

        boolean hadBurstStarted = volcano.hasBurstStarted();
        volcano.update();

        // 最後の押し込みが終わり、跳ね戻る瞬間に音と画面揺れを開始する。
        if (!hadBurstStarted && volcano.hasBurstStarted()) {
            audio.playEruption();
            screenShake.start(shakeSeconds, shakeAmplitudeX, shakeAmplitudeY);
        }

        ash.update(volcano.isErupting());
        warning.update(volcano.isCharging());

        boolean hitAccepted = rockFall.update(
                volcano.isErupting(),
                volcano.eruptionTime(),
                ctx.player,
                hitEffect);

        if (hitAccepted) {
            Sound.se("damage");
        }

        // 山体が跳ね戻り始めてから、噴火終了まで轟音を鳴らす。
        audio.setRumbling(volcano.hasBurstStarted());
    }

    @Override
    public void onPause(EventContext ctx) {
        audio.pause();
    }

    @Override
    public void onResume(EventContext ctx) {
        audio.resume();
    }

    @Override
    protected void onEnd(EventContext ctx) {
        // ステージを抜けるときなど。変えたものを元に戻す
        audio.stop();
        rockFall.clear();
        hitEffect.reset();
        screenShake.reset();
        if (ctx.player != null) {
            ctx.player.setAppearanceRenderer(null);
        }
    }

    /** 花とプレイヤーの下に火山を描く。 */
    @Override
    public void renderBack(GraphicsContext gc, EventContext ctx) {
        volcano.render(gc);
    }

    /** 岩と火山灰を描き、最後に警告を重ねる。 */
    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        rockFall.render(gc);
        ash.render(gc);
        warning.render(gc);
    }

    /** いま噴火中か（テスト・ほかの処理から見る用）。 */
    public boolean isErupting() {
        return volcano.isErupting();
    }

    /** いまため動作中か（テスト・ほかの処理から見る用）。 */
    public boolean isCharging() {
        return volcano.isCharging();
    }

    /** 噴火による画面揺れの横方向のずれ（px）を返す。 */
    @Override
    public double screenOffsetX() {
        return screenShake.offsetX();
    }

    /** 噴火による画面揺れの縦方向のずれ（px）を返す。 */
    @Override
    public double screenOffsetY() {
        return screenShake.offsetY();
    }
}
