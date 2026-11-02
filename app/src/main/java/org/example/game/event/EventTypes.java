package org.example.game.event;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * JSON の "type" とイベントのクラスの対応表。ステージは StageN.registerEvents() でここに登録する。
 *
 * <pre>
 * public void registerEvents(EventTypes types) {
 *     // 1. 用意されたイベントをそのまま使う（stage.json で "type": "fog" と書ける）
 *     types.use("fog", "gust");
 *
 *     // 2. 初期値だけ変えて、別の名前で使う（コードを書かずにカスタマイズ）
 *     types.register("thickFog", EventTypes.preset("fog", Map.of("density", 0.95, "color", "#888888")));
 *
 *     // 3. 動きを少し変える（用意されたイベントを継承して、必要なメソッドだけ上書き）
 *     types.register("pinkFog", p -> new FogEvent(p) {
 *         protected Color color(EventContext ctx) { return Color.PINK; }
 *     });
 *
 *     // 4. 大きく変える・新しく作る → EventTemplate.java を自分のステージのフォルダにコピーして作る
 *     types.register("enemy", EnemyEvent::new);
 * }
 * </pre>
 */
public class EventTypes {

    /** 用意されたイベントの一覧（game/event にあるもの）。use() で名前を指定して使う */
    private static final Map<String, Function<Map<String, Object>, StageEvent>> LIBRARY = new LinkedHashMap<>();

    static {
        LIBRARY.put("push", PushEvent::new);               // プレイヤーを押す
        LIBRARY.put("flower", FlowerChangeEvent::new);     // 花の設定を一時的に変える
        LIBRARY.put("gust", GustEvent::new);               // 突風（不規則な風で揺れるステージ向け）
        LIBRARY.put("fog", FogEvent::new);                 // 霧（視界不良）
    }

    /** 登録しなくてもすべてのステージで使えるイベント */
    private static final String[] COMMON = {"push", "flower"};

    private final Map<String, Function<Map<String, Object>, StageEvent>> types = new LinkedHashMap<>();

    /** すべてのステージで使えるイベント（push・flower）だけが入った対応表。 */
    public static EventTypes common() {
        EventTypes t = new EventTypes();
        t.use(COMMON);
        return t;
    }

    /** 用意されたイベントの名前の一覧。 */
    public static Set<String> library() {
        return LIBRARY.keySet();
    }

    /** 用意されたイベントを、同じ名前で使えるようにする（例: use("fog", "gust")）。 */
    public void use(String... names) {
        for (String name : names) {
            Function<Map<String, Object>, StageEvent> factory = LIBRARY.get(name);
            if (factory == null) {
                System.err.println("[Event] 用意されたイベントに " + name + " はありません（あるのは " + LIBRARY.keySet() + "）");
                continue;
            }
            register(name, factory);
        }
    }

    /**
     * 用意されたイベントの初期値を変えたものを作る。stage.json に書いた値のほうが優先される。
     * 例: preset("fog", Map.of("density", 0.95))  → stage.json に書かなくても濃い霧になる
     */
    public static Function<Map<String, Object>, StageEvent> preset(String name, Map<String, Object> defaults) {
        Function<Map<String, Object>, StageEvent> factory = LIBRARY.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("用意されたイベントに " + name + " はありません（あるのは " + LIBRARY.keySet() + "）");
        }
        Map<String, Object> normalized = normalize(defaults);
        return params -> {
            Map<String, Object> merged = new HashMap<>(normalized);
            merged.putAll(params);
            return factory.apply(merged);
        };
    }

    /** type の名前でイベントを登録する。同じ名前があれば上書きする（ステージ独自のものを優先）。 */
    public void register(String type, Function<Map<String, Object>, StageEvent> factory) {
        types.put(type, factory);
    }

    Function<Map<String, Object>, StageEvent> get(String type) {
        return types.get(type);
    }

    public Set<String> names() {
        return types.keySet();
    }

    /** Map.of(…, 0.95) や Map.of(…, 3) の数値を、JSON と同じ Double にそろえる。 */
    private static Map<String, Object> normalize(Map<String, Object> values) {
        Map<String, Object> result = new HashMap<>();
        values.forEach((k, v) -> result.put(k, v instanceof Number n ? (Object) n.doubleValue() : v));
        return result;
    }
}
