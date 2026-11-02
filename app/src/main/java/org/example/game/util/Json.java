package org.example.game.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小さな JSON 読み込み（ライブラリを増やさないための自作）。
 * 結果は次の型になる:
 * オブジェクト → Map&lt;String, Object&gt;、配列 → List&lt;Object&gt;、文字列 → String、
 * 数値 → Double、true/false → Boolean、null → null。
 */
public final class Json {

    private final String s;
    private int pos;

    private Json(String s) {
        this.s = s;
    }

    /** JSON を読み込む。書き方が間違っていれば IllegalArgumentException（何行目かを含む）。 */
    public static Object parse(String text) {
        Json p = new Json(text);
        p.skipSpace();
        Object v = p.value();
        p.skipSpace();
        if (p.pos < p.s.length()) throw p.error("JSON の終わりのあとに余分な文字があります");
        return v;
    }

    /** オブジェクトであることを期待して読む。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) throw new IllegalArgumentException("JSON の一番外側は { } にしてください");
        return (Map<String, Object>) v;
    }

    // ---- 値を取り出すための小さな道具 ----

    /** map の key が数値ならその値、無い・数値でないなら def。 */
    public static double number(Map<String, Object> map, String key, double def) {
        return map != null && map.get(key) instanceof Double d ? d : def;
    }

    /** map の key が文字列ならその値、無い・文字列でないなら def。 */
    public static String string(Map<String, Object> map, String key, String def) {
        return map != null && map.get(key) instanceof String str ? str : def;
    }

    /** map の key がオブジェクトならその中身、無ければ null。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Map<String, Object> map, String key) {
        return map != null && map.get(key) instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    /** map の key が配列ならその中身、無ければ空のリスト。 */
    @SuppressWarnings("unchecked")
    public static List<Object> array(Map<String, Object> map, String key) {
        return map != null && map.get(key) instanceof List<?> l ? (List<Object>) l : List.of();
    }

    // ---- 読み込み ----

    private Object value() {
        if (pos >= s.length()) throw error("値がありません");
        char c = s.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (c == '-' || Character.isDigit(c)) yield number();
                throw error("読めない文字 '" + c + "' があります");
            }
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // {
        skipSpace();
        if (peek('}')) { pos++; return map; }
        while (true) {
            skipSpace();
            if (!peek('"')) throw error("項目名は \" \" で囲んでください");
            String key = string();
            skipSpace();
            expect(':');
            skipSpace();
            map.put(key, value());
            skipSpace();
            if (peek(',')) { pos++; continue; }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++; // [
        skipSpace();
        if (peek(']')) { pos++; return list; }
        while (true) {
            skipSpace();
            list.add(value());
            skipSpace();
            if (peek(',')) { pos++; continue; }
            expect(']');
            return list;
        }
    }

    private String string() {
        pos++; // "
        StringBuilder sb = new StringBuilder();
        while (pos < s.length()) {
            char c = s.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            if (pos >= s.length()) break;
            char e = s.charAt(pos++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (pos + 4 > s.length()) throw error("\\u のあとに4桁が必要です");
                    sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> sb.append(e); // \" \\ \/
            }
        }
        throw error("文字列が \" で閉じられていません");
    }

    private Double number() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
        try {
            return Double.parseDouble(s.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error("数値の書き方が間違っています: " + s.substring(start, pos));
        }
    }

    private Object literal(String word, Object v) {
        if (!s.startsWith(word, pos)) throw error("読めない値があります");
        pos += word.length();
        return v;
    }

    private void skipSpace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    private boolean peek(char c) {
        return pos < s.length() && s.charAt(pos) == c;
    }

    private void expect(char c) {
        if (!peek(c)) throw error("'" + c + "' が必要です（カンマの付け忘れ・付けすぎに注意）");
        pos++;
    }

    private IllegalArgumentException error(String message) {
        int line = 1;
        for (int i = 0; i < Math.min(pos, s.length()); i++) if (s.charAt(i) == '\n') line++;
        return new IllegalArgumentException(line + "行目: " + message);
    }
}
