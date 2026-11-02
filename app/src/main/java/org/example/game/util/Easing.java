package org.example.game.util;

/**
 * イージング（動きの緩急）の関数。t は 0.0〜1.0 の進み具合で、0.0〜1.0 の値を返す。
 * 参考: https://easings.net/ja
 */
public final class Easing {

    private Easing() {}

    /** easeInOutSine。ゆっくり動き出して、ゆっくり止まる。https://easings.net/ja#easeInOutSine */
    public static double inOutSine(double t) {
        t = Math.clamp(t, 0.0, 1.0);
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    /**
     * easeOutBounce。最後にボールが床で跳ねるように、目標で何度か弾んでから止まる。
     * https://easings.net/ja#easeOutBounce の式と同じ。
     */
    public static double outBounce(double t) {
        t = Math.clamp(t, 0.0, 1.0);
        final double n1 = 7.5625;
        final double d1 = 2.75;

        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            t -= 1.5 / d1;
            return n1 * t * t + 0.75;
        } else if (t < 2.5 / d1) {
            t -= 2.25 / d1;
            return n1 * t * t + 0.9375;
        } else {
            t -= 2.625 / d1;
            return n1 * t * t + 0.984375;
        }
    }
}
