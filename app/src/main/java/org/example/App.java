package org.example;

import java.util.List;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.example.game.GameScene;
import org.example.ui.result.ResultScene;
import org.example.ui.title.TitleScene;
import org.example.core.BaseScene;
import org.example.core.Game;
import org.example.state.GameState;
import org.example.core.Input;
import org.example.media.Assets;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(Game.WIDTH, Game.HEIGHT);
        Group game = new Group(canvas); // ゲーム画面（Game が UI 用のレイヤーもここに足す）

        // ウィンドウの大きさに合わせて、縦横比を保ったまま拡大する。余った部分は黒
        // 外側の Group で包むと、拡大したあとの大きさで真ん中にそろえられる
        StackPane root = new StackPane(new Group(game));
        root.setStyle("-fx-background-color: black;");
        Scale scale = new Scale(1, 1, 0, 0);
        game.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            double s = Math.min(root.getWidth() / Game.WIDTH, root.getHeight() / Game.HEIGHT);
            scale.setX(s);
            scale.setY(s);
        };
        root.widthProperty().addListener(fit);
        root.heightProperty().addListener(fit);

        // 起動したときは半分の大きさ（1920×1080 のままだとノートPCの画面からはみ出るため）
        Scene scene = new Scene(root, Game.WIDTH / 2.0, Game.HEIGHT / 2.0);
        Input.attach(scene);

        primaryStage.setScene(scene);
        primaryStage.setTitle(Game.TITLE);
        primaryStage.setResizable(true); // 最大化ボタンを押せるようにする
        // タイトルバー・タスクバーのアイコン（images/icon/。大きさごとに用意して、場所に合うものを使わせる）
        for (int size : new int[] { 16, 32, 48, 256 }) {
            Image icon = Assets.image("images/icon/app_icon_" + size);
            if (icon != null) primaryStage.getIcons().add(icon);
        }

        // 最大化ボタンか F12 でフルスクリーンにする。F12 でもう一度押すと元の大きさのウィンドウに戻る
        // （Esc はゲームで使うので、フルスクリーンを抜けるキーにはしない）
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreenExitHint("F12 でウィンドウに戻ります");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F12) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                e.consume();
            }
        });
        primaryStage.maximizedProperty().addListener((o, was, now) -> {
            if (now)
                primaryStage.setFullScreen(true);
        });
        primaryStage.fullScreenProperty().addListener((o, was, now) -> {
            if (!now)
                primaryStage.setMaximized(false);
        });
        primaryStage.show();

        Game.start(canvas, firstScene(getParameters().getRaw()));
    }

    /** 起動時の引数で最初の画面を選ぶ（例: gradlew run --args=game）。 */
    private static BaseScene firstScene(List<String> args) {
        String name = args.isEmpty() ? "title" : args.get(0);
        return switch (name) {
            case "game" -> new GameScene();
            case "result" -> new ResultScene(new GameState());
            default -> new TitleScene();
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
