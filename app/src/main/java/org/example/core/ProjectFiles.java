package org.example.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * HUMMING 直下（settings.gradle がある場所）にある設定ファイルの場所を決める。
 * gradlew run・VS Code の Run・どのフォルダを開いて起動しても、必ず同じ HUMMING 直下を指す。
 *
 * <pre>
 * Path p = ProjectFiles.resolve("player_config.properties");   // HUMMING/player_config.properties
 * </pre>
 *
 * 探し方:
 * 1. このプログラム（クラスファイル）が置かれている場所から上へたどって settings.gradle を探す
 *    （build/classes/… でも bin/… でも、HUMMING の中にあるので必ず見つかる）
 * 2. 見つからなければ、起動した場所（作業フォルダ）から上へたどって探す
 * 3. それでも見つからなければ、配布したゲームとして動いている。
 *    保存先（saveDir()）の config フォルダを使い、ビルドのときに組み込んだ設定ファイル（jar の /config/）をそこへ書き出す
 *    （player_config.properties などの調整値が、配布先でも同じになるように）
 *
 * 配布したゲームの保存先は、HUMMING.exe の隣の save フォルダ（ベストタイムと調整ファイルを置く）。
 * そこに書き込めないとき（C:/Program Files に置いたときなど）は、ユーザーフォルダの .humming を使う。
 */
public final class ProjectFiles {

    private static final String MARKER = "settings.gradle";
    /** 設定ファイルを組み込んである場所（app/build.gradle の processResources で入れている） */
    private static final String EMBEDDED = "/config/";

    private static Path root;
    private static Path saveDir;
    /** 配布したゲームとして動いているか（HUMMING のフォルダが見つからなかったか） */
    private static boolean distributed;
    /** このあいだに書き出した組み込みの設定ファイル */
    private static final Set<String> extracted = new HashSet<>();

    private ProjectFiles() {}

    /** HUMMING 直下にある name のファイルの場所。配布したゲームでは、組み込みの設定ファイルを書き出した場所。 */
    public static synchronized Path resolve(String name) {
        Path path = root().resolve(name);
        if (distributed && extracted.add(name)) extract(name, path);
        return path;
    }

    /** HUMMING 直下のフォルダ。配布したゲームでは、保存先（saveDir()）の config フォルダ。 */
    public static synchronized Path root() {
        if (root == null) root = findRoot();
        return root;
    }

    /** 配布したゲームとして動いているか（HUMMING のフォルダが見つからなかったか）。 */
    public static synchronized boolean isDistributed() {
        root();
        return distributed;
    }

    /**
     * 配布したゲームの保存先。HUMMING.exe の隣の save フォルダ。
     * jar から起動していないとき・そこに書き込めないときは、ユーザーフォルダの .humming。
     */
    public static synchronized Path saveDir() {
        if (saveDir == null) saveDir = findSaveDir();
        return saveDir;
    }

    private static Path findRoot() {
        Path found = searchUp(codeLocation());
        if (found == null) found = searchUp(Path.of("").toAbsolutePath());
        if (found != null) return found;

        distributed = true;
        return saveDir().resolve("config");
    }

    private static Path findSaveDir() {
        Path home = Path.of(System.getProperty("user.home"), ".humming");
        // 配布したフォルダでは、このプログラムは HUMMING/app/app.jar にある。その2つ上が HUMMING.exe のあるフォルダ
        Path jar = codeLocation();
        if (jar == null || !Files.isRegularFile(jar) || jar.getParent() == null || jar.getParent().getParent() == null)
            return home;
        Path dir = jar.getParent().getParent().resolve("save");
        if (canWrite(dir)) return dir;
        System.err.println("[ProjectFiles] " + dir + " に書き込めないので、" + home + " に保存します");
        return home;
    }

    /** dir を作って、ためしにファイルを書いて消せるか。 */
    private static boolean canWrite(Path dir) {
        try {
            Files.createDirectories(dir);
            Files.delete(Files.createTempFile(dir, "write-check", ".tmp"));
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    /**
     * 組み込みの設定ファイル（jar の /config/name）があれば、path へ書き出す（起動するたびに上書きして、新しい版の値にそろえる）。
     * 組み込みに無いファイル（stage.json など）は何もしない（それぞれ組み込みのものを直接読む）。
     */
    private static void extract(String name, Path path) {
        try (InputStream in = ProjectFiles.class.getResourceAsStream(EMBEDDED + name)) {
            if (in == null) return;
            Files.createDirectories(path.getParent());
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[ProjectFiles] 設定ファイルを書き出せません: " + path + " (" + e.getMessage() + ")");
        }
    }

    /** start から上へたどって、settings.gradle があるフォルダを返す。無ければ null。 */
    static Path searchUp(Path start) {
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve(MARKER))) return dir;
        }
        return null;
    }

    /** このクラスが読み込まれた場所（build/classes/java/main や jar）。分からなければ null。 */
    private static Path codeLocation() {
        try {
            var source = ProjectFiles.class.getProtectionDomain().getCodeSource();
            return source == null ? null : Path.of(source.getLocation().toURI()).toAbsolutePath();
        } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
}
