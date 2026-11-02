package org.example.stages.stage4;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.example.media.Assets;
import org.example.core.Game;

/**
 * 火山（EruptionEvent の部品）。通常 → ため → 噴火 → 通常 と動き、山を描く。
 * 秒数や画像の場所は EruptionEvent から受け取る（stage.json で変えられる）。
 */
public class Volcano {

    /** 火山の現在の状態。 */
    private enum State {
        NORMAL,
        CHARGING,
        ERUPTING
    }

    // 各動作の長さ（秒）。
    private final double chargeSeconds;
    private final double eruptionSeconds;

    // ため動作が完了したときの高さの倍率。
    private final double chargedScale;

    // 画像の置き場所（"" なら共通の images/）。拡張子はAssets側で付ける。
    private final String imageFolder;

    // 噴火開始時に、山体が元の高さへ跳ね戻る時間（秒）。
    private static final double REBOUND_SECONDS = 0.38;

    // 跳ね戻りの強さ。大きいほど元の高さを大きく超える。
    private static final double REBOUND_STRENGTH = 1.70158;

    // 噴火直前に、山体をもう一段押し込む時間（秒）。
    private static final double ERUPTION_PRESS_SECONDS = 0.10;

    // 正式な噴煙が広がる時間（秒）。
    private static final double ERUPTION_RISE_SECONDS = 0.28;

    // 噴煙が広がった後の、高さの揺れ幅。
    private static final double SMOKE_PULSE_AMOUNT = 0.025;

    // 噴煙が一往復する時間（秒）。
    private static final double SMOKE_PULSE_PERIOD = 0.9;

    // 1920×1080の素材上で、噴煙の根元を固定する高さ。
    private static final double ERUPTION_ANCHOR_Y = 440.0;

    // 噴火終了後、噴煙が消えるまでの時間（秒）。
    private static final double SMOKE_FADE_SECONDS = 1.5;

    // 噴煙の退場演出に残っている時間（秒）。
    private double smokeRemaining = 0;

    private State state = State.NORMAL;

    // 現在の状態になってからの経過時間（秒）。
    private double stateTime = 0;

    public Volcano(String imageFolder, double chargeSeconds,
            double eruptionSeconds, double chargedScale) {
        this.imageFolder = imageFolder;
        this.chargeSeconds = Math.max(0.01, chargeSeconds);
        this.eruptionSeconds = Math.max(0.01, eruptionSeconds);
        this.chargedScale = chargedScale;
    }

    /**
     * 画像を描くときに読み込む（Assets が覚えておくので2回目からは読み直さない）。
     * 作った瞬間に読むと、画面のないテスト（gradlew build）で JavaFX が画像を作れずに止まるため。
     * 画像が無いときは null を返し、その部分は描かない。
     */
    private Image image(String name) {
        Image image = Assets.image(imageFolder + name);
        return image == null || image.isError() ? null : image;
    }

    /** 通常状態で、次の噴火を開始できるかどうかを返す。 */
    public boolean isIdle() {
        return state == State.NORMAL;
    }

    /** 現在、噴火前のため動作中かどうかを返す。 */
    public boolean isCharging() {
        return state == State.CHARGING;
    }

    /** 現在、噴火中かどうかを返す。 */
    public boolean isErupting() {
        return state == State.ERUPTING;
    }

    /** 最後の押し込みを終え、噴煙が出始めたかを返す。 */
    public boolean hasBurstStarted() {
        return isErupting()
                && stateTime + 1.0e-9 >= ERUPTION_PRESS_SECONDS;
    }

    /** 噴火が始まってからの経過時間（秒）を返す。 */
    public double eruptionTime() {
        return isErupting() ? stateTime : 0;
    }

    /** 通常時にだけ、噴火前のため動作を開始する。 */
    public void startCharging() {
        if (state != State.NORMAL) {
            return;
        }

        changeState(State.CHARGING);
    }

    /** 状態を切り替え、その状態の経過時間をリセットする。 */
    private void changeState(State next) {
        state = next;
        stateTime = 0;
    }

    /** 毎ステップ、現在の状態と経過時間を更新する。 */
    public void update() {

        smokeRemaining = Math.max(0, smokeRemaining - Game.STEP);

        if (state == State.NORMAL) {
            return;
        }

        stateTime += Game.STEP;

        switch (state) {
            case CHARGING -> {
                if (stateTime + 1.0e-9 >= chargeSeconds) {
                    changeState(State.ERUPTING);
                }
            }

            case ERUPTING -> {
                if (stateTime + 1.0e-9 >= eruptionSeconds) {
                    smokeRemaining = SMOKE_FADE_SECONDS;
                    changeState(State.NORMAL);
                }
            }
            case NORMAL -> {
            }
        }
    }

    /** 山体だけを変形し、湖岸は固定したまま重ねる。 */
    public void render(GraphicsContext gc) {
        renderSky(gc);
        double scaleY = mountainScaleY();

        // 噴煙を先に描き、根元を山体で覆う。
        if (isErupting() || smokeRemaining > 0) {
            renderEruption(gc, scaleY);
        }

        gc.save();
        // 山のふもとを固定し、高さだけを変える。
        gc.translate(0, Game.HEIGHT);
        gc.scale(1, scaleY);
        gc.translate(0, -Game.HEIGHT);

        drawLayer(gc, image("Tex_StageBG_MtFuji"));

        gc.restore();

        // 山体の変形を引き継がず、湖岸を固定して描く。
        gc.save();
        drawLayer(gc, image("Tex_StageBG_MtFuji_Lake"));
        gc.restore();
    }

    /** 現在の状態に応じた山体の高さ倍率を返す。 */
    private double mountainScaleY() {
        if (state == State.CHARGING) {
            double progress = Math.min(1.0, stateTime / chargeSeconds);

            // ゆっくり動き始め、ため終わりも滑らかに止める。
            double compression = progress * progress * (3.0 - 2.0 * progress);

            return 1.0 - (1.0 - chargedScale) * compression;
        }

        if (state == State.ERUPTING) {
            double pressedScale = Math.max(0.1, chargedScale - 0.04);

            // 最初の0.10秒で、ためた位置からさらに押し込む。
            if (stateTime < ERUPTION_PRESS_SECONDS) {
                double progress = stateTime / ERUPTION_PRESS_SECONDS;
                double eased = progress * progress * (3.0 - 2.0 * progress);

                return chargedScale
                        + (pressedScale - chargedScale) * eased;
            }

            // 押し込み終わった位置から、一気に跳ね戻る。
            double progress = Math.min(
                    1.0,
                    (stateTime - ERUPTION_PRESS_SECONDS) / REBOUND_SECONDS);
            double offset = progress - 1.0;

            double rebound = 1.0
                    + (REBOUND_STRENGTH + 1.0)
                            * offset * offset * offset
                    + REBOUND_STRENGTH * offset * offset;

            return pressedScale + (1.0 - pressedScale) * rebound;
        }

        return 1.0;
    }

    /** ステージ全体を通して、空を茶褐色に保つ。 */
    private void renderSky(GraphicsContext gc) {
        gc.save();

        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.rgb(116, 50, 38));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        gc.restore();
    }

    /** 透明な余白も含め、画像全体をゲーム画面に合わせて描く。 */
    private void drawLayer(GraphicsContext gc, Image layer) {
        if (layer == null) {
            return;
        }

        gc.drawImage(layer, 0, 0, Game.WIDTH, Game.HEIGHT);
    }

    /** 噴煙を広げ、噴火終了後は上へ流しながら消す。 */
    private void renderEruption(GraphicsContext gc, double scaleY) {
        Image eruption = image("Tex_StageBG_MtFuji_Eruption");
        if (eruption == null) {
            return;
        }

        double growth;
        double opacity = 1.0;
        double rise = 0.0;

        if (isErupting()) {
            // 山体を押し込む0.10秒が終わってから噴煙を出す。
            double progress = Math.clamp(
                    (stateTime - ERUPTION_PRESS_SECONDS)
                            / ERUPTION_RISE_SECONDS,
                    0.0, 1.0);

            growth = 1.0 - Math.pow(1.0 - progress, 3.0);

            // 噴煙が完全に広がってから、揺れを始める。
            double pulseTime = Math.max(
                    0.0,
                    stateTime - ERUPTION_PRESS_SECONDS - ERUPTION_RISE_SECONDS);

            // 揺れは徐々に強まり、噴火終了前には静かに収まる。
            double blend = Math.clamp(
                    Math.min(pulseTime, eruptionSeconds - stateTime) / 0.4,
                    0.0, 1.0);
            blend = blend * blend * (3.0 - 2.0 * blend);

            double pulse = Math.sin(
                    pulseTime * 2.0 * Math.PI / SMOKE_PULSE_PERIOD);

            growth *= 1.0 + SMOKE_PULSE_AMOUNT * blend * pulse;
        } else {
            if (smokeRemaining <= 0) {
                return;
            }

            // 退場は、噴煙に残された時間を使って進める。
            double progress = 1.0 - smokeRemaining / SMOKE_FADE_SECONDS;
            double eased = progress * progress * (3.0 - 2.0 * progress);

            growth = 1.0;
            opacity = 1.0 - eased;
            rise = Game.HEIGHT * 0.07 * eased;
            scaleY = 1.0;
        }

        if (growth <= 0.0) {
            return;
        }

        double normalAnchorY = Game.HEIGHT * ERUPTION_ANCHOR_Y / 1080.0;

        double currentAnchorY = Game.HEIGHT
                + (normalAnchorY - Game.HEIGHT) * scaleY;

        gc.save();

        gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
        gc.translate(0, currentAnchorY - rise);
        gc.scale(1, growth);
        gc.translate(0, -normalAnchorY);

        drawLayer(gc, eruption);

        gc.restore();
    }
}