package org.example.ui;

import org.example.ui.stageselect.StageInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StageInfoTest {

    @Test
    void readsInfoFromStageJson() {
        StageInfo info = StageInfo.load(1);
        assertEquals("そよ風の平原", info.name());
        assertEquals(1, info.difficulty());
        assertEquals("stage_preview_1", info.preview()); // ステージのフォルダに画像が無いので共通の名前のまま
    }
}
