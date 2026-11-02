package org.example.game.flower;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javafx.scene.paint.Color;

import org.example.core.ProjectFiles;

/**
 * 吸蜜エフェクト（蜜の粒）の設定。HUMMING 直下の effect_config.properties から読む。
 * ファイルが無いとき・書き間違えた項目は、下の初期値を使う（ファイルは作らない）。
 * ゲーム画面で R キーを押すと読み直す（FeedingEffect が呼ぶ）。
 */
public class FeedingEffectConfig {

    /** 設定ファイルの名前（HUMMING 直下） */
    public static final String FILE_NAME = "effect_config.properties";

    /** 1秒に出る粒の数 */
    public double rate = 25;
    /** 吸い始めにはじける粒の数（0 ではじけない） */
    public int burstCount = 14;
    /** 花からくちばしに届く秒数 */
    public double travelSeconds = 0.4;
    /** くちばしの上を顔まで流れる秒数 */
    public double slideSeconds = 0.28;
    /** 粒の大きさ（半径 px）の範囲 */
    public double sizeMin = 3.0;
    public double sizeMax = 6.0;
    /** 白く光る粒の割合（0.0〜1.0） */
    public double sparkleRatio = 0.25;
    /** 十字のきらめきが1秒に出る数（0 で出ない） */
    public double crossPerSecond = 6;
    /** 粒が出る範囲の半径（花の入り口から、px） */
    public double spawnRadius = 19;
    /** 粒の色（しずくの粒はこの中からランダムに選ぶ） */
    public List<Color> colors = List.of(Color.web("#ffd166"), Color.web("#ffa502"));

    /** HUMMING 直下の effect_config.properties を読む。 */
    public static FeedingEffectConfig load() {
        FeedingEffectConfig config = new FeedingEffectConfig();
        config.reload();
        return config;
    }

    /** HUMMING 直下の effect_config.properties を読み直す。 */
    public void reload() {
        loadFrom(ProjectFiles.resolve(FILE_NAME));
    }

    /** file から読む。無いときや読めないときは、今の値のまま警告を出す。 */
    public void loadFrom(Path file) {
        if (!Files.exists(file)) {
            System.err.println("[FeedingEffectConfig] " + FILE_NAME + " がありません。初期値で動きます: " + file);
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            System.err.println("[FeedingEffectConfig] 読み込みエラー: " + e.getMessage());
            return;
        }
        apply(props);
    }

    /** 読んだ値を当てはめる。書き間違えた項目は今の値のまま。 */
    void apply(Properties props) {
        rate = Math.max(0, number(props, "feeding.rate", rate));
        burstCount = (int) Math.max(0, number(props, "feeding.burstCount", burstCount));
        travelSeconds = Math.max(0.05, number(props, "feeding.travelSeconds", travelSeconds));
        slideSeconds = Math.max(0.01, number(props, "feeding.slideSeconds", slideSeconds));
        sizeMin = Math.max(0.1, number(props, "feeding.sizeMin", sizeMin));
        sizeMax = Math.max(sizeMin, number(props, "feeding.sizeMax", sizeMax));
        sparkleRatio = Math.clamp(number(props, "feeding.sparkleRatio", sparkleRatio), 0, 1);
        crossPerSecond = Math.max(0, number(props, "feeding.crossPerSecond", crossPerSecond));
        spawnRadius = Math.max(0, number(props, "feeding.spawnRadius", spawnRadius));
        colors = colors(props, "feeding.colors", colors);
    }

    private static double number(Properties props, String key, double def) {
        String value = props.getProperty(key);
        if (value == null) return def;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("[FeedingEffectConfig] " + key + " の値が数ではありません: " + value);
            return def;
        }
    }

    private static List<Color> colors(Properties props, String key, List<Color> def) {
        String value = props.getProperty(key);
        if (value == null) return def;
        List<Color> result = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                result.add(Color.web(part.trim()));
            } catch (IllegalArgumentException e) {
                System.err.println("[FeedingEffectConfig] " + key + " に色ではない値があります: " + part.trim());
            }
        }
        return result.isEmpty() ? def : List.copyOf(result);
    }
}
