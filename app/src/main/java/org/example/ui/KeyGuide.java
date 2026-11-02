package org.example.ui;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.example.core.Game;
import org.example.core.ProjectFiles;
import org.example.media.Assets;

/**
 * 画面の右下に出すキー操作の案内（images/UI_Key.png「Enter 決定 ESC 戻る」）。
 * タイトル・ステージセレクトで使う。
 * 大きさと、画面の右端・下端からの隙間は HUMMING 直下の ui_config.properties で変えられる。
 * 画面を開くたび（new KeyGuide() のとき）に読むので、書き換えたら画面を移り直せば反映される。
 *
 * <pre>
 * Canvas に描く画面:   keyGuide.render(gc);              （render の最後のほうで呼ぶ）
 * ノードを並べる画面:   getChildren().add(keyGuide.createNode());
 * </pre>
 */
public final class KeyGuide {

    /** 設定ファイルの名前（HUMMING 直下） */
    public static final String FILE_NAME = "ui_config.properties";
    private static final String IMAGE = "UI_Key";

    /** 表示する横幅（px）。高さは画像の縦横比で決まる */
    private double width = 470;
    /** 画面の右端から画像の右端までの隙間（px） */
    private double marginX = 40;
    /** 画面の下端から画像の下端までの隙間（px） */
    private double marginY = 30;

    public KeyGuide() {
        load(ProjectFiles.resolve(FILE_NAME));
    }

    /** Canvas の右下に描く。画像が無いときは何もしない。 */
    public void render(GraphicsContext gc) {
        Image image = Assets.image(IMAGE);
        if (image == null) return;
        double height = heightOf(image);
        gc.drawImage(image, Game.WIDTH - marginX - width, Game.HEIGHT - marginY - height, width, height);
    }

    /** 右下に置いた ImageView を作る（ステージセレクトのように、Canvas の上にノードを重ねる画面用）。 */
    public ImageView createNode() {
        ImageView view = new ImageView();
        view.setMouseTransparent(true);
        Image image = Assets.image(IMAGE);
        if (image == null) return view;
        view.setImage(image);
        view.setFitWidth(width);
        view.setFitHeight(heightOf(image));
        view.setLayoutX(Game.WIDTH - marginX - width);
        view.setLayoutY(Game.HEIGHT - marginY - heightOf(image));
        return view;
    }

    private double heightOf(Image image) {
        return width * image.getHeight() / image.getWidth();
    }

    /** file から読む。無いときや書き間違えた項目は、上の初期値のまま。 */
    private void load(Path file) {
        if (!Files.exists(file)) {
            System.err.println("[KeyGuide] " + FILE_NAME + " がありません。初期値で動きます: " + file);
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            System.err.println("[KeyGuide] 読み込みエラー: " + e.getMessage());
            return;
        }
        width = Math.max(1, number(props, "keyGuide.width", width));
        marginX = number(props, "keyGuide.marginX", marginX);
        marginY = number(props, "keyGuide.marginY", marginY);
    }

    private static double number(Properties props, String key, double def) {
        String value = props.getProperty(key);
        if (value == null) return def;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("[KeyGuide] " + key + " の値が数ではありません: " + value);
            return def;
        }
    }
}
