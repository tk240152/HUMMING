package org.example.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.example.core.ProjectFiles;

class RecordsTest {

    @TempDir
    Path dir;

    private Path file;

    @BeforeEach
    void useTempFile() {
        file = dir.resolve(Records.FILE_NAME);
        Records.useFile(file);
    }

    @Test
    void firstClearIsNewRecord() {
        assertFalse(Records.hasBest(1));
        assertTrue(Records.submit(1, 12.34));
        assertTrue(Records.hasBest(1));
        assertEquals(12.34, Records.best(1));
    }

    @Test
    void onlyFasterTimeUpdatesBest() {
        Records.submit(1, 20.00);
        assertFalse(Records.submit(1, 25.00)); // 遅い
        assertFalse(Records.submit(1, 20.00)); // 同じ
        assertTrue(Records.submit(1, 19.99));  // 速い
        assertEquals(19.99, Records.best(1));
    }

    @Test
    void stagesAreSeparate() {
        Records.submit(1, 10.00);
        assertFalse(Records.hasBest(2));
        assertTrue(Records.submit(2, 30.00));
        assertEquals(10.00, Records.best(1));
    }

    @Test
    void bestIsKeptAfterRestart() {
        Records.submit(2, 42.50);
        Records.useFile(file); // 起動し直したのと同じ（ファイルから読み直す）
        assertEquals(42.50, Records.best(2));
    }

    @Test
    void clearAllRemovesEveryRecordAndIsSaved() {
        Records.submit(1, 10.00);
        Records.submit(3, 30.00);
        Records.clearAll();
        assertFalse(Records.hasBest(1));
        assertFalse(Records.hasBest(3));
        Records.useFile(file); // 起動し直しても消えたまま
        assertFalse(Records.hasBest(1));
        assertTrue(Records.submit(1, 50.00)); // 消したあとは、どんなタイムでも新記録
    }

    @Test
    void formatsLikeTheTimer() {
        assertEquals("0.00", Records.format(0));
        assertEquals("12.34", Records.format(12.34));
        assertEquals("9999.99", Records.format(9999.99));
    }

    @Test
    void defaultFileIsOutsideTheProject() {
        Path file = Records.defaultFile().toAbsolutePath();
        assertTrue(file.startsWith(Path.of(System.getProperty("user.home"))), "ユーザーフォルダに保存する: " + file);
        assertFalse(file.startsWith(ProjectFiles.root().toAbsolutePath()), "プロジェクトの中に保存してはいけない（git に入る）: " + file);
    }

    @Test
    void savingCreatesTheFolder() {
        Path nested = dir.resolve("new_folder").resolve(Records.FILE_NAME);
        Records.useFile(nested);
        Records.submit(1, 10.00);
        assertTrue(java.nio.file.Files.exists(nested), "フォルダが無くても作って保存する");
    }
}
