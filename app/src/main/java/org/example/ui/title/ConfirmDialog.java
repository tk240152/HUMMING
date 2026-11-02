package org.example.ui.title;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import org.example.core.Game;
import org.example.core.Input;
import org.example.media.Assets;
import org.example.media.Sound;

/**
 * 「はい／いいえ」の確認（タイトルの上に重ねて出す）。終了の確認と、ベストスコアのリセットの確認で使う。
 * 
 * <pre>
 * 操作: A/D（←/→）で「はい」「いいえ」を選ぶ ／ ENTER で決定 ／ ESC は「いいえ」と同じ
 * </pre>
 * 
 * 開いたときは、うっかり「はい」にならないよう「いいえ」を選んだ状態から始める。
 * パネルの画像（panelKey）があればそれを描き、無ければ白いパネルに message の文字を描く。
 */
public final class ConfirmDialog {

    public enum Result {
        NONE, YES, NO
    }

    private static final String FOLDER = ""; // 画像は共通の images/ にある

    // パネルは画面の真ん中。大きさは画像そのままの大きさ（1920x1080 の画面の px）
    private static final double PANEL_W = 1140, PANEL_H = 480;
    private static final double BUTTON_W = 280, BUTTON_H = 157;
    /** ボタンの中心の、画面の真ん中からのずれ（「はい」は左へ、「いいえ」は右へ） */
    private static final double BUTTON_DX = 250, BUTTON_DY = 80;
    /** 選んでいるボタンの赤い枠（ボタンとのすき間と線の太さ） */
    private static final double FRAME_PADDING = 8, FRAME_WIDTH = 5;
    private static final Color FRAME_COLOR = Color.web("#e8202a");
    /** パネルの画像が無いときに描く文字の、画面の真ん中からの高さのずれと大きさ（画像の文字の位置に合わせた） */
    private static final double MESSAGE_DY = -102, MESSAGE_SIZE = 58;

    private static final int YES = 0, NO = 1;

    private final String panelKey;
    private final String message;
    private boolean open;
    private int selected = NO;

    /**
     * @param panelKey パネルの画像（images/ のファイル名）。null なら画像を使わない
     * @param message  パネルの画像が無いときに描く文字
     */
    public ConfirmDialog(String panelKey, String message) {
        this.panelKey = panelKey;
        this.message = message;
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        selected = NO;
    }

    public void close() {
        open = false;
    }

    /** 1ステップ分の操作を受け付ける。決まったら閉じて、その結果を返す。 */
    public Result update() {
        if (!open)
            return Result.NONE;

        if (Input.isPressed(KeyCode.ESCAPE)) {
            Sound.se("SE_Cancel");
            close();
            return Result.NO;
        }
        if ((Input.isPressed(KeyCode.A) || Input.isPressed(KeyCode.LEFT)) && selected != YES) {
            selected = YES;
            Sound.se("SE_Cursor");
        }
        if ((Input.isPressed(KeyCode.D) || Input.isPressed(KeyCode.RIGHT)) && selected != NO) {
            selected = NO;
            Sound.se("SE_Cursor");
        }
        if (Input.isPressed(KeyCode.ENTER)) {
            close();
            if (selected == YES) {
                Sound.se("SE_Decide");
                return Result.YES;
            }
            Sound.se("SE_Cancel");
            return Result.NO;
        }
        return Result.NONE;
    }

    public void render(GraphicsContext gc) {
        if (!open)
            return;

        double cx = Game.WIDTH / 2.0;
        double cy = Game.HEIGHT / 2.0;
        double left = cx - PANEL_W / 2, top = cy - PANEL_H / 2;
        if (panelKey != null && Assets.image(FOLDER + panelKey) != null) {
            // 透明な余白を除き、文字を含む素材をそのまま描画する
            gc.drawImage(
                    Assets.image(FOLDER + panelKey),
                    54, 293, 972, 409,
                    left, top, PANEL_W, PANEL_H);
        } else {
            // 素材が見つからない場合だけ、代替パネルを描画する
            drawPlainPanel(gc, left, top, cx, cy);
        }
        drawButton(gc, "UI_EndChack_Yes", cx - BUTTON_DX, cy + BUTTON_DY, selected == YES);
        drawButton(gc, "UI_EndChack_No", cx + BUTTON_DX, cy + BUTTON_DY, selected == NO);
    }

    /** 画像の無いパネル。終了の確認の画像に似せて、白い四角に灰色のふちと黒い文字。 */
    private void drawPlainPanel(GraphicsContext gc, double left, double top, double cx, double cy) {
        gc.save();
        gc.setFill(Color.WHITE);
        gc.fillRect(left, top, PANEL_W, PANEL_H);
        gc.setStroke(Color.web("#888888"));
        gc.setLineWidth(3);
        gc.strokeRect(left, top, PANEL_W, PANEL_H);
        gc.setFill(Color.BLACK);
        gc.setFont(Assets.font("main", MESSAGE_SIZE));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(message, cx, cy + MESSAGE_DY);
        gc.restore();
    }

    /** (x, y) を中心にボタンを描く。選んでいれば赤い枠で囲む。 */
    private void drawButton(GraphicsContext gc, String key, double x, double y, boolean selected) {
        double left = x - BUTTON_W / 2;
        double top = y - BUTTON_H / 2;
        var buttonImage = Assets.image(FOLDER + key);

        if (buttonImage != null && !buttonImage.isError()) {
            gc.drawImage(
                    buttonImage,
                    382, 457, 333, 187,
                    left, top, BUTTON_W, BUTTON_H);
        } else {
            Assets.draw(gc, FOLDER + key,
                    left, top, BUTTON_W, BUTTON_H, Color.LIGHTGRAY);
        }
        if (!selected)
            return;

        gc.save();
        gc.setStroke(FRAME_COLOR);
        gc.setLineWidth(FRAME_WIDTH);
        // 切り出した画像から画面への拡大率
        double scaleX = BUTTON_W / 333.0;
        double scaleY = BUTTON_H / 187.0;

        // 影を除いたボタン本体に選択枠を合わせる
        double frameX = left + 9 * scaleX - FRAME_PADDING;
        double frameY = top + 7 * scaleY - FRAME_PADDING;
        double frameW = 298 * scaleX + FRAME_PADDING * 2;
        double frameH = 152 * scaleY + FRAME_PADDING * 2;

        // ボタンの形に合わせて、選択枠の角を丸くする
        gc.strokeRoundRect(frameX, frameY, frameW, frameH, 100, 100);
        gc.restore();
    }
}
