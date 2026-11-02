package org.example.ui.stageselect.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javafx.geometry.Point2D;
import javafx.scene.layout.Region;

import org.example.core.Game;

/**
 * チーム共通レイアウトの読み込みと出力を担当する。
 *
 * 出力したテキストを設定ファイルへ貼り付け、
 * Gitで共有することで、全員が同じ配置を使用できる。
 */
public final class TeamLayout {

    private static final String RESOURCE =
            "/stage_layout/stage-detail-layout.properties";

    private TeamLayout() {
    }

    /**
     * ゲームに同梱された共通設定を読む。
     * ファイルがまだない場合は、Javaで指定した初期位置を使用する。
     *
     * @return 共通設定を読み込んだ場合はtrue
     */
    public static boolean load(
            Map<String, Region> modules) throws IOException {

        try (InputStream input =
                TeamLayout.class.getResourceAsStream(RESOURCE)) {

            if (input == null) {
                return false;
            }

            Properties properties = new Properties();

            // 日本語のコメントなども扱えるようUTF-8で読む。
            try (InputStreamReader reader = new InputStreamReader(
                    input, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            applyLayout(modules, properties);
            return true;
        }
    }

    /**
     * 現在の位置を、設定ファイルへ貼り付けられる形式にする。
     * 個人用のファイルは作成しない。
     */
    public static String exportLayout(Map<String, Region> modules) {
        StringBuilder text = new StringBuilder();

        text.append("# HUMMING stage detail layout\n");
        text.append("format.version=1\n\n");

        // 登録順に出力し、Gitで変更箇所を比較しやすくする。
        for (Map.Entry<String, Region> entry : modules.entrySet()) {
            String id = entry.getKey();
            Region module = entry.getValue();

            // 小数点がPCの地域設定に影響されない形式で出力する。
            text.append(id)
                    .append(".x=")
                    .append(Double.toString(module.getLayoutX()))
                    .append('\n');

            text.append(id)
                    .append(".y=")
                    .append(Double.toString(module.getLayoutY()))
                    .append("\n\n");
        }

        return text.toString();
    }

    /**
     * 全項目を確認してから、一括で位置を変更する。
     * 途中で不正な項目が見つかった場合は、現在位置を変更しない。
     */
    static void applyLayout(
            Map<String, Region> modules,
            Properties properties) {

        if (!"1".equals(properties.getProperty("format.version"))) {
            throw new IllegalArgumentException(
                    "対応していないレイアウト形式です。");
        }

        Map<Region, Point2D> positions = new LinkedHashMap<>();

        for (Map.Entry<String, Region> entry : modules.entrySet()) {
            String id = entry.getKey();
            Region module = entry.getValue();

            double x = coordinate(properties, id + ".x");
            double y = coordinate(properties, id + ".y");

            // 初回レイアウト前でも使えるよう、推奨サイズで確認する。
            double maxX = Math.max(
                    0, Game.WIDTH - module.prefWidth(-1));

            double maxY = Math.max(
                    0, Game.HEIGHT - module.prefHeight(-1));

            if (x < 0 || x > maxX || y < 0 || y > maxY) {
                throw new IllegalArgumentException(
                        id + " の位置が画面の範囲外です。");
            }

            positions.put(module, new Point2D(x, y));
        }

        // 全項目が正しい場合にだけ、実際の配置を変更する。
        for (Map.Entry<Region, Point2D> entry : positions.entrySet()) {
            entry.getKey().relocate(
                    entry.getValue().getX(),
                    entry.getValue().getY()
            );
        }
    }

    /** 指定された項目を座標として読み取る。 */
    private static double coordinate(
            Properties properties, String key) {

        String text = properties.getProperty(key);

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    key + " がありません。");
        }

        try {
            double value = Double.parseDouble(text.strip());

            // NaNやInfinityを座標として使用させない。
            if (!Double.isFinite(value)) {
                throw new NumberFormatException();
            }

            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    key + " は有限の数値で指定してください。", e);
        }
    }
}
