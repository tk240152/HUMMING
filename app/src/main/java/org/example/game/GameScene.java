package org.example.game;

import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.example.media.Assets;
import org.example.core.BaseScene;
import org.example.core.FadeInScene;
import org.example.core.Game;
import org.example.state.GameState;
import org.example.core.DevMode;
import org.example.core.Input;
import org.example.state.Records;
import org.example.media.Sound;
import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;
import org.example.game.event.StageEvents;
import org.example.player.Player;
import org.example.ui.Hud;
import org.example.ui.PauseMenu;
import org.example.ui.result.ResultScene;
import org.example.ui.stageselect.StageSelectScene;
import org.example.game.util.DebugOverlay;
import org.example.game.flower.Feeding;
import org.example.game.flower.FeedingEffect;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.util.GameTimer;
import org.example.game.stage.StageConfig;
import org.example.game.stage.StageScript;
import org.example.game.flower.Target;
import org.example.game.flower.Wind;

/**
 * ゲーム画面（担当: それ以外）。
 * 各担当の部品を呼ぶ順番と描く順番だけをここで決める。中身は各担当のクラスに書く。
 */
public class GameScene implements BaseScene {

    private final GameState state = new GameState();
    private final Player player = new Player();
    private final Hud hud = new Hud();
    private final DebugOverlay debug = new DebugOverlay();

    private final Wind wind = new Wind();
    private final Feeding feeding = new Feeding();
    private final FeedingEffect feedingEffect = new FeedingEffect();
    private final GameTimer timer = new GameTimer();
    private final Target target = new Target();

    private final PauseMenu pauseMenu = new PauseMenu();
    private boolean paused;
    private double bgmVolumeBeforePause;

    // ============================================================
    // 開始演出: 暗転からフェードインし、「Ready...」で待機、「Go!」でゲーム開始
    // ============================================================
    private double startElapsed = 0.0;
    private static final double FADE_IN_SECONDS = 0.5; // 黒画面から明るくなるまでの時間
    private static final double READY_SECONDS = 1.2; // 「Ready...」でゲームを止めておく時間
    private static final double START_TEXT_SCALE = 1.0; // Ready・Go の画像を描く倍率（1.0 で画像のまま）
    private static final double GO_SECONDS = 1.0; // ゲーム開始後、「Go!」を表示しておく時間

    /** BGMとイベントの音を一時停止する。 */
    private void pause() {
        paused = true;
        events.pause(eventContext);
        Sound.pauseBgm(); // BGMを一時停止
        Sound.pauseSe(); // 風などの長い効果音も一時停止
        Sound.se("pause_on");
        pauseMenu.open();
    }

    /** 続ける場合は、音も再開する。 */
    private void resume() {
        resume(true);
    }

    /** 画面を切り替える場合は、音を再開せずメニューを閉じる。 */
    private void resume(boolean resumeAudio) {
        if (!paused) {
            return;
        }

        paused = false;

        if (resumeAudio) {
            Sound.resumeBgm(); // BGMを再開
            Sound.resumeSe(); // 風などの長い効果音も続きから
            events.resume(eventContext);
        }
        Sound.se("pause_off"); // 再開・リトライ・ステージ選択のどれで閉じても鳴らす

        pauseMenu.close();
    }

    // ステージごとに変わるもの（stages/stage{番号}/ の stage.json と StageN クラスから作る）
    private final int stageNumber;
    private final StageConfig stage;
    private final StageScript script;
    private final FlowerConfig flowerConfig;
    private final Flower flower;
    private final StageEvents events;
    private final EventContext eventContext;

    /** 直接起動した場合はステージ1を開始する。 */
    public GameScene() {
        this(1);
    }

    /** 選択画面からステージ番号を受け取る。 */
    public GameScene(int stageNumber) {
        this.stageNumber = stageNumber;
        state.stage = stageNumber;

        stage = StageConfig.load(stageNumber);
        flowerConfig = stage.flower;
        flower = new Flower(flowerConfig);
        script = StageScript.load(stageNumber);
        EventTypes types = EventTypes.common();
        script.registerEvents(types); // そのステージだけのイベントを足す
        events = StageEvents.create(stage.events, types);
        eventContext = new EventContext(state, player, flower, flowerConfig, wind);
        state.rankTimes = stage.rankTimes.clone();
    }

    @Override
    public void onEnter() {
        // ゲーム画面の P は画面送りではなく、クリアにする（update で扱う）
        enterToNextBefore = Game.enterToNext;
        Game.enterToNext = false;
        script.onStart(eventContext);
    }

    /** 入る前の Game.enterToNext（出るときに戻す） */
    private boolean enterToNextBefore;

    /** stage.json の bgm（Game が画面に入ったときに流す） */
    @Override
    public String bgm() {
        return stage.bgmKey();
    }

    @Override
    public void update() {
        // クリア演出中は操作を受け付けず、演出だけを進める
        if (clearing) {
            updateClearing();
            return;
        }

        // Esc で一時停止
        if (!paused && Input.isPressed(KeyCode.ESCAPE)) {
            pause();
            return;
        }

        // 一時停止中は、メニューの操作だけを受け付けて、ゲームの更新はすべて止める
        if (paused) {
            switch (pauseMenu.update()) {
                case RESUME -> resume();
                case RETRY -> {
                    resume(false);
                    Game.change(new GameScene(stageNumber));
                }
                case STAGE_SELECT -> {
                    resume(false);
                    Game.change(new StageSelectScene());
                }
                case NONE -> {
                }
            }
            return;
        }

        // ★ 開始演出の進行と待機処理
        if (startElapsed < READY_SECONDS + GO_SECONDS) {
            double before = startElapsed;
            startElapsed += Game.STEP;

            // 演出開始時にサウンドを1回だけ再生
            if (before == 0) {
                Sound.se("SE_Ready");
            }
        }
        if (startElapsed < READY_SECONDS) {
            // ★ 修正: タイマーとプレイヤーは止めたまま、ステージのギミックや花などを更新して表示する
            events.update(eventContext, timer.seconds()); // 0秒時点のイベント（ギミック出現など）を発火
            script.update(eventContext); // ステージ専用ギミックの更新
            wind.update(flowerConfig.swaySpeed);
            flower.update(wind.value());
            state.maxNectar = flowerConfig.clearNectar;
            player.writeTo(state);
            target.update(state, flower.hitRadius(), flowerConfig.guideDistance);
            return;
        }

        if (DevMode.ENABLED && Input.isPressed(KeyCode.F3)) // 開発用
            debug.toggle();
        if (DevMode.ENABLED && Input.isPressed(KeyCode.O)) // 開発用
            Sound.se("debug"); // 効果音の確認用

        if (DevMode.ENABLED && Input.isPressed(KeyCode.R)) { // 開発用
            stage.reload(); // Player の設定と同じく R キーで読み直す（花の設定。イベントは次のプレイから）
            state.rankTimes = stage.rankTimes.clone();
        }
        if (DevMode.ENABLED && Input.isPressed(KeyCode.P)) { // 開発用: その時のタイムで強制的にクリア
            startClearing();
            return;
        }

        debug.update();

        updateWorld();

        // 満腹度がクリアの値に達したら、クリア演出を始める（終わったらリザルトへ）
        if (state.status == GameState.Status.CLEAR) {
            startClearing();
        }
    }

    /** 1ステップ分、ゲームの中の物（プレイヤー・花・イベントなど）を動かす。 */
    private void updateWorld() {
        timer.update();
        player.update();
        events.update(eventContext, timer.seconds()); // ステージのイベント（風・花・プレイヤーなどを動かす）
        script.update(eventContext); // そのステージだけの処理
        feeding.setBlocked(eventContext.isFeedingBlocked());
        wind.update(flowerConfig.swaySpeed);
        flower.update(wind.value());
        state.maxNectar = flowerConfig.clearNectar;
        feeding.update(player.getBeakTip(), flower, flowerConfig.nectarRate, flowerConfig.guideDistance, state);
        // 吸蜜エフェクト（蜜の粒）。クリア演出中も吸い続けている姿を見せる
        feedingEffect.update(state.status == GameState.Status.FEEDING || clearing, flower.x, flower.y,
                player.getBodyCenter(), player.getBeakTip());
        player.writeTo(state);
        target.update(state, flower.hitRadius(), flowerConfig.guideDistance); // 判定は Feeding の結果を使う
    }

    // ============================================================
    // クリア演出: スローになりながらハチドリへ3段階で寄り（寄るたびに白いビネットと集中線が一瞬出る）、
    // 白くなってリザルトへ（リザルトは白からだんだん見えてくる）
    // ============================================================

    /** 演出中の動きの速さ（1 が通常。0.25 なら 1/4 の速さ） */
    private static final double CLEAR_SLOW = 0.25;

    /** カット1回分。start 秒（クリアした瞬間から）に、zoom 倍へぱっと寄り、power の強さの効果を出す */
    private record Cut(double start, double zoom, CutEffect.Power power) {
    }

    /** カットの一覧（start の早い順）。行を足せばカットが増える */
    private static final Cut[] CUTS = {
            new Cut(0.0, 1.5, CutEffect.Power.WEAK),
            new Cut(1.0, 2.5, CutEffect.Power.STRONG),
            new Cut(2.0, 4.0, CutEffect.Power.STRONGEST),
    };

    /** 白くなり始める秒数と、リザルトへ移る秒数（どちらもクリアした瞬間から） */
    private static final double CLEAR_FADE_START = 3.0;
    private static final double CLEAR_SECONDS = 3.5;
    /** リザルトが白からだんだん見えてくる秒数 */
    private static final double CLEAR_FADE_IN_SECONDS = 0.5;
    /** カットのたびに鳴らす音（シャッター音。sounds/se/SE_Shutter） */
    private static final String CUT_SE = "SE_Shutter";

    private boolean clearing;
    private double clearElapsed;
    /** スローで進めるときの、たまった進み具合（1 たまるごとに1ステップ動かす） */
    private double slowCarry;
    /** カットの瞬間の白いビネットと集中線（あとのカットほど強く） */
    private final CutEffect cutEffect = new CutEffect();
    /** 今のカットの番号（CUTS の何番目か） */
    private int cutIndex;

    /** クリアした瞬間に1回だけ呼ぶ。タイムを記録して、演出を始める。 */
    private void startClearing() {
        timer.stop();
        // クリアタイムを記録して、ベストを更新したかをリザルト画面に渡す
        state.clearTime = timer.seconds();
        state.newBest = Records.submit(stageNumber, state.clearTime);
        state.bestTime = Records.best(stageNumber);
        state.backgroundKey = stage.backgroundKey(); // リザルト画面の背景にする

        clearing = true;
        clearElapsed = 0;
        slowCarry = 0;
        cutIndex = 0;
        cutEffect.start(CUTS[0].power()); // 1回目のカット
        Sound.se(CUT_SE);
        Input.setBlocked(true); // 演出中は操作できない（Esc・P も効かない）
    }

    private void updateClearing() {
        clearElapsed += Game.STEP;
        // 次のカットの時間になったら、カットを進めて効果を出す
        if (cutIndex + 1 < CUTS.length && clearElapsed >= CUTS[cutIndex + 1].start()) {
            cutIndex++;
            cutEffect.start(CUTS[cutIndex].power());
            Sound.se(CUT_SE);
        }
        cutEffect.update(); // 効果はスローにせず、すぐ消す
        // CLEAR_SLOW の速さで動かす（0.25 なら 4ステップに1回だけゲームの中を進める）
        slowCarry += CLEAR_SLOW;
        while (slowCarry >= 1) {
            updateWorld();
            slowCarry -= 1;
        }
        if (clearElapsed >= CLEAR_SECONDS) {
            // 真っ白になったところでリザルトへ。リザルトは白から CLEAR_FADE_IN_SECONDS 秒かけて見えてくる
            Game.change(new FadeInScene(new ResultScene(state), Color.WHITE, CLEAR_FADE_IN_SECONDS));
        }
    }

    /**
     * ゲームの中を描く前に、ハチドリへ寄る拡大をかける（カットごとに、ぱっと切り替わる）。
     * 画面の外（何も描いていない所）が見えないよう、見える範囲は画面の中に収める。
     */
    private void applyClearCamera(GraphicsContext gc) {
        double zoom = clearZoom();
        gc.translate(Game.WIDTH / 2.0, Game.HEIGHT / 2.0);
        gc.scale(zoom, zoom);
        gc.translate(-clearCameraX(zoom), -clearCameraY(zoom));
    }

    private double clearZoom() {
        return CUTS[cutIndex].zoom();
    }

    /** 拡大したときに画面の真ん中に来る位置。ハチドリの体の中心（画面の外が見えないよう、画面の中に収める）。 */
    private double clearCameraX(double zoom) {
        double half = Game.WIDTH / 2.0 / zoom;
        return Math.clamp(player.getBodyCenter().getX(), half, Game.WIDTH - half);
    }

    private double clearCameraY(double zoom) {
        double half = Game.HEIGHT / 2.0 / zoom;
        return Math.clamp(player.getBodyCenter().getY(), half, Game.HEIGHT - half);
    }

    /** カットの効果を描く。集中線は、拡大した画面の上でハチドリがいる所へ集める。 */
    private void renderCutEffect(GraphicsContext gc) {
        double zoom = clearZoom();
        var bird = player.getBodyCenter();
        double x = Game.WIDTH / 2.0 + (bird.getX() - clearCameraX(zoom)) * zoom;
        double y = Game.HEIGHT / 2.0 + (bird.getY() - clearCameraY(zoom)) * zoom;
        cutEffect.render(gc, x, y);
    }

    /** 最後に画面を白くする。 */
    private void renderClearFade(GraphicsContext gc) {
        double a = Math.clamp((clearElapsed - CLEAR_FADE_START) / (CLEAR_SECONDS - CLEAR_FADE_START), 0, 1);
        if (a <= 0)
            return;
        gc.setFill(Color.color(1, 1, 1, a));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
    }

    @Override
    public void onExit() {
        // P キーでも、クリアで自動的に移るときも、画面を抜けたら止める
        resume(false);
        Game.enterToNext = enterToNextBefore;
        Input.setBlocked(false); // クリア演出で止めた操作を戻す
        Sound.stopBgm();
        Sound.stopSe(); // 鳴りかけの風などの音も止める（リトライ・ステージ選択へ移るとき）
        script.onExit(eventContext);
        events.endAll(eventContext);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        if (clearing) {
            // クリア演出中はハチドリへ寄る。
            applyClearCamera(gc);
        } else {
            // 背景から手前のイベントまで、描画位置をまとめてずらす。
            events.applyScreenOffset(gc);
        }
        Assets.draw(gc, stage.backgroundKey(), 0, 0, Game.WIDTH, Game.HEIGHT, Color.SKYBLUE); // 背景
        script.renderBack(gc, eventContext); // そのステージだけの描画（花より下）
        events.renderBack(gc, eventContext); // イベントの奥の描画（火山の山など。花より下）
        flower.render(gc); // 花
        if (!clearing)
            target.render(gc, flower.x, flower.y); // ターゲットリング（クリア演出中は消す）
        feedingEffect.renderBehind(gc); // 花からくちばしへ飛んでいる蜜の粒（プレイヤーより奥）
        player.render(gc); // プレイヤー
        feedingEffect.renderFront(gc); // くちばしの上を流れる蜜の粒ときらめき（プレイヤーより手前）
        events.render(gc, eventContext); // イベント（霧など。UI より下）
        script.renderFront(gc, eventContext); // そのステージだけの描画（UI より下）
        gc.restore();

        // クリア演出中は UI を出さず、カットの効果を出して、最後に白くする
        if (clearing) {
            renderCutEffect(gc);
            renderClearFade(gc);
            return;
        }

        renderTimeAndName(gc);
        renderFullness(gc);

        hud.render(gc, state); // UI（いちばん上）
        debug.render(gc, state); // F3 で表示/非表示

        // ★ 開始演出の描画（黒からのフェードインと Ready/Go の文字）
        if (startElapsed < READY_SECONDS + GO_SECONDS) {
            renderStartSequence(gc);
        }

        pauseMenu.render(gc);
    }

    /** 開始前のフェードインと文字を描画する。 */
    private void renderStartSequence(GraphicsContext gc) {
        // 黒から徐々に透明になるフェードイン
        double fade = Math.max(0, 1.0 - (startElapsed / FADE_IN_SECONDS));
        if (fade > 0) {
            gc.setFill(Color.color(0, 0, 0, fade));
            gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        }

        // 文字の画像（images/Tex_Game_Ready・Tex_Game_Go）。画像が無いときは文字で描く
        boolean ready = startElapsed < READY_SECONDS;
        Image image = Assets.image(ready ? "Tex_Game_Ready" : "Tex_Game_Go");
        if (image != null) {
            // 2枚は同じ大きさの画像の中で文字の位置をそろえてあるので、画像ごと画面の真ん中に置く
            double w = image.getWidth() * START_TEXT_SCALE;
            double h = image.getHeight() * START_TEXT_SCALE;
            gc.drawImage(image, (Game.WIDTH - w) / 2, (Game.HEIGHT - h) / 2, w, h);
            return;
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Assets.font("main", 120));
        gc.setFill(ready ? Color.WHITE : Color.YELLOW);
        gc.fillText(ready ? "Ready..." : "Go!", Game.WIDTH / 2.0, Game.HEIGHT / 2.0 + 40);
        gc.setTextAlign(TextAlignment.LEFT); // 他の描画に影響が出ないよう左揃えに戻す
    }

    // 右上のタイムとステージ名（images/Tex_TimeText*.png・Tex_StageText{N}.png）。
    // 画像は 561x561 の真ん中に文字だけがあるので、透明でない部分を切り出して描く
    private static final double HUD_RIGHT = Game.WIDTH - 35;
    /** 「タイム：」の画像の上端の位置と、描く倍率 */
    private static final double TIME_TOP = 30, TIME_SCALE = 1.0;
    /** 秒数の数字の倍率。TIME_SCALE とは別に、数字だけ大きさを変えられる（文字の大きさ・白い縁・太らせる量が一緒に変わる） */
    private static final double TIME_DIGIT_SCALE = 1.6;
    private static final double NAME_TOP = 130, NAME_SCALE = 1.0;
    // 下の秒数の値は倍率 1 のときの値。数字の大きさ・縁・太さには TIME_DIGIT_SCALE、位置とすき間には TIME_SCALE がかかる
    private static final double TIME_FONT_SIZE = 61;
    /** 秒数の文字の下端（ベースライン）が「タイム：」の画像の上端から何 px 下か。小さくすると秒数が上へ。画像の大きさに合わせて動く */
    private static final double TIME_BASELINE = 56;
    /** 「タイム：」の画像と秒数のすき間（px） */
    private static final double TIME_GAP = 7;
    /** 秒数の数字の数の最大（整数 4 けた＋小数 2 けた = 9999.99） */
    private static final int TIME_MAX_DIGITS = 6;
    /** 秒数の白い縁取りの太さ（px）。線の半分は文字の内側に隠れるので、見える縁はこの半分 */
    private static final double TIME_OUTLINE = 18;
    /** 秒数の数字を太らせる量（px）。文字と同じ色の線でなぞる太さで、0 なら太字のフォントのまま。大きくすると太くなる */
    private static final double TIME_BOLDNESS = 3;
    /** 秒数の色（「タイム：」の画像の文字の色）。ステージ1・2 は共通の画像なので同じ色 */
    private static final Color[] TIME_COLORS = {
            Color.web("#ff3c00"), Color.web("#ff3c00"), Color.web("#ff8a0b"), Color.web("#0098ff"),
            Color.web("#ff6700") };
    private static final Map<String, Rectangle2D> TRIMS = new HashMap<>();

    /** 右上に「タイム：」の画像と秒数、その下にステージ名の画像を、右端をそろえて描く。画像が無ければ文字で描く。 */
    private void renderTimeAndName(GraphicsContext gc) {
        String timeKey = stageNumber <= 2 ? "Tex_TimeText" : "Tex_TimeText" + stageNumber;
        Color timeColor = TIME_COLORS[Math.clamp(stageNumber - 1, 0, TIME_COLORS.length - 1)];
        double fontSize = TIME_FONT_SIZE * TIME_DIGIT_SCALE;
        Font font = Font.font(Assets.font("main", fontSize).getFamily(), FontWeight.BOLD, fontSize); // 画像の文字に合わせて太字

        // 秒数（右端そろえ。画像と同じく白い縁取り）。
        // 数字ごとに幅が違うと数字が変わるたびに左右に揺れるので、1文字ずつ同じ幅のマスの真ん中に描く
        Image timeImage = Assets.image(timeKey);
        gc.save(); // 線の太さなどをほかの描画に残さない
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(font);
        double baseline = TIME_TOP + TIME_BASELINE * TIME_SCALE;
        double digitW = textWidth("0", font), dotW = textWidth(".", font);
        String text = timer.text();
        double[] centers = new double[text.length()];
        double x = HUD_RIGHT;
        for (int i = text.length() - 1; i >= 0; i--) {
            double w = text.charAt(i) == '.' ? dotW : digitW;
            x -= w;
            centers[i] = x + w / 2;
        }
        if (timeImage != null) {
            // 白い縁 → 文字と同じ色の線（太らせる分）→ 塗り の順に、全部の文字で重ねる
            // （1文字ずつ描くと、となりの文字の白い縁が前の文字に重なるため）
            double bold = TIME_BOLDNESS * TIME_DIGIT_SCALE;
            gc.setLineJoin(StrokeLineJoin.ROUND); // 太くしても角がとがらないように
            gc.setLineWidth(TIME_OUTLINE * TIME_DIGIT_SCALE + bold); // 太らせた分だけ白い縁も外へ広げる
            gc.setStroke(Color.WHITE);
            for (int i = 0; i < text.length(); i++)
                gc.strokeText(String.valueOf(text.charAt(i)), centers[i], baseline);
            if (bold > 0) {
                gc.setLineWidth(bold);
                gc.setStroke(timeColor);
                for (int i = 0; i < text.length(); i++)
                    gc.strokeText(String.valueOf(text.charAt(i)), centers[i], baseline);
            }
        }
        gc.setFill(timeImage != null ? timeColor : Color.WHITE);
        for (int i = 0; i < text.length(); i++)
            gc.fillText(String.valueOf(text.charAt(i)), centers[i], baseline);
        gc.setTextAlign(TextAlignment.LEFT);

        // 「タイム：」は秒数の左。最大の「9999.99」（GameTimer の上限）の幅で場所を取り、秒数が増えても動かないようにする
        if (timeImage != null) {
            double digitsWidth = digitW * TIME_MAX_DIGITS + dotW;
            drawTrimmed(gc, timeImage, timeKey, HUD_RIGHT - digitsWidth - TIME_GAP * TIME_SCALE, TIME_TOP, TIME_SCALE);
        }

        String nameKey = "Tex_StageText" + stageNumber;
        Image nameImage = Assets.image(nameKey);
        if (nameImage != null) {
            drawTrimmed(gc, nameImage, nameKey, HUD_RIGHT, NAME_TOP, NAME_SCALE);
        } else {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(Color.WHITE);
            gc.setFont(Assets.font("main", 39));
            gc.fillText(stage.name, HUD_RIGHT, 130);
        }
        gc.restore();
    }

    /** 文字列をそのフォントで描いたときの幅。 */
    private static double textWidth(String s, Font font) {
        Text t = new Text(s);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    /** 画像の透明でない部分だけを、右端が right・上端が top になるように scale 倍で描く。 */
    private static void drawTrimmed(GraphicsContext gc, Image image, String key, double right, double top,
            double scale) {
        Rectangle2D r = TRIMS.computeIfAbsent(key, k -> opaqueBounds(image));
        double w = r.getWidth() * scale, h = r.getHeight() * scale;
        gc.drawImage(image, r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight(), right - w, top, w, h);
    }

    /** 画像の中で透明でない部分の範囲。全部透明（または読めない）なら画像全体。 */
    private static Rectangle2D opaqueBounds(Image image) {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelReader pr = image.getPixelReader();
        if (pr == null)
            return new Rectangle2D(0, 0, w, h);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((pr.getArgb(x, y) >>> 24) > 20) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0)
            return new Rectangle2D(0, 0, w, h);
        return new Rectangle2D(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // まんぷくゲージ（images/Tex_GameScene_Gauge.png）。画像の左上の位置と、画面に描く倍率
    private static final String GAUGE_TEXTURE = "Tex_GameScene_Gauge";
    private static final double GAUGE_X = 20, GAUGE_Y = 15, GAUGE_SCALE = 1.0;
    /** 画像の大きさと、その中の灰色の部分（中身が伸びる所。両端が丸い）の位置と大きさ（画像の px） */
    private static final double GAUGE_IMAGE_W = 820, GAUGE_IMAGE_H = 175;
    private static final double GAUGE_INNER_X = 17, GAUGE_INNER_Y = 73, GAUGE_INNER_W = 776, GAUGE_INNER_H = 76;
    /** 中身の色（吸っている間は少し明るくする） */
    private static final Color GAUGE_FILL = Color.web("#36c23a");
    private static final Color GAUGE_FILL_FEEDING = Color.web("#6ee86a");

    /** 満腹度を左上にゲージで表示する。画像の灰色の部分を、満腹度の割合だけ緑で塗る。 */
    private void renderFullness(GraphicsContext gc) {
        double ratio = Math.clamp(state.nectar / state.maxNectar, 0.0, 1.0);
        boolean feeding = state.status == GameState.Status.FEEDING;

        gc.save();
        gc.translate(GAUGE_X, GAUGE_Y);
        gc.scale(GAUGE_SCALE, GAUGE_SCALE);
        // 画像の余白を除き、まんぷくゲージの部分だけを描く
        var gaugeImage = Assets.image(GAUGE_TEXTURE);
        if (gaugeImage != null) {
            gc.drawImage(
                    gaugeImage,
                    // 元画像から切り出す位置と大きさ
                    135, 459, GAUGE_IMAGE_W, GAUGE_IMAGE_H,
                    // 描画先の位置と大きさ（縦横比を変えずに描く）
                    0, 0, GAUGE_IMAGE_W, GAUGE_IMAGE_H);
        }
        // 中身：灰色の部分の形（両端が丸い）で切り抜いてから、左から割合の分だけ塗る
        if (ratio > 0) {
            double r = GAUGE_INNER_H / 2;
            double x = GAUGE_INNER_X, y = GAUGE_INNER_Y, w = GAUGE_INNER_W, h = GAUGE_INNER_H;
            gc.beginPath();
            gc.moveTo(x + r, y);
            gc.lineTo(x + w - r, y);
            gc.arcTo(x + w, y, x + w, y + r, r);
            gc.arcTo(x + w, y + h, x + w - r, y + h, r);
            gc.lineTo(x + r, y + h);
            gc.arcTo(x, y + h, x, y + r, r);
            gc.arcTo(x, y, x + r, y, r);
            gc.closePath();
            gc.clip();
            gc.setFill(feeding ? GAUGE_FILL_FEEDING : GAUGE_FILL);
            gc.fillRect(x, y, w * ratio, h);
        }
        gc.restore();
    }

    @Override
    public BaseScene next() {
        return new ResultScene(state);
    }
}