package org.example.ui.stageselect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.example.core.BaseScene;
import org.example.core.Game;
import org.example.core.DevMode;
import org.example.core.FadeInScene;
import org.example.core.Input;
import org.example.game.GameScene;
import org.example.media.Sound;
import org.example.ui.stageselect.detail.StageDetailPanel;
import org.example.ui.title.TitleScene;

/** 選択画面のキー操作、詳細の開閉、画面切り替えを担当する。 */
public class StageSelectScene implements BaseScene {

    private final StageSelectView view = new StageSelectView();
    private final StageSelectEntrance entrance = new StageSelectEntrance();
    private final boolean fadeInOnEnter;
    private boolean debugNextBeforeScene;

    // フェードアウト用の変数（ゲーム画面・タイトルへ遷移時）
    private boolean exiting = false;
    private double exitTime = 0.0;
    /** 暗転し終わったら移る画面 */
    private BaseScene exitTo;
    private static final double EXIT_SECONDS = 0.4; // フェードにかかる秒数

    // BGMフェードイン用の変数
    private boolean fadingInBgm = false;
    private double bgmFadeInTime = 0.0;
    private static final double BGM_FADE_IN_SECONDS = 0.6;
    private double targetBgmVolume;

    public StageSelectScene() {
        this(false);
    }

    public StageSelectScene(boolean fadeInOnEnter) {
        this.fadeInOnEnter = fadeInOnEnter;
    }

    @Override
    public void onEnter() {
        debugNextBeforeScene = Game.enterToNext;
        Game.enterToNext = false;
        exiting = false;
        exitTime = 0.0;
        startEntrance(fadeInOnEnter);
        Game.showUi(view);
        view.requestFocus();

        // ステージセレクト専用BGMのフェードイン準備
        targetBgmVolume = Sound.getBgmVolume(); // 本来の音量設定値を記憶
        Sound.setBgmVolume(0.0);                 // 音量を一時的に0にセット
        Sound.bgm("BGM_StageSelect");          // ステージセレクト専用BGMの再生開始
        fadingInBgm = true;
        bgmFadeInTime = 0.0;
    }

    /** 画面を暗くし始める。暗くなりきったら target へ移る。 */
    private void startExit(BaseScene target) {
        exitTo = target;
        exiting = true;
        exitTime = 0.0;
    }

    @Override
    public void onExit() {
        // BGM がだんだん大きくなる途中で抜けたときも、BGM の音量の設定を元に戻す
        if (fadingInBgm) {
            Sound.setBgmVolume(targetBgmVolume);
            fadingInBgm = false;
        }
        view.detail().close();
        Game.enterToNext = debugNextBeforeScene;
        Game.showUi(null);
    }

    @Override
    public void update() {
        view.updatePulse(); // 選んでいる花を脈打たせる

        // BGMのフェードイン処理
        if (fadingInBgm) {
            bgmFadeInTime += Game.STEP;
            double progress = Math.clamp(bgmFadeInTime / BGM_FADE_IN_SECONDS, 0.0, 1.0);
            Sound.setBgmVolume(targetBgmVolume * progress);
            if (progress >= 1.0) {
                fadingInBgm = false;
            }
        }

        // 1. フェードアウト中なら、画面を暗くする処理だけを行う
        if (exiting) {
            exitTime += Game.STEP;
            double progress = Math.min(exitTime / EXIT_SECONDS, 1.0);
            
            // UI全体の透明度を下げて暗転させる
            view.setOpacity(1.0 - progress);

            // 完全に暗転したら、行き先（ゲーム画面かタイトル）へ切り替える
            if (progress >= 1.0) {
                Game.change(exitTo);
            }
            return;
        }

        StageDetailPanel detail = view.detail();

        if (!detail.isVisible() && Input.isPressed(KeyCode.ESCAPE)) {
            Sound.se("SE_Cancel");
            // 暗くしてからタイトルへ。タイトルは暗い所からだんだん見えてくる
            startExit(new FadeInScene(new TitleScene(), Color.BLACK, EXIT_SECONDS));
            return;
        }
        
        if (!entrance.isFinished()) {
            entrance.update();
            view.applyEntrance(entrance);
            return;
        }

        if (detail.isVisible()) {
            if (Input.isPressed(KeyCode.ESCAPE)) {
                Sound.se("SE_Cancel");
                detail.close();
                startEntrance(false);
                view.requestFocus();
                return;
            }
            if (DevMode.ENABLED && Input.isPressed(KeyCode.F2)) { // 開発用
                detail.toggleLayoutEditing();
                return;
            }
            if (detail.isLayoutEditing()) {
                detail.updateEditor();
                return;
            }

            // A/D（←/→）で、詳細を開いたまま一個前・次のステージに切り替える（端からは反対の端へ）
            if (Input.isPressed(KeyCode.A) || Input.isPressed(KeyCode.LEFT)) {
                view.selectRelative(-1);
                detail.open(view.selectedStage());
                return;
            }
            if (Input.isPressed(KeyCode.D) || Input.isPressed(KeyCode.RIGHT)) {
                view.selectRelative(1);
                detail.open(view.selectedStage());
                return;
            }

            // 2. 詳細画面で ENTER キーが押されたら決定音を鳴らしてゲーム画面へフェードアウト
            if (Input.isPressed(KeyCode.ENTER)) {
                Sound.se("SE_Decide"); // ★ 決定音
                startExit(next());
            }
            return;
        }

        // ステージ一覧画面での決定（詳細パネルを開く）および移動
        if (Input.isPressed(KeyCode.ENTER)) {
            Sound.se("SE_Decide"); // ★ 決定音
            detail.open(view.selectedStage());
        } else if (Input.isPressed(KeyCode.A) || Input.isPressed(KeyCode.LEFT)) {
            view.selectRelative(-1);
        } else if (Input.isPressed(KeyCode.D) || Input.isPressed(KeyCode.RIGHT)) {
            view.selectRelative(1);
        }
    }

    private void startEntrance(boolean fadeIn) {
        entrance.start(fadeIn);
        view.applyEntrance(entrance);
    }

    @Override
    public void render(GraphicsContext gc) {
        if (exiting) {
            double progress = Math.min(exitTime / EXIT_SECONDS, 1.0);
            gc.setFill(Color.rgb(0, 0, 0, progress));
            gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        }
    }

    @Override
    public BaseScene next() {
        return new GameScene(view.selectedStage());
    }
}