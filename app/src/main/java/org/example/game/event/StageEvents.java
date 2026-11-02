package org.example.game.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javafx.scene.canvas.GraphicsContext;

import org.example.core.Game;
import org.example.game.util.Json;

/**
 * ステージのイベントをまとめて動かす。
 *
 * 新しい種類のイベントを足すときは:
 * 1. StageEvent を継承したクラスを作る
 * 2. 全ステージで使うなら EventTypes.common() に、そのステージだけなら StageN.registerEvents() で登録する
 */
public class StageEvents {

    private final List<StageEvent> events = new ArrayList<>();

    /** ステージ設定の events（JSON の配列）から、共通のイベントだけでイベントを作る。 */
    public static StageEvents create(List<Object> definitions) {
        return create(definitions, EventTypes.common());
    }

    /** 使えるイベントの対応表（ステージ独自のものを含む）を指定して作る。知らない種類は警告を出して飛ばす。 */
    @SuppressWarnings("unchecked")
    public static StageEvents create(List<Object> definitions, EventTypes types) {
        StageEvents result = new StageEvents();
        for (Object def : definitions) {
            if (!(def instanceof Map<?, ?> m))
                continue;
            Map<String, Object> params = (Map<String, Object>) m;
            String type = Json.string(params, "type", null);
            Function<Map<String, Object>, StageEvent> factory = type == null ? null : types.get(type);
            if (factory == null) {
                System.err.println("[Event] 知らない type です: " + type + "（使えるのは " + types.names() + "）");
                continue;
            }
            result.events.add(factory.apply(params));
        }
        return result;
    }

    /** イベントを直接足す（テスト用）。 */
    void add(StageEvent event) {
        events.add(event);
    }

    /** 毎ステップ呼ぶ。time はステージ開始からの秒数。 */
    public void update(EventContext ctx, double time) {
        ctx.beginStep(time);
        for (StageEvent e : events)
            e.step(ctx);
    }

    /** 続いているイベントを、花・プレイヤーより手前に描く（霧など）。 */
    public void render(GraphicsContext gc, EventContext ctx) {
        for (StageEvent e : events) {
            if (e.isActive())
                e.render(gc, ctx);
        }
    }

    /** 続いているイベントを、背景のすぐ上（花・プレイヤーより奥）に描く（火山の山など）。 */
    public void renderBack(GraphicsContext gc, EventContext ctx) {
        for (StageEvent e : events) {
            if (e.isActive())
                e.renderBack(gc, ctx);
        }
    }

    /** 実行中のイベントによる描画位置のずれをまとめて適用する。 */
    public void applyScreenOffset(GraphicsContext gc) {
        double offsetX = 0.0;
        double offsetY = 0.0;

        for (StageEvent event : events) {
            if (event.isActive()) {
                offsetX += event.screenOffsetX();
                offsetY += event.screenOffsetY();
            }
        }

        gc.translate(offsetX, offsetY);
        if (offsetX == 0.0 && offsetY == 0.0) return;

        // ずらした分だけ画面の端があかないよう、ずれに合わせて真ん中から少し拡大する（24px ずれて約 1.025 倍）
        double scale = Math.max(1 + 2 * Math.abs(offsetX) / Game.WIDTH, 1 + 2 * Math.abs(offsetY) / Game.HEIGHT);
        double cx = Game.WIDTH / 2.0 - offsetX, cy = Game.HEIGHT / 2.0 - offsetY;
        gc.translate(cx, cy);
        gc.scale(scale, scale);
        gc.translate(-cx, -cy);
    }

    /** ステージを抜けるときに呼ぶ。続いているイベントを終わらせて、変えたものを元に戻す。 */
    public void endAll(EventContext ctx) {
        for (StageEvent e : events)
            e.finish(ctx);
    }

    /** 実行中のイベントに一時停止を通知する。 */
    public void pause(EventContext ctx) {
        for (StageEvent event : events) {
            if (event.isActive()) {
                event.onPause(ctx);
            }
        }
    }

    /** 実行中のイベントに再開を通知する。 */
    public void resume(EventContext ctx) {
        for (StageEvent event : events) {
            if (event.isActive()) {
                event.onResume(ctx);
            }
        }
    }

    public int size() {
        return events.size();
    }

    /** 今続いているイベントの数。 */
    public long activeCount() {
        return events.stream().filter(StageEvent::isActive).count();
    }
}
