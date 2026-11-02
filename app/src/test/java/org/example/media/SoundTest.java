package org.example.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SoundTest {

    @AfterEach
    void reset() {
        Sound.setMasterVolume(Sound.DEFAULT_MASTER_VOLUME);
        Sound.setBgmVolume(Sound.DEFAULT_BGM_VOLUME);
        Sound.setSeVolume(Sound.DEFAULT_SE_VOLUME);
        Sound.setMuted(false);
    }

    @Test
    void volumeIsClampedToZeroAndOne() {
        Sound.setMasterVolume(1.5);
        Sound.setBgmVolume(-0.2);
        assertEquals(1.0, Sound.getMasterVolume());
        assertEquals(0.0, Sound.getBgmVolume());
    }

    @Test
    void effectiveVolumeIsMasterTimesCategory() {
        Sound.setMasterVolume(0.5);
        Sound.setBgmVolume(0.6);
        Sound.setSeVolume(0.8);
        assertEquals(0.3, Sound.effectiveBgmVolume(), 1e-9);
        assertEquals(0.4, Sound.effectiveSeVolume(), 1e-9);
    }

    @Test
    void muteMakesEffectiveVolumeZero() {
        Sound.setMuted(true);
        assertEquals(0.0, Sound.effectiveBgmVolume());
        assertEquals(0.0, Sound.effectiveSeVolume());

        Sound.setMuted(false);
        assertEquals(Sound.DEFAULT_MASTER_VOLUME * Sound.DEFAULT_BGM_VOLUME, Sound.effectiveBgmVolume(), 1e-9);
    }

    @Test
    void volumesFileCanBeRead() {
        // 書き方を間違えて読めなくなっていないか（値は 0〜1 の数字であること）
        var volumes = Sound.loadVolumes();
        for (String key : volumes.stringPropertyNames()) {
            double v = Double.parseDouble(volumes.getProperty(key).strip());
            assertTrue(v >= 0 && v <= 1, "sounds/volumes.properties の " + key + " は 0〜1 にする: " + v);
        }
    }

    @Test
    void gainIsOneForUnlistedSound() {
        assertEquals(1.0, Sound.gain("no_such_sound_for_test"));
    }
}
