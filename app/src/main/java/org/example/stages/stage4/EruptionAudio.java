package org.example.stages.stage4;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import org.example.media.Sound;

/**
 * 噴火の効果音（EruptionEvent の部品）。噴火の音・落石の音（ループ）・当たった音を鳴らす。
 * ファイルが無い音は鳴らさずに進む。BGM はステージの stage.json の bgm で決める。
 */
public class EruptionAudio {

    // ===== 各音の音量：共通の音量設定に掛け合わせる =====
    private static final double ERUPTION_VOLUME = 0.8;
    private static final double FALLING_VOLUME = 0.45;
    private static final double HIT_VOLUME = 0.8;

    // 音の置き場所（例: /sounds/se/）と、拡張子を含むファイル名。空文字ならその音を使用しない。
    private final String root;
    private final String eruptionFile;
    private final String fallingFile;
    private final String hitFile;

    private final Set<String> warned = new HashSet<>();
    private AudioClip startClip;
    private AudioClip eruptionClip;
    private AudioClip hitClip;
    private MediaPlayer fallingPlayer;
    private boolean fallingPlaying = false;
    private boolean audioPaused = false;

    public EruptionAudio(String soundFolder, String eruptionFile, String fallingFile, String hitFile) {
        this.root = "/" + soundFolder;
        this.eruptionFile = eruptionFile;
        this.fallingFile = fallingFile;
        this.hitFile = hitFile;
    }

    /** イベント開始時に音を準備する。 */
    public void start() {
        stop();

        eruptionClip = loadClip(eruptionFile);
        hitClip = loadClip(hitFile);
        fallingPlayer = loadLoop(fallingFile);
        updateVolumes();
    }

    /** Ready中に開始音を読み込んでおく。 */
    public void prepareStart(String file) {
        if (startClip != null) {
            startClip.stop();
        }

        startClip = loadClip(file);
    }


    /** 噴火が始まった瞬間に、一度だけ鳴らす。 */
    public void playEruption() {
        playClip(eruptionClip, eruptionFile, ERUPTION_VOLUME);
    }

    /** 衝突が受理されたときに、一度だけ鳴らす。 */
    public void playHit() {
        playClip(hitClip, hitFile, HIT_VOLUME);
    }

    /** 岩が落下している間だけループ再生する。 */
    public void setRumbling(boolean falling) {
        if (fallingPlayer == null || fallingPlaying == falling) {
            return;
        }

        try {
            if (falling) {
                fallingPlayer.play();
            } else {
                fallingPlayer.stop();
            }
            fallingPlaying = falling;
        } catch (RuntimeException e) {
            warn(fallingFile, e.getMessage());
            dispose(fallingPlayer);
            fallingPlayer = null;
            fallingPlaying = false;
        }
    }

    /** 共通設定の音量変更とミュートを、ループ音へ反映する。 */
    public void updateVolumes() {
        if (fallingPlayer != null) {
            fallingPlayer.setVolume(volume(
                    Sound.effectiveSeVolume(), FALLING_VOLUME));
        }
    }

    /** ループ音を一時停止し、単発音は止める。 */
    public void pause() {
        if (audioPaused) {
            return;
        }
        audioPaused = true;

        if (startClip != null)
            startClip.stop();
        if (eruptionClip != null)
            eruptionClip.stop();
        if (hitClip != null)
            hitClip.stop();

        if (fallingPlayer != null && fallingPlaying) {
            fallingPlayer.pause();
        }
    }

    /** 一時停止前に鳴っていたループ音だけ再開する。 */
    public void resume() {
        if (!audioPaused) {
            return;
        }
        audioPaused = false;

        updateVolumes();

        if (fallingPlayer != null && fallingPlaying) {
            fallingPlayer.play();
        }
    }

    /** イベントが終わるとき（ステージを抜けるとき）に、音をすべて止める。 */
    public void stop() {
        audioPaused = false;
        if (startClip != null)
            startClip.stop();
        if (eruptionClip != null)
            eruptionClip.stop();
        if (hitClip != null)
            hitClip.stop();

        dispose(fallingPlayer);

        fallingPlayer = null;
        fallingPlaying = false;
    }

    /** 単発の効果音を読み込む。素材がなくてもゲームは続行する。 */
    private AudioClip loadClip(String file) {
        URL resource = resource(file);
        if (resource == null)
            return null;

        try {
            return new AudioClip(resource.toExternalForm());
        } catch (RuntimeException e) {
            warn(file, e.getMessage());
            return null;
        }
    }

    /** 持続音として使用する、ループ再生用の音声。 */
    private MediaPlayer loadLoop(String file) {
        URL resource = resource(file);
        if (resource == null)
            return null;

        try {
            MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setOnError(() -> warn(file,
                    String.valueOf(player.getError())));
            return player;
        } catch (RuntimeException e) {
            warn(file, e.getMessage());
            return null;
        }
    }

    /** 共通の音量と、この音だけの倍率から実際の音量を求める。 */
    private static double volume(double common, double local) {
        return Math.clamp(common * local, 0.0, 1.0);
    }

    /** 現在の共通音量で単発音を鳴らす。毎フレームは呼ばない。 */
    private void playClip(AudioClip clip, String file, double localVolume) {
        if (clip == null)
            return;
        try {
            clip.play(volume(Sound.effectiveSeVolume(), localVolume));
        } catch (RuntimeException e) {
            warn(file, e.getMessage());
        }
    }

    private URL resource(String file) {
        if (file == null || file.isBlank())
            return null;

        URL resource = EruptionAudio.class.getResource(root + file);
        if (resource == null) {
            warn(file, "ファイルがないため、この音をスキップします。");
        }
        return resource;
    }

    private static void dispose(MediaPlayer player) {
        if (player != null) {
            player.stop();
            player.dispose();
        }
    }

    private void warn(String file, String message) {
        if (warned.add(file)) {
            System.err.println("[EruptionAudio] " + file + ": " + message);
        }
    }
}
