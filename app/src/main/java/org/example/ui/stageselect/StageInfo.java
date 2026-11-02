package org.example.ui.stageselect;

import org.example.game.stage.StageConfig;

/**
 * ステージごとの表示内容。
 * 各ステージの stages/stageN/stage.json（name・description・difficulty・preview・rank）から読む。
 * rankTimes はランクの基準（秒。SS・S・A・B・C の順）。ベストタイムのランクの絵を、リザルトと同じ基準で決めるのに使う。
 */
public record StageInfo(
        String name,
        String description,
        int difficulty,
        String preview,
        double[] rankTimes) {

    /** 指定されたステージの情報を取得する。 */
    public static StageInfo load(int stageNumber) {
        StageConfig stage = StageConfig.load(stageNumber);
        return new StageInfo(
                stage.name,
                stage.description,
                stage.difficulty,
                stage.previewKey(),
                stage.rankTimes.clone());
    }
}
