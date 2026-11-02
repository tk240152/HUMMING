package org.example.ui.title;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.example.core.BaseScene;
import org.example.core.Game;
import org.example.core.Input;
import org.example.media.Sound;
import org.example.state.Records;
import org.example.ui.KeyGuide;
import org.example.ui.options.OptionPanel;
import org.example.ui.stageselect.StageSelectScene;

/** タイトル画面の進行と、ほかの画面への切り替えを担当する。 */
public class TitleScene implements BaseScene {

    private final TitleMenu menu = new TitleMenu();
    private final TitleView view = new TitleView();
    private final TitleEntrance entrance = new TitleEntrance();
    private final TitleBird bird = new TitleBird();
    private final KeyGuide keyGuide = new KeyGuide(); // 右下のキー案内
    /** 「オプション」を選んだときに、タイトルの上に重ねて出すパネル（ポーズメニューと同じもの） */
    private final OptionPanel optionPanel = new OptionPanel();
    /** 「遊び方」を選んだときに、タイトルの上に重ねて出す説明 */
    private final HowToPanel howToPanel = new HowToPanel();
    /** 「終了」を選んだときに、タイトルの上に重ねて出す確認（はい／いいえ） */
    private final ConfirmDialog exitConfirm = new ConfirmDialog("UI_EndChack_BG", "本当に終了しますか？");
    /** 1・2・3・4 を同時に押したときに出す、ベストスコアのリセットの確認（はい／いいえ） */
    private final ConfirmDialog resetConfirm = new ConfirmDialog(null, "ベストスコアをリセットしますか？");
    private static final KeyCode[] RESET_KEYS = { KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4 };
    private boolean debugNextBeforeScene;

    // BGMフェードアウト用の変数
    private boolean isStartingGame = false;
    private double fadeOutTime = 0.0;
    private static final double FADE_OUT_SECONDS = 0.6;
    private double savedBgmVolume;

    @Override
    public void onEnter() {
        // タイトルでは開発用のPキーによる画面送りを停止する。
        debugNextBeforeScene = Game.enterToNext;
        Game.enterToNext = false;
        menu.reset();
        entrance.reset();
        bird.reset();
        optionPanel.close();
        howToPanel.close();
        exitConfirm.close();
        resetConfirm.close();

        // タイトルBGMを開始
        Sound.bgm("BGM_Title");
        isStartingGame = false;
        fadeOutTime = 0.0;
    }

    @Override
    public void onExit() {
        Game.enterToNext = debugNextBeforeScene;
        // 画面を出る際、フェードアウトで下げた音量を元の設定値に戻す
        if (isStartingGame) {
            Sound.setBgmVolume(savedBgmVolume);
        }
    }

    @Override
    public void update() {
        entrance.update();
        bird.update();

        // スタート後の鳥の飛立ちに合わせてBGMをフェードアウトする
        if (isStartingGame) {
            fadeOutTime += Game.STEP;
            double progress = Math.clamp(fadeOutTime / FADE_OUT_SECONDS, 0.0, 1.0);
            Sound.setBgmVolume(savedBgmVolume * (1.0 - progress));
        }

        // 離場中は入力を受け付けない。
        if (bird.isDeparting()) {
            return;
        }
        if (!entrance.isFinished()) {
            return;
        }

        // オプションを開いている間は、オプションの操作だけを受け付ける（ESC でタイトルのメニューに戻る）
        if (optionPanel.isOpen()) {
            optionPanel.update();
            return;
        }

        // 遊び方を開いている間は、閉じる操作（ENTER・ESC）だけを受け付ける
        if (howToPanel.isOpen()) {
            howToPanel.update();
            return;
        }

        // 終了の確認を出している間は、確認の操作だけを受け付ける（「はい」で終了、「いいえ」・ESC でメニューに戻る）
        if (exitConfirm.isOpen()) {
            if (exitConfirm.update() == ConfirmDialog.Result.YES) {
                Platform.exit();
            }
            return;
        }

        // リセットの確認を出している間は、確認の操作だけを受け付ける（「はい」ですべての記録を消す）
        if (resetConfirm.isOpen()) {
            if (resetConfirm.update() == ConfirmDialog.Result.YES) {
                Records.clearAll();
            }
            return;
        }

        // 1・2・3・4 を同時に押したら、リセットの確認を出す（4つ目を押した瞬間に1回だけ）
        if (resetKeysPressed()) {
            resetConfirm.open();
            return;
        }

        switch (menu.update()) {
            case START -> startGame();
            case HOW_TO -> howToPanel.open();
            case OPTION -> optionPanel.open();
            case EXIT -> exitConfirm.open();
            case NONE -> {
            }
        }
    }

    /** 1・2・3・4 が全部押されていて、そのうちどれかをこのステップで押したところか。 */
    private static boolean resetKeysPressed() {
        boolean justPressed = false;
        for (KeyCode key : RESET_KEYS) {
            if (!Input.isDown(key))
                return false;
            if (Input.isPressed(key))
                justPressed = true;
        }
        return justPressed;
    }

    /** 鳥が右へ飛び出すのに合わせて、左から右へワイプして、ステージセレクトへ切り替える。 */
    private void startGame() {
        // 鳥の飛び立ちSE再生とBGMフェードアウト開始
        Sound.se("SE_BirdWings");
        savedBgmVolume = Sound.getBgmVolume();
        isStartingGame = true;
        fadeOutTime = 0.0;

        bird.startDeparture();
        // ★ 元通りの引数構成（StageSelectScene()）に戻し、ワイプ進行度を取得するクロージャをセット
        Game.wipeTo(new StageSelectScene(), () -> view.wipeProgress(bird));
    }

    @Override
    public void render(GraphicsContext gc) {
        view.render(gc, entrance, bird, menu.selectedIndex());
        keyGuide.render(gc);

        // オプションを開いている間は、タイトルを暗くして、その上にパネルを出す
        if (optionPanel.isOpen()) {
            gc.setFill(Color.rgb(0, 0, 0, 0.55));
            gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
            optionPanel.render(gc);
        }

        // 遊び方を開いている間も、タイトルを暗くして、その上に説明を出す
        if (howToPanel.isOpen()) {
            gc.setFill(Color.rgb(0, 0, 0, 0.55));
            gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
            howToPanel.render(gc);
        }

        // 終了・リセットの確認（タイトルは暗くせず、そのまま上に重ねる）
        exitConfirm.render(gc);
        resetConfirm.render(gc);
    }

    @Override
    public BaseScene next() {
        return new StageSelectScene();
    }
}