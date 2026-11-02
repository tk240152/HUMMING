package org.example.state;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.example.core.ProjectFiles;

/**
 * ステージごとのベストタイムの記録。ユーザーフォルダの .humming/records.properties に保存する
 * （例: C:/Users/〇〇/.humming/records.properties）。
 * 遊んだ人ごとの記録なので、プロジェクトの外に置いて git に入らないようにしている。
 * 前の場所（HUMMING 直下の records.properties）に記録があれば、最初の1回だけ引き継ぐ。
 * 配布したゲームでは、HUMMING.exe の隣の save/records.properties に保存する（ProjectFiles.saveDir()）。
 * 配布したゲームは何も引き継がず、ファイルが無ければ記録なしで始まる。
 *
 * <pre>
 * Records.hasBest(1)          // ステージ1の記録があるか
 * Records.best(1)             // ステージ1のベストタイム（秒）。記録が無ければ 0
 * Records.submit(1, 12.34)    // クリアタイムを渡す。ベスト更新なら保存して true
 * Records.format(12.34)       // 表示用の文字列 "12.34"
 * Records.clearAll()          // すべての記録を消す（タイトルで 1・2・3・4 を同時に押したときのリセット）
 * </pre>
 *
 * タイムは 1/100 秒単位（小数第2位まで）で扱う。
 */
public final class Records {

    static final String FILE_NAME = "records.properties";

    private static Path file;
    private static Properties props;
    private static boolean useDefault = true;

    private Records() {}

    /** その stage の記録があるか。 */
    public static boolean hasBest(int stage) {
        return bestCentis(stage) >= 0;
    }

    /** その stage のベストタイム（秒）。記録が無ければ 0。 */
    public static double best(int stage) {
        long c = bestCentis(stage);
        return c < 0 ? 0 : c / 100.0;
    }

    /**
     * クリアタイムを記録する。今までの記録より速い（または初めてのクリア）ならファイルに保存して true を返す。
     */
    public static boolean submit(int stage, double seconds) {
        long c = toCentis(seconds);
        long old = bestCentis(stage);
        if (old >= 0 && c >= old) return false;

        load().setProperty(key(stage), format(seconds));
        save();
        return true;
    }

    /** すべてのステージの記録（ベストタイムと、クリアしたかどうか）を消して保存する。 */
    public static void clearAll() {
        load().clear();
        save(); // 空のファイルを書いておく（前の場所の記録を、もう一度引き継がないように）
    }

    /** 秒を表示用の文字列にする（例: 12.34）。小数第2位まで、頭に 0 は付けない。 */
    public static String format(double seconds) {
        long c = toCentis(seconds);
        return (c / 100) + "." + String.format("%02d", c % 100);
    }

    // ---- 内部 ----

    private static String key(int stage) {
        return "stage" + stage + ".best";
    }

    /** 小数の誤差で 12.34 が 12.33 にならないよう、少しだけ足してから切り捨てる。 */
    private static long toCentis(double seconds) {
        return (long) Math.floor(Math.max(0, seconds) * 100 + 1e-6);
    }

    /** 記録（1/100 秒単位）。無い・読めないときは -1。 */
    private static long bestCentis(int stage) {
        String v = load().getProperty(key(stage));
        if (v == null) return -1;
        try {
            return toCentis(Double.parseDouble(v.trim()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Properties load() {
        if (props != null) return props;
        props = new Properties();
        if (file == null) file = defaultFile();
        // 新しい場所にまだ無ければ、前の場所（HUMMING 直下）の記録を読んで引き継ぐ（前のファイルは消さない）
        Path source = Files.exists(file) ? file : legacyFile();
        if (source != null && Files.exists(source)) {
            try (Reader r = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException e) {
                System.err.println("[Records] 読み込みエラー: " + e.getMessage());
            }
        }
        return props;
    }

    private static void save() {
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[Records] 保存先のフォルダを作れません: " + e.getMessage());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(w, "HUMMING best times (seconds)");
        } catch (IOException e) {
            System.err.println("[Records] 保存エラー: " + e.getMessage());
        }
    }

    /** ふだんの保存先: ユーザーフォルダの .humming/records.properties（配布したゲームでは save/records.properties） */
    static Path defaultFile() {
        if (ProjectFiles.isDistributed()) return ProjectFiles.saveDir().resolve(FILE_NAME);
        return Path.of(System.getProperty("user.home"), ".humming", FILE_NAME);
    }

    /** 前の保存先（HUMMING 直下）。テストで保存先を差し替えたとき・配布したゲームでは引き継がない。 */
    private static Path legacyFile() {
        return useDefault && !ProjectFiles.isDistributed() ? ProjectFiles.resolve(FILE_NAME) : null;
    }

    /** テスト用: 保存先を差し替えて、読み込み済みの記録を捨てる。 */
    static void useFile(Path path) {
        file = path;
        useDefault = false;
        props = null;
    }
}
