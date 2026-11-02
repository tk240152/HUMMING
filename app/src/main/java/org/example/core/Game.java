package org.example.core;

import java.util.function.DoubleSupplier;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javafx.scene.Group;
import javafx.scene.Node;

import org.example.media.Sound;

/**
 * ゲームループと画面切り替え。
 * update は PCの性能に関係なく 1秒に60回（固定ステップ）呼ばれる。
 * 調整値（速度など）はすべて「1ステップあたり」の値として書くこと。
 */
public final class Game {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final String TITLE = "HUMMING";

    /** 1ステップの長さ（秒） */
    public static final double STEP = 1.0 / 60.0;

    /** true の間は、どの画面でも P キーで次の画面へ進む（開発用）。 */
    public static boolean enterToNext = true;

    private static BaseScene current;
    private static BaseScene pending;

    /** Canvasの上に重ねるUI用レイヤー。 */
    private static Group uiLayer;

    /** ワイプ中に、新しく表示する画面。null ならワイプしていない。 */
    private static BaseScene wipeNext;

    /** ワイプの進み具合（0〜1）。画面の左端から、この割合までが新しい画面になる。 */
    private static DoubleSupplier wipeProgress;

    /** ワイプ中に、新しい画面のUI部品を、左側だけ見せるための枠。 */
    private static final Rectangle wipeClip = new Rectangle(0, 0, 0, HEIGHT);

    private Game() {
    }

    /** ゲームを開始する。App から1回だけ呼ぶ。 */
    public static void start(Canvas canvas, BaseScene first) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Appが作成したGroupにUI用レイヤーを追加する。
        uiLayer = new Group();
        ((Group) canvas.getParent()).getChildren().add(uiLayer);

        current = first;
        enter(current);

        new AnimationTimer() {
            private long last = -1;
            private double acc = 0;

            @Override
            public void handle(long now) {
                if (last < 0)
                    last = now;
                // 処理落ちしても一気に進みすぎないよう上限を付ける
                acc += Math.min((now - last) / 1e9, 0.25);
                last = now;

                while (acc >= STEP) {
                    step();
                    acc -= STEP;
                }

                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, WIDTH, HEIGHT);
                current.render(gc);

                // ワイプ中は、新しい画面を、左端から境目までだけ重ねて見せる
                if (wipeNext != null) {
                    double edge = WIDTH * Math.clamp(wipeProgress.getAsDouble(), 0.0, 1.0);

                    gc.save();
                    gc.beginPath();
                    gc.rect(0, 0, edge, HEIGHT);
                    gc.clip();
                    wipeNext.render(gc);
                    gc.restore();

                    
                    double blur = 200.0; // ぼかしの幅
                    gc.setFill(new javafx.scene.paint.LinearGradient(
                            edge - blur, 0, edge, 0, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                            new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                            new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(0, 0, 0, 0.4))
                    ));
                    gc.fillRect(edge - blur, 0, blur, HEIGHT);

                    // 新しい画面のUI部品も、同じ境目で左側だけ見せる
                    wipeClip.setWidth(edge);
                }
            }
        }.start();
    }

    /** 画面を切り替える。切り替えは現在のステップが終わってから行われる。 */
    public static void change(BaseScene next) {
        pending = next;
    }

    public static void wipeTo(BaseScene next, DoubleSupplier progress) {
        // ワイプ中は、重ねて始めない
        if (wipeNext != null) {
            return;
        }

        current.onExit();   // 前の画面の後片づけを先にする（Enterの設定などを戻す）
        enter(next);        // 新しい画面の部品を置く

        wipeNext = next;
        wipeProgress = progress;
        wipeClip.setWidth(0);
        uiLayer.setClip(wipeClip);
    }

    /** ワイプを終えて、新しい画面を今の画面にする。 */
    private static void finishWipe() {
        current = wipeNext;
        wipeNext = null;
        wipeProgress = null;
        uiLayer.setClip(null);
    }

    /** UIを表示する。nullを渡すと現在のUIを取り除く。 */
    public static void showUi(Node view) {
        uiLayer.getChildren().clear();

        if (view != null) {
            uiLayer.getChildren().add(view);
        }
    }

    private static void step() {

        // ワイプ中は、前の画面だけを更新する（新しい画面は、ワイプが終わるまで更新しない）
        if (wipeNext != null) {
            current.update();
            Input.endStep();

            if (wipeProgress.getAsDouble() >= 1.0) {
                finishWipe();
            }
            return;
        }

        if (DevMode.ENABLED && enterToNext && Input.isPressed(KeyCode.P)) { // 開発用の画面送り
            change(current.next());
        } else {
            current.update();
        }
        Input.endStep();

        if (pending != null) {
            current.onExit();
            current = pending;
            pending = null;
            enter(current);
        }
    }

    /** 画面に入る。onEnter のあと、その画面の BGM を流す（null なら前の BGM のまま）。 */
    private static void enter(BaseScene scene) {
        scene.onEnter();
        String bgm = scene.bgm();
        if (bgm != null) Sound.bgm(bgm);
    }
}