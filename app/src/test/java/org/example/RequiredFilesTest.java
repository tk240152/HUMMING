package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.game.util.Json;
import org.example.game.stage.StageConfig;
import org.junit.jupiter.api.Test;
import org.example.core.ProjectFiles;

/**
 * ゲームに必要なファイルがそろっているかを確かめる。
 * git に上げ忘れた・.gitignore で無視されたファイルがあると、ここで失敗する
 * （作った本人の PC でだけ動く、を防ぐ）。
 */
class RequiredFilesTest {

    /** resources に必ず必要なファイル */
    private static final String[] RESOURCES = {
            "/stage_layout/stage-detail-layout.properties",
            "/styles/stage-select.css",
            "/styles/stage-detail.css",
    };

    /** HUMMING 直下に必ず必要なファイル（設定ファイルなど） */
    private static final String[] PROJECT_FILES = {
            "effect_config.properties",
    };

    @Test
    void projectFilesExist() {
        for (String name : PROJECT_FILES) {
            assertTrue(Files.exists(ProjectFiles.resolve(name)),
                    name + " がありません。git に上がっているか（.gitignore で無視されていないか）確認してください");
        }
    }

    @Test
    void projectRootIsHumming() {
        Path root = ProjectFiles.root();
        assertTrue(Files.exists(root.resolve("settings.gradle")), "HUMMING のフォルダではない: " + root);
        assertTrue(Files.isDirectory(root.resolve("app")), "HUMMING のフォルダではない: " + root);
    }

    @Test
    void resourcesExist() {
        for (String name : RESOURCES) {
            assertNotNull(RequiredFilesTest.class.getResource(name),
                    name + " がありません。git に上がっているか（.gitignore で無視されていないか）確認してください");
        }
    }

    @Test
    void everyStageHasAConfigFile() throws Exception {
        int stages = StageConfig.STAGE_COUNT;

        for (int n = 1; n <= stages; n++) {
            Path file = ProjectFiles.resolve("app/src/main/resources/stages/stage" + n + "/stage.json");
            assertTrue(Files.exists(file), "stages/stage" + n + "/stage.json がありません（ステージは " + stages
                    + " 個）。ほかのステージのフォルダをコピーして作り、git に上げてください");

            // JSON として読めて、花の設定とイベントの欄があること
            var json = Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
            assertNotNull(Json.object(json, "flower"), "stages/stage" + n + "/stage.json に flower がありません");
            for (String key : new String[] {"name", "description", "difficulty", "preview"}) {
                assertNotNull(json.get(key), "stages/stage" + n + "/stage.json に " + key + " がありません");
            }
        }
    }
}
