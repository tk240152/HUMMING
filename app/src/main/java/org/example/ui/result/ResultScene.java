package org.example.ui.result;

import org.example.ui.result.ResultSkin.Slot;
import org.example.ui.stageselect.StageSelectScene;

import static org.example.ui.result.ResultSkin.*;

import java.util.Random;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.TextAlignment;

import org.example.media.Assets;
import org.example.media.Sound;
import org.example.core.BaseScene;
import org.example.core.Game;
import org.example.state.GameState;
import org.example.core.DevMode;
import org.example.core.Input;
import org.example.game.GameScene;

/**
 * リザルト画面。
 */
public class ResultScene implements BaseScene {

    // ===== 演出タイムライン（秒） =====
    private static final double JINGLE_DURATION = 3.0; // ジングル再生時間（3秒）

    private static final double TIME_START  = 0.5;
    private static final double TIME_DUR    = 2.7;
    private static final double RANK_START  = TIME_START + TIME_DUR + 0.4;
    private static final double RANK_DROP   = 0.35;
    private static final double BIRD_START  = RANK_START + RANK_DROP + 0.2;
    private static final double BEST_START  = RANK_START + RANK_DROP + 0.6;
    private static final double BEST_DUR    = 2.7;
    private static final double BADGE_START = BEST_START + BEST_DUR + 0.3;
    private static final double BADGE_DUR   = 0.35;
    private static final double BUTTON_FADE = 0.3;

    private static final LinearGradient SKY = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, SKY_TOP), new Stop(1, SKY_BOTTOM));

    private static final int CONFETTI = 30;

    private final GameState state;
    private final double buttonStart;
    private final double endTime;

    // 紙吹雪
    private final double[] cX = new double[CONFETTI];
    private final double[] cSpeed = new double[CONFETTI];
    private final double[] cDelay = new double[CONFETTI];
    private final double[] cSize = new double[CONFETTI];
    private final double[] cPhase = new double[CONFETTI];
    private final Color[] cColor = new Color[CONFETTI];

    private double t = 0;
    private double jingleTimer = 0;
    private boolean bgmStarted = false;
    private int selectedIndex = 0; // 0: リプレイ, 1: ステージセレクト

    // SE重複再生防止フラグ
    private boolean playedClearTimeSe = false;
    private boolean playedRankSe = false;
    private boolean playedBestTimeSe = false;
    private boolean playedNewBestSe = false;
    private boolean playedConfettiSe = false;
    private boolean playedConfettiSe2 = false;

    public ResultScene(GameState state) {
        this.state = state;
        this.buttonStart = state.newBest
                ? BADGE_START + BADGE_DUR + 0.3
                : BEST_START + BEST_DUR + 0.3;
        this.endTime = buttonStart + BUTTON_FADE;

        Random rnd = new Random(7);
        for (int i = 0; i < CONFETTI; i++) {
            cX[i] = 80 + rnd.nextDouble() * 1760;
            cSpeed[i] = 160 + rnd.nextDouble() * 140;
            cDelay[i] = rnd.nextDouble() * 2.5;
            cSize[i] = 22 + rnd.nextDouble() * 18;
            cPhase[i] = rnd.nextDouble() * Math.PI * 2;
            cColor[i] = PETAL_COLORS[rnd.nextInt(PETAL_COLORS.length)];
        }
    }

    @Override
    public String bgm() {
        return "BGM_Result";
    }

    @Override
    public void onEnter() {
        Sound.se("SE_Result_Jingle");
    }

    // ============================================================
    // 更新
    // ============================================================
    @Override
    public void update() {
        // デバッグ機能: Rキー押下でベストスコアをリセット
        if (DevMode.ENABLED && Input.isPressed(KeyCode.R)) { // 開発用
            state.resetBestTime(); 
            System.out.println("[Debug] ベストタイムをリセットしました！");
            Game.change(new ResultScene(state));
            return;
        }

        // 1. ジングル再生フェーズ（3秒間）
        if (jingleTimer < JINGLE_DURATION) {
            jingleTimer += Game.STEP;

            if (Input.isPressed(KeyCode.ENTER)) {
                jingleTimer = JINGLE_DURATION;
            }
            return;
        }

        // 2. ジングル終了後にBGM開始
        if (!bgmStarted) {
            bgmStarted = true;
            Sound.bgm(bgm());
        }

        // 3. タイムライン進行
        t += Game.STEP;

        // SE再生制御
        updateSoundEffects();

        // 演出中: Enter で最後までスキップ
        if (t < endTime) {
            if (Input.isPressed(KeyCode.ENTER)) {
                t = endTime;
                 
                Sound.stopSe(); 
                
                playedClearTimeSe = true;
                playedRankSe = true;
                playedBestTimeSe = true;
                playedNewBestSe = true;
                playedConfettiSe = true;
            }
            return;
        }

        // A/D カーソル移動
        if ((Input.isPressed(KeyCode.A) || Input.isPressed(KeyCode.LEFT)) && selectedIndex != 0) {
            selectedIndex = 0;
            Sound.se("SE_Cursor");
        }
        if ((Input.isPressed(KeyCode.D) || Input.isPressed(KeyCode.RIGHT)) && selectedIndex != 1) {
            selectedIndex = 1;
            Sound.se("SE_Cursor");
        }

        // ENTER 決定
        if (Input.isPressed(KeyCode.ENTER)) {
            Sound.se("SE_Decide");
            if (selectedIndex == 0) {
                Game.change(new GameScene(state.stage));
            } else {
                Game.change(new StageSelectScene());
            }
        }
    }

    private void updateSoundEffects() {
        if (!playedClearTimeSe && t >= TIME_START) {
            playedClearTimeSe = true;
            Sound.se("SE_Score");
        }

        if (!playedRankSe && t >= RANK_START + RANK_DROP) {
            playedRankSe = true;
            Sound.se("SE_Stamp");
        }

        if (!playedBestTimeSe && t >= BEST_START) {
            playedBestTimeSe = true;
            Sound.se("SE_Score");
        }

        if (!playedNewBestSe && state.newBest && t >= BADGE_START) {
            playedNewBestSe = true;
            Sound.se("SE_UpdateScore");
        }

        if (!playedConfettiSe && state.newBest && t >= BADGE_START) {
            playedConfettiSe = true;
            System.out.println("[Debug] SE_Iyahho 再生試行");
            Sound.se("SE_Iyahho");
            
        }

        if (!playedConfettiSe2 && state.newBest && t >= BADGE_START + 0.2) {
            playedConfettiSe2 = true;
            Sound.se("SE_Donpahu"); 
        }
    }

    // ============================================================
    // 描画（動きの流れ）
    // ============================================================
    @Override
    public void render(GraphicsContext gc) {
        // 背景の描画
        drawBackground(gc);

        gc.save();
        applyShake(gc);

        // ★ 1. 「ステージクリア」タイトルの描画（ジングル中からアニメーション付きで登場）
        drawAnimatedTitle(gc);

        // ★ 2. スコア表示用パネルボードの描画（ジングル終了後にスライド＆フェードイン登場）
        if (jingleTimer >= JINGLE_DURATION) {
            drawAnimatedPanel(gc);
        }

        // ★ 3. 各種スコアパーツの演出（パネル登場後に順次アニメーション表示）
        if (jingleTimer >= JINGLE_DURATION) {
            
            // --- クリアタイム ---
            if (t >= TIME_START) {
                double popProg = clamp01((t - TIME_START) / 0.25);
                double scale = 0.8 + 0.2 * easeOutBack(popProg);
                
                gc.save();
                gc.translate(px(PANEL.cx()), py(380));
                gc.scale(scale, scale);
                gc.translate(-px(PANEL.cx()), -py(380));
                gc.setGlobalAlpha(popProg);

                double p = clamp01((t - TIME_START) / TIME_DUR);
                drawLabel(gc, LABEL_CLEAR, "クリアタイム", TEAL);
                text(gc, NUMBER_FONT, formatTime(state.clearTime * easeOutCubic(p)),
                        TIME_NUM_X, TIME_NUM_Y, TIME_NUM_SIZE, TextAlignment.RIGHT, INK);
                drawBar(gc, BAR_CLEAR, TEAL);

                gc.restore();
            }

            // --- ベストタイム ---
            if (t >= BEST_START) {
                double popProg = clamp01((t - BEST_START) / 0.25);
                double scale = 0.8 + 0.2 * easeOutBack(popProg);

                gc.save();
                gc.translate(px(PANEL.cx()), py(580));
                gc.scale(scale, scale);
                gc.translate(-px(PANEL.cx()), -py(580));
                gc.setGlobalAlpha(popProg);

                double p = clamp01((t - BEST_START) / BEST_DUR);
                drawLabel(gc, LABEL_BEST, "ベストタイム", GREEN);
                text(gc, NUMBER_FONT, formatTime(state.bestTime * easeOutCubic(p)),
                        BEST_NUM_X, BEST_NUM_Y, BEST_NUM_SIZE, TextAlignment.RIGHT, INK);
                drawBar(gc, BAR_BEST, GREEN);

                gc.restore();
            }

            // --- ランクスタンプ & ハチドリ & ベスト更新バッジ ---
            drawRankStamp(gc);
            drawBird(gc);
            drawNewBestBadge(gc);
        }

        gc.restore();

        // 花びら演出
        if (jingleTimer >= JINGLE_DURATION) {
            drawConfetti(gc);
        }

        // ボタン表示（フェードイン）
        if (jingleTimer >= JINGLE_DURATION) {
            double alpha = clamp01((t - buttonStart) / BUTTON_FADE);
            if (alpha > 0) {
                gc.setGlobalAlpha(alpha);
                drawButton(gc, BTN_REPLAY, "リプレイ", selectedIndex == 0);
                drawButton(gc, BTN_STAGE, "ステージセレクト", selectedIndex == 1);
                gc.setGlobalAlpha(1.0);
            }
        }

        // 描画状態のリセット
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    // ============================================================
    // アニメーション付き個別描画処理
    // ============================================================

    /** ジングル中からポップに登場する「ステージクリア」タイトル */
    private void drawAnimatedTitle(GraphicsContext gc) {
        double titleProg = clamp01(jingleTimer / 0.4); // 0.4秒間でポップアップ
        double scale = 0.5 + 0.5 * easeOutBack(titleProg);
        double alpha = titleProg;

        gc.save();
        gc.translate(px(TITLE.cx()), py(TITLE.cy()));
        gc.scale(scale, scale);
        gc.translate(-px(TITLE.cx()), -py(TITLE.cy()));
        gc.setGlobalAlpha(alpha);

        drawTitle(gc);

        gc.restore();
    }

    /** ジングル終了後に下からスライド＆フェードインで表示されるスコアパネルボード */
    private void drawAnimatedPanel(GraphicsContext gc) {
        double panelProg = clamp01(t / 0.35); // 0.35秒かけて出現
        double slideY = py(40) * (1.0 - easeOutCubic(panelProg)); // 下から上へ移動
        double scale = 0.9 + 0.1 * easeOutCubic(panelProg);
        double alpha = panelProg;

        gc.save();
        gc.translate(px(PANEL.cx()), py(PANEL.cy()) + slideY);
        gc.scale(scale, scale);
        gc.translate(-px(PANEL.cx()), -py(PANEL.cy()));
        gc.setGlobalAlpha(alpha);

        drawPanel(gc);

        gc.restore();
    }

    // ============================================================
    // 各パーツ基本描画処理
    // ============================================================
    private void drawBackground(GraphicsContext gc) {
        // クリアしたステージの背景（ゲーム画面と同じく、画像が無ければ水色）。
        // 晴れの絵があるステージは、ステージの背景（嵐など）は描かず、水色の空の上に晴れの絵を描く
        if (state.backgroundKey != null) {
            String[] layers = stageBackgroundLayers(state.stage);
            if (layers.length > 0
                    || !texture(gc, state.backgroundKey, px(960), py(540), Game.WIDTH, Game.HEIGHT)) {
                gc.setFill(Color.SKYBLUE);
                gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
            }
            for (String layer : layers) {
                texture(gc, layer, px(960), py(540), Game.WIDTH, Game.HEIGHT);
            }
            return;
        }
        if (!slot(gc, BACKGROUND)) {
            gc.setFill(SKY);
            gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        }
    }

    private void drawTitle(GraphicsContext gc) {
        if (slot(gc, TITLE)) {
            return;
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Assets.font(LABEL_FONT, fontSize(110)));

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(fs(16));
        gc.strokeText("ステージクリア", px(TITLE.cx()), py(TITLE.cy()));
        gc.setFill(INK);
        gc.fillText("ステージクリア", px(TITLE.cx()), py(TITLE.cy()));

        drawFlower(gc, px(500), py(TITLE.cy()), fs(40), Color.ORCHID, CENTER, (jingleTimer + t) * 40);
        drawFlower(gc, px(1420), py(TITLE.cy()), fs(40), Color.SKYBLUE, CENTER, -(jingleTimer + t) * 40);
    }

    private void drawPanel(GraphicsContext gc) {
        if (slot(gc, PANEL)) {
            return;
        }
        double x = px(PANEL.cx() - PANEL.w() / 2);
        double y = py(PANEL.cy() - PANEL.h() / 2);
        gc.setFill(Color.web("#ffffff", 0.82));
        gc.fillRoundRect(x, y, px(PANEL.w()), py(PANEL.h()), fs(60), fs(60));
        gc.setStroke(TEAL);
        gc.setLineWidth(fs(8));
        gc.strokeRoundRect(x, y, px(PANEL.w()), py(PANEL.h()), fs(60), fs(60));
    }

    private void drawLabel(GraphicsContext gc, Slot s, String label, Color color) {
        if (slot(gc, s)) {
            return;
        }
        double w = px(s.w());
        double h = py(s.h());
        gc.setFill(color);
        gc.fillRoundRect(px(s.cx()) - w / 2, py(s.cy()) - h / 2, w, h, h, h);
        text(gc, LABEL_FONT, label, s.cx(), s.cy(), 54, TextAlignment.CENTER, Color.WHITE);
    }

    private void drawBar(GraphicsContext gc, Slot s, Color color) {
        if (slot(gc, s)) {
            return;
        }
        gc.save();
        gc.setStroke(color);
        gc.setLineWidth(fs(10));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(px(s.cx() - s.w() / 2), py(s.cy()),
                px(s.cx() + s.w() / 2), py(s.cy()));
        gc.restore();
    }

    private void drawRankStamp(GraphicsContext gc) {
        double e = t - RANK_START;
        if (e < 0) {
            return;
        }

        double scale;
        double alpha;
        double rot;
        if (e < RANK_DROP) {
            double p = e / RANK_DROP;
            scale = 1.0 + 2.0 * (1.0 - p * p);
            alpha = Math.min(1.0, p * 3.0);
            rot = -50.0 * (1.0 - p * p);
        } else {
            double q = clamp01((e - RANK_DROP) / 0.18);
            scale = 1.0 + 0.12 * Math.sin(Math.PI * q);
            alpha = 1.0;
            rot = 0;
        }

        String rank = state.getRank();

        gc.save();
        gc.translate(px(STAMP.cx()), py(STAMP.cy()));
        gc.scale(scale, scale);
        gc.setGlobalAlpha(alpha);
        gc.rotate(rot);

        if (!texture(gc, rankKey(rank), 0, 0, px(STAMP.w()), py(STAMP.h()))) {
            drawFlower(gc, 0, 0, fs(STAMP.w() / 2), rankColor(rank), CENTER, 0);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(Assets.font(NUMBER_FONT, fontSize(140)));
            gc.setFill(INK);
            gc.fillText(rank, 0, 0);
        }

        gc.restore();
    }

    private void drawBird(GraphicsContext gc) {
        double e = t - BIRD_START;
        if (e < 0) {
            return;
        }

        double in = easeOutCubic(clamp01(e / 0.6));
        
        double idleSpeed = 4.0;     
        double idleRadiusX = fs(4); 
        double idleRadiusY = fs(8); 
        double circleOffsetX = Math.cos(t * idleSpeed) * idleRadiusX;
        double circleOffsetY = Math.sin(t * idleSpeed) * idleRadiusY;

        double x = px(BIRD.cx()) - fs(300) * (1.0 - in) + circleOffsetX;
        double y = py(BIRD.cy()) + circleOffsetY;

        gc.save();
        gc.translate(x, y);
        gc.setGlobalAlpha(clamp01(e / 0.3));

        double w = px(300); 
        double h = py(250);

        if (hasTexture("hummingbird_body")) {
            texture(gc, "hummingbird_body", 0, 0, w, h);

            gc.save();
            double rootX = px(-20);
            double rootY = py(-1.0);
            
            gc.translate(rootX, rootY);

            double wingFlapsPerSecond = 12.0; 
            double wingFlapDegrees = 90.0;    
            double wingBaseAngle = -80.0; 
            double wingSwing = Math.sin(t * 2 * Math.PI * wingFlapsPerSecond);
            
            gc.rotate(wingBaseAngle + (wingSwing * wingFlapDegrees));

            if (hasTexture("hummingbird_wing")) {
                texture(gc, "hummingbird_wing", -rootX, -rootY, w, h);
            }
            
            gc.restore();

        } else if (!texture(gc, BIRD.key(), 0, 0, w, h)) {
            gc.scale(fs(3.2), fs(3.2));
            drawBirdFallback(gc);
        }

        gc.restore();
    }

    private void drawNewBestBadge(GraphicsContext gc) {
        if (!state.newBest || t < BADGE_START) {
            return;
        }

        double s = easeOutBack(clamp01((t - BADGE_START) / BADGE_DUR));
        if (s < 0.01) {
            return;
        }

        double w = px(BADGE.w());
        double h = py(BADGE.h());

        gc.save();
        gc.translate(px(BADGE.cx()), py(BADGE.cy()));
        gc.scale(s, s);
        gc.rotate(-4);

        if (!texture(gc, BADGE.key(), 0, 0, w, h)) {
            gc.setFill(PINK);
            gc.fillRoundRect(-w / 2, -h / 2, w, h, h, h);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(fs(7));
            gc.strokeRoundRect(-w / 2, -h / 2, w, h, h, h);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(Assets.font(LABEL_FONT, fontSize(42)));
            gc.setFill(Color.WHITE);
            gc.fillText("ベストタイム更新!!", 0, 0);
        }

        gc.restore();
    }

    private void drawConfetti(GraphicsContext gc) {
        if (!state.newBest || t < BADGE_START) {
            return;
        }

        double e = t - BADGE_START;
        for (int i = 0; i < CONFETTI; i++) {
            double tt = e - cDelay[i];
            if (tt < 0) {
                continue;
            }
            double y = -40 + tt * cSpeed[i];
            if (y > 1120) {
                continue;
            }
            double x = cX[i] + Math.sin(cPhase[i] + tt * 2.0) * 60;
            double s = fs(cSize[i]);

            gc.save();
            gc.translate(px(x), py(y));
            gc.rotate(Math.toDegrees(cPhase[i] + tt * 3.0));
            gc.setGlobalAlpha(0.9);
            if (!texture(gc, PETAL_KEY, 0, 0, s, s)) {
                gc.setFill(cColor[i]);
                gc.fillOval(-s / 2, -s * 0.3, s, s * 0.6);
            }
            gc.restore();
        }
    }

    private void drawButton(GraphicsContext gc, Slot s, String label, boolean selected) {
        double w = px(s.w());
        double h = py(s.h());
        double pulse = selected ? 1.04 + 0.02 * Math.sin(t * 6.0) : 1.0;

        gc.save();
        gc.translate(px(s.cx()), py(s.cy()));
        gc.scale(pulse, pulse);

        boolean textured = (selected && texture(gc, s.key() + "_sel", 0, 0, w, h))
                || texture(gc, s.key(), 0, 0, w, h);

        if (!textured) {
            gc.setFill(selected ? PINK : Color.web("#ffffff", 0.92));
            gc.fillRoundRect(-w / 2, -h / 2, w, h, h, h);
            gc.setStroke(selected ? PINK.darker() : TEAL);
            gc.setLineWidth(fs(6));
            gc.strokeRoundRect(-w / 2, -h / 2, w, h, h, h);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(Assets.font(LABEL_FONT, fontSize(50)));
            gc.setFill(selected ? Color.WHITE : INK);
            gc.fillText(label, 0, 0);
        }

        // 選択中は赤い枠で囲む（ボタンと一緒に脈打つ）
        if (selected) {
            double left = px(CURSOR_LEFT - CURSOR_GAP), top = py(CURSOR_TOP - CURSOR_GAP);
            double right = px(CURSOR_RIGHT + CURSOR_GAP), bottom = py(CURSOR_BOTTOM + CURSOR_GAP);
            gc.setStroke(CURSOR_COLOR);
            gc.setLineWidth(fs(CURSOR_LINE));
            gc.strokeRect(left, top, right - left, bottom - top);
        }

        gc.restore();
    }

    private void applyShake(GraphicsContext gc) {
        double e = t - RANK_START - RANK_DROP;
        if (e >= 0 && e < 0.2) {
            double amp = fs(10) * (1.0 - e / 0.2);
            gc.translate(Math.sin(e * 80) * amp, Math.cos(e * 95) * amp);
        }
    }

    private boolean slot(GraphicsContext gc, Slot s) {
        return texture(gc, s.key(), px(s.cx()), py(s.cy()), px(s.w()), py(s.h()));
    }

    private boolean texture(GraphicsContext gc, String key,
                            double cx, double cy, double w, double h) {
        if (!hasTexture(key)) {
            return false;
        }
        Assets.draw(gc, key, cx - w / 2, cy - h / 2, w, h, Color.TRANSPARENT);
        return true;
    }

    private void drawBirdFallback(GraphicsContext gc) {
        gc.setFill(Color.web("#0096c7"));
        gc.fillPolygon(new double[]{-18, -36, -32}, new double[]{4, -3, 14}, 3);

        gc.setStroke(Color.web("#4a4e69"));
        gc.setLineWidth(3.5);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(10, -1, 46, 0);

        gc.setFill(Color.web("#38b000"));
        gc.fillOval(-24, -7, 40, 24);
        gc.setFill(Color.web("#d8f3dc"));
        gc.fillOval(-17, 1, 26, 14);
        gc.setFill(Color.web("#00b4d8"));
        gc.fillOval(-1, -15, 24, 24);
        gc.setFill(Color.web("#ff758f"));
        gc.fillOval(11, -3, 8, 5);

        gc.setFill(Color.web("#2b2d42"));
        gc.fillOval(11, -9, 7, 7);
        gc.setFill(Color.WHITE);
        gc.fillOval(14, -8, 2.5, 2.5);

        gc.save();
        gc.translate(-6, -2);
        gc.rotate(Math.sin((jingleTimer + t) * 42.0) * 50.0);
        gc.setFill(Color.rgb(144, 224, 239, 0.85));
        gc.setStroke(Color.web("#0077b6"));
        gc.setLineWidth(1.5);
        gc.fillOval(-9, -40, 18, 40);
        gc.strokeOval(-9, -40, 18, 40);
        gc.restore();
    }

    private void drawFlower(GraphicsContext gc, double x, double y, double radius,
                            Color petal, Color center, double rotationDeg) {
        double len = radius * 0.78;
        double wid = radius * 0.55;
        double dist = radius - len / 2;

        gc.save();
        gc.translate(x, y);
        gc.rotate(rotationDeg);

        gc.setFill(petal);
        gc.setStroke(petal.darker());
        gc.setLineWidth(Math.max(1.5, radius * 0.03));
        for (int i = 0; i < 8; i++) {
            gc.save();
            gc.rotate(i * 45);
            gc.fillOval(-wid / 2, -dist - len / 2, wid, len);
            gc.strokeOval(-wid / 2, -dist - len / 2, wid, len);
            gc.restore();
        }

        gc.setFill(center);
        gc.fillOval(-radius * 0.55, -radius * 0.55, radius * 1.1, radius * 1.1);
        gc.setStroke(petal.darker());
        gc.strokeOval(-radius * 0.55, -radius * 0.55, radius * 1.1, radius * 1.1);

        gc.restore();
    }

    private void text(GraphicsContext gc, String fontKey, String s, double x, double y,
                      double size, TextAlignment align, Color color) {
        gc.setTextAlign(align);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Assets.font(fontKey, fontSize(size)));
        gc.setFill(color);
        gc.fillText(s, px(x), py(y));
    }

    private static double px(double v) {
        return v * Game.WIDTH / 1920.0;
    }

    private static double py(double v) {
        return v * Game.HEIGHT / 1080.0;
    }

    private static double fs(double v) {
        return v * Math.min(Game.WIDTH / 1920.0, Game.HEIGHT / 1080.0);
    }

    private static int fontSize(double v) {
        return (int) Math.max(1, Math.round(fs(v)));
    }

    private static String formatTime(double v) {
        return String.format("%.2f", v); // 頭に0はつけない（ゲーム中のタイムと同じ形）
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double easeOutCubic(double p) {
        return 1.0 - Math.pow(1.0 - p, 3);
    }

    private static double easeOutBack(double p) {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        return 1.0 + c3 * Math.pow(p - 1.0, 3) + c1 * Math.pow(p - 1.0, 2);
    }

    @Override
    public BaseScene next() {
        return new StageSelectScene();
    }
}