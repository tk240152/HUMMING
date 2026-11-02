package org.example.media;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 画像・フォントをキー名で読み込む（音は Sound を使う）。
 * ファイルが無くてもエラーにならない（画像は仮の四角、フォントは標準）。
 *
 * 画像 : resources/images/{key}.png（key に "/" を含むときは resources/{key}.png。例:
 * images/icon/app_icon_16）
 * フォント: resources/fonts/{key}.ttf
 */
public final class Assets {

    private static final Map<String, Image> images = new HashMap<>();
    private static final Map<String, Font> fonts = new HashMap<>();

    private Assets() {
    }

    /** 画像を返す。無ければ null。 */
    public static Image image(String key) {
        return images.computeIfAbsent(key, k -> {
            String path = (k.contains("/") ? "/" : "/images/") + k;

            for (String ext : new String[] { ".png", ".jpg", ".jpeg" }) {
                URL url = Assets.class.getResource(path + ext);
                if (url != null) {
                    return new Image(url.toExternalForm());
                }
            }

            return null;
        });
    }

    /** 画像を描く。画像が無ければ、色付きの四角とキー名を仮表示する。 */
    public static void draw(GraphicsContext gc, String key, double x, double y, double w, double h, Color fallback) {
        Image img = image(key);
        if (img != null) {
            gc.drawImage(img, x, y, w, h);
            return;
        }
        gc.setFill(fallback);
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, w, h);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(26));
        gc.fillText(key, x + 9, y + 30);
    }

    /** 効果音を鳴らす。Sound.se(key) と同じ（音まわりは Sound を使うこと）。 */
    public static void play(String key) {
        Sound.se(key);
    }

    /** フォントを返す。ファイルが無ければ標準フォント。 */
    public static Font font(String key, double size) {
        return fonts.computeIfAbsent(key + "@" + size, k -> {
            try (InputStream in = Assets.class.getResourceAsStream("/fonts/" + key + ".ttf")) {
                Font f = in == null ? null : Font.loadFont(in, size);
                return f != null ? f : Font.font(size);
            } catch (Exception e) {
                return Font.font(size);
            }
        });
    }
}
