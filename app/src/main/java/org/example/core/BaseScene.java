package org.example.core;

import javafx.scene.canvas.GraphicsContext;

/**
 * 画面（シーン）の共通インターフェース。
 * 新しい画面を作るときはこれを実装したクラスを1つ作るだけでよい。
 */
public interface BaseScene {

    /** 1ステップ（1/60秒）ごとに呼ばれる。ロジックを書く。 */
    void update();

    /** 毎フレーム呼ばれる。描画だけを書く。 */
    void render(GraphicsContext gc);

    /** P キー（開発用の画面送り）を押したときに進む次の画面。 */
    BaseScene next();

    /** 画面に入ったときに1回だけ呼ばれる。 */
    default void onEnter() {}

    /** 画面を抜けるときに1回だけ呼ばれる。 */
    default void onExit() {}

    /**
     * この画面の BGM のキー（Sound.bgm に渡す名前）。画面に入ったとき（onEnter のあと）に流れる。
     * null なら前の画面の BGM をそのまま続ける。ファイルが無いキーなら無音になる。
     */
    default String bgm() { return null; }
}
