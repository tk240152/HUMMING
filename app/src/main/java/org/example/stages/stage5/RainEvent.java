package org.example.stages.stage5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.example.core.Game;
import org.example.game.event.EventContext;
import org.example.game.event.StageEvent;
import org.example.media.Sound;
import org.example.player.Player;

/**
 * 常に雨が降り、画面全体を暗く演出するとともに、
 * プレイヤーの移動能力（加速度・最高速度）にデバフをかけ続けるイベント。
 */
public class RainEvent extends StageEvent {

    // 雨粒のデータ構造
    private static class RainDrop {
        double x, y;
        double speed;
        double length;

        RainDrop(double x, double y, double speed, double length) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
        }
    }

    private final List<RainDrop> drops = new ArrayList<>();
    private final Random random = new Random();

    // stage.json で調整可能なパラメータ
    private final int dropCount = (int) number("dropCount", 140);          // 雨粒の数（少し増量）
    private final double slowdownFactor = number("slowdownFactor", 0.7);    // 移動性能の倍率 (0.7 = 30%減速)
    
    // 暗さの調整（0.0 = 透明, 1.0 = 完全な黒）。0.25〜0.35 あたりが雰囲気に合わせておすすめ
    private final double darkOpacity = number("darkOpacity", 0.3);

    // 雨の音（降っている間くり返し鳴らす。ポーズで止まる）。se が "" なら鳴らさない
    private final String se = string("se", "rain");
    private final double seVolume = number("seVolume", 0.5); // 雨の音の大きさ（0〜1。効果音の音量に掛ける）

    public RainEvent(Map<String, Object> params) {
        super(params);
    }

    @Override
    protected void onStart(EventContext ctx) {
        drops.clear();
        for (int i = 0; i < dropCount; i++) {
            drops.add(new RainDrop(
                random.nextDouble() * Game.WIDTH,
                random.nextDouble() * Game.HEIGHT,
                1080 + random.nextDouble() * 648, // 落下の速さ
                30 + random.nextDouble() * 22    // 雨粒の長さ
            ));
        }
        if (!se.isEmpty()) Sound.loopSe(se, seVolume);
    }

    @Override
    protected void onUpdate(EventContext ctx) {
        double dt = Game.STEP;

        // 1. 雨粒の位置更新
        for (RainDrop drop : drops) {
            drop.y += drop.speed * dt;
            drop.x -= drop.speed * 0.25 * dt; // 風に合わせて少し斜めに降らせる

            if (drop.y > Game.HEIGHT) {
                drop.y = -drop.length;
                drop.x = random.nextDouble() * (Game.WIDTH + 240);
            }
        }

        // 2. プレイヤーへのデバフ適用（既存の setGainScale を使用）
        if (ctx.player instanceof Player) {
            Player player = (Player) ctx.player;
            player.setGainScale(slowdownFactor, slowdownFactor);
        }
    }

    @Override
    protected void onEnd(EventContext ctx) {
        drops.clear();
        if (!se.isEmpty()) Sound.stopLongSe(se);
        // イベント終了時に標準値（1.0）に戻す
        if (ctx.player instanceof Player) {
            Player player = (Player) ctx.player;
            player.setGainScale(1.0, 1.0);
        }
    }

    @Override
    public void render(GraphicsContext gc, EventContext ctx) {
        gc.save();

        // ==========================================
        // 1. 画面全体の暗転（雨模様の暗いフィルター）
        // ==========================================
        // 濃いネイビー・グレーで画面全体を覆う
        gc.setFill(Color.rgb(15, 20, 35, darkOpacity));
        gc.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        // ==========================================
        // 2. 雨粒の描画（斜めの光るライン）
        // ==========================================
        gc.setStroke(Color.rgb(200, 225, 255, 0.6));
        gc.setLineWidth(3.9);

        for (RainDrop drop : drops) {
            gc.strokeLine(drop.x, drop.y, drop.x - drop.length * 0.25, drop.y + drop.length);
        }

        gc.restore();
    }
}