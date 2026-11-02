package org.example.ui.title;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.media.Assets;

/** タイトル画面の画像配置と、選択枠を描く。ステージセレクトへのワイプの進み具合も決める。 */
public final class TitleView {

        private static final String FOLDER = ""; // 画像は共通の images/ にある
        /** 背景（ステージ3と同じ草原の画像を使う） */
        private static final String BACKGROUND = "bg_grassland";
        private static final String[] BUTTONS = {
                        "Tex_Title_Start", "Tex_Title_HowTo", "Tex_Title_Option", "Tex_Title_End"
        };

        // 画像の最終位置と表示サイズ。登場中はここに移動量を加える。
        private static final double TITLE_X = 30, TITLE_Y = 40;
        /** ロゴの横幅。高さは画像の縦横比で決まる（画像を差し替えても伸び縮みしない） */
        private static final double TITLE_WIDTH = 960;
        /** ロゴの画像が無いときに代わりに描く四角の高さ */
        private static final double TITLE_FALLBACK_HEIGHT = 367;
        private static final double BIRD_X = 1080, BIRD_Y = 184, BIRD_SIZE = 648;
        /** ボタンの幅。これだけ変えれば、高さは画像（512x130）の比で決まり、横の真ん中もそろったまま */
        private static final double BUTTON_WIDTH = 400;
        private static final double BUTTON_HEIGHT = BUTTON_WIDTH * 130 / 512;
        /** ボタンの横の真ん中（ロゴの真ん中にそろえる）と、1つ目のボタンの上端 */
        private static final double BUTTON_CENTER_X = 510, BUTTON_Y = 588;
        private static final double BUTTON_X = BUTTON_CENTER_X - BUTTON_WIDTH / 2;
        /** ボタン4つが画面の下に収まる間隔 */
        private static final double BUTTON_SPACING = 118;
        private static final double FRAME_PADDING = 10;
        /** 選択枠の角の丸み（ボタンの画像の角に合わせる） */
        private static final double FRAME_ARC = 70;
        /** 選択枠の色（ポーズメニューのカーソルと同じ赤） */
        private static final Color FRAME_COLOR = Color.web("#ff0000");

        public void render(GraphicsContext gc, TitleEntrance entrance,
                        TitleBird bird, int selectedIndex) {
                Assets.draw(gc, BACKGROUND,
                                0, 0, Game.WIDTH, Game.HEIGHT, Color.MIDNIGHTBLUE);
                Image logo = Assets.image(FOLDER + "Tex_Title_Logo");
                double logoHeight = logo == null ? TITLE_FALLBACK_HEIGHT
                                : TITLE_WIDTH * logo.getHeight() / logo.getWidth();
                Assets.draw(gc, FOLDER + "Tex_Title_Logo",
                                TITLE_X, TITLE_Y + entrance.titleOffsetY(),
                                TITLE_WIDTH, logoHeight, Color.SALMON);
                bird.render(gc, BIRD_X, BIRD_Y + entrance.birdOffsetY(), BIRD_SIZE);

                for (int i = 0; i < BUTTONS.length; i++) {
                        Assets.draw(gc, FOLDER + BUTTONS[i],
                                        BUTTON_X + entrance.buttonOffsetX(i),
                                        BUTTON_Y + i * BUTTON_SPACING,
                                        BUTTON_WIDTH, BUTTON_HEIGHT, Color.ORANGE);
                }

                gc.save();
                gc.setStroke(FRAME_COLOR);
                gc.setLineWidth(6.5);
                gc.strokeRoundRect(
                                BUTTON_X - FRAME_PADDING + entrance.buttonOffsetX(selectedIndex),
                                BUTTON_Y - FRAME_PADDING + selectedIndex * BUTTON_SPACING,
                                BUTTON_WIDTH + FRAME_PADDING * 2,
                                BUTTON_HEIGHT + FRAME_PADDING * 2, FRAME_ARC, FRAME_ARC);
                gc.restore();
        }

        /** ワイプが鳥を追いかける速さ（鳥の飛ぶ距離 2500 より大きいので、途中で鳥に追いつく） */
        private static final double WIPE_CHASE_DISTANCE = 4500;
        /** 鳥の画像の中で、尾の先あたりの横の位置（画像の幅に対する割合）。追いついたワイプの境目をここにくっつける */
        private static final double BIRD_TAIL_X = 0.05;

        /**
         * ステージセレクトへのワイプの進み具合（0〜1）。
         * 鳥が左にバックしている間は待機し、右へダッシュし始めたら画面左端からワイプをスタートして鳥を追いかける。
         * ワイプは鳥より速いので途中で鳥の尾に追いつき、そこからは尾にくっついて、鳥に引っ張られるように進む。
         */
        public double wipeProgress(TitleBird bird) {
                // 右へのダッシュが始まるまでは、ワイプを左端（0.0）で待機させる
                double p = bird.flightProgress();
                if (p == 0.0) {
                        return 0.0;
                }

                // 左端から、鳥と同じように加速しながら（ただし鳥より速く）追いかける
                double chase = WIPE_CHASE_DISTANCE * p * p;
                // 鳥の尾の位置。追いついたら、境目はここより先へは行かない（鳥に引っ張られる）
                double tail = BIRD_X + bird.currentOffsetX() + BIRD_SIZE * BIRD_TAIL_X;
                double edge = Math.min(chase, tail);

                return Math.clamp(edge / Game.WIDTH, 0.0, 1.0);
        }
}