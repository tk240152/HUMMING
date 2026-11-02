package org.example.media;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * 効果音・BGM の再生と音量。
 * ファイルが無い・読み込めないときは何もしない（警告をキーごとに1回だけ出す）。
 *
 * 効果音: resources/sounds/se/{key}.wav（無ければ .mp3。key に "/" を含むときは resources/{key}.wav）
 * BGM   : resources/sounds/bgm/{key}.mp3（無ければ .wav。key に "/" を含むときは resources/{key}.mp3）
 *
 * 実際の音量 = 全体の音量 × 種類ごとの音量 × 音ごとの倍率（× 1回ごとの倍率）。ミュート中は 0。
 * 音ごとの倍率は resources/sounds/volumes.properties に「key=倍率」で書く（書いていない音は 1.0）。
 * 素材の音の大きさがばらばらなときに、大きい音を下げてそろえるためのもの（1.0 より大きくはできない）。
 */
public final class Sound {

    public static final double DEFAULT_MASTER_VOLUME = 1.0;
    public static final double DEFAULT_BGM_VOLUME = 0.6;
    public static final double DEFAULT_SE_VOLUME = 1.0;

    private static double masterVolume = DEFAULT_MASTER_VOLUME;
    private static double bgmVolume = DEFAULT_BGM_VOLUME;
    private static double seVolume = DEFAULT_SE_VOLUME;
    private static boolean muted;

    private static final Map<String, AudioClip> clips = new HashMap<>();
    private static final Set<String> warned = new HashSet<>();

    /** 音ごとの倍率の表の場所（resources からの場所） */
    static final String VOLUMES_FILE = "/sounds/volumes.properties";
    /** 音ごとの倍率。最初に使うときに読む */
    private static Properties volumes;

    // 鳴っている長めの効果音（longSe）と、その音のキー（音量を変えたときに使う）。フィールドで持たないと GC で止まる
    private static final Map<MediaPlayer, String> longSePlayers = new HashMap<>();
    private static final Map<String, Media> longSeMedia = new HashMap<>();
    /** 長めの効果音ごとの音量の倍率（loopSe の scale。longSe は 1.0） */
    private static final Map<MediaPlayer, Double> longSeScales = new HashMap<>();
    private static boolean longSePaused;

    // 再生中の BGM。フィールドで持たないと GC で止まる
    private static MediaPlayer bgmPlayer;
    private static String bgmKey;

    private Sound() {}

    // ---- 効果音 ----

    /** 効果音を鳴らす。 */
    public static void se(String key) {
        se(key, 1.0);
    }

    /** 効果音を鳴らす。scale はこの1回だけの音量の倍率。 */
    public static void se(String key, double scale) {
        AudioClip clip = clips.computeIfAbsent(key, k -> {
            URL url = find((k.contains("/") ? "/" : "/sounds/se/") + k, ".wav", ".mp3");
            if (url == null) return null;
            try {
                return new AudioClip(url.toExternalForm());
            } catch (Exception e) {
                warn("se:" + k, e);
                return null;
            }
        });
        if (clip != null) clip.play(clamp(effectiveSeVolume() * clamp(scale) * gain(key)));
    }

    /**
     * 長めの効果音を鳴らす（風・鳥の群れなど、数秒続く音）。置き場所と音量は se と同じ。
     * se と違って、pauseSe で一時停止・resumeSe で続きから鳴らせる（ゲーム画面のポーズで止まる）。
     * 鳴らし始めが se より少し遅れることがあるので、短い音は se を使う。
     */
    public static void longSe(String key) {
        playLongSe(key, 1.0, false);
    }

    /**
     * 効果音を、止めるまでくり返し鳴らす（雨など、ずっと続く音）。scale はこの音の音量の倍率。
     * longSe と同じく、pauseSe・resumeSe・stopSe が効く。途中で止めるときは stopLongSe(key)。
     * 音のファイルは、終わりと始まりがつながるように作る（つなぎ目に無音があると途切れて聞こえる）。
     */
    public static void loopSe(String key, double scale) {
        playLongSe(key, scale, true);
    }

    /** key の長めの効果音（longSe・loopSe）を止める。 */
    public static void stopLongSe(String key) {
        for (MediaPlayer p : new ArrayList<>(longSePlayers.keySet())) {
            if (key.equals(longSePlayers.get(p))) disposeLongSe(p);
        }
    }

    private static void playLongSe(String key, double scale, boolean loop) {
        URL url = find((key.contains("/") ? "/" : "/sounds/se/") + key, ".wav", ".mp3");
        if (url == null) return;
        try {
            Media media = longSeMedia.computeIfAbsent(key, k -> new Media(url.toExternalForm()));
            MediaPlayer player = new MediaPlayer(media);
            longSePlayers.put(player, key);
            longSeScales.put(player, clamp(scale));
            player.setVolume(clamp(effectiveSeVolume() * gain(key) * clamp(scale)));
            if (loop) {
                player.setCycleCount(MediaPlayer.INDEFINITE);
            } else {
                player.setOnEndOfMedia(() -> disposeLongSe(player)); // 鳴り終わったら片づける
            }
            player.setOnError(() -> {
                warn("se:" + key, player.getError());
                disposeLongSe(player);
            });
            if (!longSePaused) player.play();
        } catch (Exception e) {
            warn("se:" + key, e);
        }
    }

    /** 鳴っている長めの効果音（longSe）を一時停止する。止めている間に鳴らした longSe も、resumeSe まで待つ。 */
    public static void pauseSe() {
        longSePaused = true;
        for (MediaPlayer p : longSePlayers.keySet()) p.pause();
    }

    /** 一時停止した長めの効果音（longSe）を続きから鳴らす。 */
    public static void resumeSe() {
        longSePaused = false;
        for (MediaPlayer p : longSePlayers.keySet()) p.play();
    }

    /** 長めの効果音（longSe）および再生中の効果音（AudioClip）をすべて止める。 */
    public static void stopSe() {
        // 1. AudioClip (SE) の再生を全停止
        for (AudioClip clip : clips.values()) {
            if (clip != null) {
                
                clip.stop();
            }
        }

        // 2. MediaPlayer (longSe) の再生を全停止
        longSePaused = false;
        for (MediaPlayer p : new ArrayList<>(longSePlayers.keySet())) {
            disposeLongSe(p);
        }
    }
    // ---- BGM ----

    /** BGM をループ再生する。同じキーなら鳴らし続け、違うキーなら切り替える。 */
    public static void bgm(String key) {
        if (key.equals(bgmKey)) return;
        stopBgm();
        bgmKey = key;

        URL url = find((key.contains("/") ? "/" : "/sounds/bgm/") + key, ".mp3", ".wav");
        if (url == null) return;
        try {
            MediaPlayer player = new MediaPlayer(new Media(url.toExternalForm()));
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(clamp(effectiveBgmVolume() * gain(key)));
            player.setOnError(() -> warn("bgm:" + key, player.getError()));
            player.play();
            bgmPlayer = player;
        } catch (Exception e) {
            warn("bgm:" + key, e);
            bgmPlayer = null;
        }
    }

    /** BGM を一時停止する。 */
    public static void pauseBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.pause();
        }
    }

    /** BGM を再開する。 */
    public static void resumeBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.play();
        }
    }

    /** BGM を止める。 */
    public static void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
        bgmKey = null;
    }
    

    // ---- 音量（0.0〜1.0） ----

    public static double getMasterVolume() { return masterVolume; }
    public static double getBgmVolume() { return bgmVolume; }
    public static double getSeVolume() { return seVolume; }
    public static boolean isMuted() { return muted; }

    public static void setMasterVolume(double v) {
        masterVolume = clamp(v);
        applyBgmVolume();
        applyLongSeVolume();
    }

    public static void setBgmVolume(double v) {
        bgmVolume = clamp(v);
        applyBgmVolume();
    }

    public static void setSeVolume(double v) {
        seVolume = clamp(v);
        applyLongSeVolume();
    }

    public static void setMuted(boolean m) {
        muted = m;
        applyBgmVolume();
        applyLongSeVolume();
    }

    /** 実際に BGM に掛かる音量。 */
    public static double effectiveBgmVolume() {
        return muted ? 0 : masterVolume * bgmVolume;
    }

    /** 実際に効果音に掛かる音量。 */
    public static double effectiveSeVolume() {
        return muted ? 0 : masterVolume * seVolume;
    }

    /** その音だけの倍率（volumes.properties の値）。書いていない・読めない値は 1.0。 */
    public static double gain(String key) {
        if (volumes == null) volumes = loadVolumes();
        String value = volumes.getProperty(key);
        if (value == null) return 1.0;
        try {
            return clamp(Double.parseDouble(value.strip()));
        } catch (NumberFormatException e) {
            warn("volumes:" + key, e);
            return 1.0;
        }
    }

    // ---- 内部 ----

    private static void applyBgmVolume() {
        if (bgmPlayer != null) bgmPlayer.setVolume(clamp(effectiveBgmVolume() * gain(bgmKey)));
    }

    private static void applyLongSeVolume() {
        longSePlayers.forEach((p, key) ->
                p.setVolume(clamp(effectiveSeVolume() * gain(key) * longSeScales.getOrDefault(p, 1.0))));
    }

    private static void disposeLongSe(MediaPlayer player) {
        longSeScales.remove(player);
        if (longSePlayers.remove(player) == null) return;
        player.stop();
        player.dispose();
    }

    /** 音ごとの倍率の表を読む。ファイルが無ければ空の表（すべて 1.0）。 */
    static Properties loadVolumes() {
        Properties p = new Properties();
        try (InputStream in = Sound.class.getResourceAsStream(VOLUMES_FILE)) {
            if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            warn("volumes", e);
        }
        return p;
    }

    private static double clamp(double v) {
        return Math.clamp(v, 0.0, 1.0);
    }

    private static URL find(String base, String... exts) {
        for (String ext : exts) {
            URL url = Sound.class.getResource(base + ext);
            if (url != null) return url;
        }
        return null;
    }

    private static void warn(String id, Exception e) {
        if (warned.add(id)) System.err.println("[Sound] 読み込めませんでした: " + id + " (" + e + ")");
    }
}