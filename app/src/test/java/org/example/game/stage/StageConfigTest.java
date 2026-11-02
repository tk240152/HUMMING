package org.example.game.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StageConfigTest {

    @Test
    void readsNameFlowerAndEvents() {
        StageConfig c = StageConfig.fromJson(3, """
                {
                  "name": "むずかしい",
                  "flower": { "swayWidth": 150, "_swayWidth": "説明は無視される" },
                  "events": [ { "type": "fog" } ]
                }
                """);
        assertEquals("むずかしい", c.name);
        assertEquals(150, c.flower.swayWidth);
        assertEquals(52, c.flower.hitRadius); // 書いていない項目は初期値
        assertEquals(1, c.events.size());
    }

    @Test
    void flowerTextureIsFoundInImages() {
        // 名前だけ書けば、共通の images/ の画像を指す
        String[] names = {"petunia", "anemone", "trumpetvine", "cornflower", "golden_columbine"};
        for (int n = 1; n <= names.length; n++) {
            StageConfig c = StageConfig.load(n);
            assertEquals(names[n - 1], c.flower.texture);
            assertTrue(StageConfig.class.getResource("/images/" + c.flower.texture + ".png") != null,
                    c.flower.texture + ".png がありません");
        }
        // 書いていなければ空（今までの花）で、回りの上乗せもない
        assertEquals("", StageConfig.fromJson(3, "{}").flower.texture);
        assertEquals(0, StageConfig.fromJson(3, "{}").flower.textureSpin);
        assertEquals(0.4, StageConfig.fromJson(3, "{\"flower\": {\"textureSpin\": 0.4}}").flower.textureSpin);
        // 花芯のずれは書いていなければ 0（画像の真ん中が花芯）
        assertEquals(0, StageConfig.fromJson(3, "{}").flower.textureCenterY);
        assertEquals(3.5, StageConfig.fromJson(3, "{\"flower\": {\"textureCenterY\": 3.5}}").flower.textureCenterY);
    }

    @Test
    void defaultFileCanBeReadBack() {
        StageConfig c = StageConfig.fromJson(4, "{}");
        c.flower.stemLength = 321;
        StageConfig back = StageConfig.fromJson(4, c.defaultJson());
        assertEquals(321, back.flower.stemLength);
        assertEquals("STAGE 4", back.name);
    }

    @Test
    void missingFileIsNotCreated() {
        // 無いステージを読んでも、ファイルは作らない（作ると git の管理外のファイルができて pull が止まる）
        Path file = org.example.core.ProjectFiles.resolve(StageConfig.sourcePath(999));
        StageConfig c = StageConfig.load(999);
        assertEquals(false, Files.exists(file), "ファイルが作られてしまった: " + file);
        assertEquals(0, c.events.size());          // 初期値（イベントなし）で動く
        assertEquals(52, c.flower.hitRadius);
    }

    @Test
    void realStageFilesAreValid() {
        for (int stage = 1; stage <= StageConfig.STAGE_COUNT; stage++) {
            StageConfig c = StageConfig.load(stage);
            assertTrue(!c.name.isBlank() && !c.description.isBlank(), "ステージ" + stage + " の名前・説明がありません");
            assertTrue(c.difficulty >= 1 && c.difficulty <= 5);
            assertEquals(5, c.rankTimes.length); // SS・S・A・B・C
        }
    }

    @Test
    void readsBgmBackgroundAndRank() {
        StageConfig c = StageConfig.fromJson(3, """
                { "bgm": "wind", "background": "sky", "rank": { "SS": 20, "S": 30, "C": 90 } }
                """);
        assertEquals("wind", c.bgmKey());          // 書いた名前のまま（共通の sounds/bgm/ から読む）
        assertEquals("sky", c.backgroundKey());
        assertEquals(20, c.rankTimes[0]);         // SS
        assertEquals(30, c.rankTimes[1]);         // S
        assertEquals(60, c.rankTimes[2]);         // 書いていないものは初期値
        assertEquals(90, c.rankTimes[4]);         // C
    }

    @Test
    void rankWithoutSsUsesDefaultSs() {
        // SS を足す前の書き方（S・A・B・C だけ）でも読める。SS は初期値になる
        StageConfig c = StageConfig.fromJson(3, """
                { "rank": { "S": 45, "A": 60, "B": 80, "C": 100 } }
                """);
        assertEquals(30, c.rankTimes[0]);
        assertEquals(45, c.rankTimes[1]);
    }
}
