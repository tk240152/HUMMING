package org.example.game.flower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class FlowerTest {

    @Test
    void configReadsNumbersFromJson() {
        FlowerConfig c = new FlowerConfig();
        c.apply("{ \"stemLength\": 300, \"stemWidth\": 5.5, \"swayWidth\": 60, \"unknown\": 1 }");
        assertEquals(300, c.stemLength);
        assertEquals(5.5, c.stemWidth);
        assertEquals(60, c.swayWidth);
        assertEquals(130, c.flowerSize); // 書いていない項目は初期値のまま
    }

    @Test
    void configJsonRoundTrips() {
        FlowerConfig a = new FlowerConfig();
        a.stemLength = 123;
        FlowerConfig b = new FlowerConfig();
        b.apply(a.toJson());
        assertEquals(123, b.stemLength);
    }

    @Test
    void stemLengthStaysTheSameWhileSwaying() {
        FlowerConfig c = new FlowerConfig();
        Flower flower = new Flower(c);
        Wind wind = new Wind(new Random(1));

        for (int step = 0; step < 60 * 30; step++) {
            wind.update(c.swaySpeed);
            flower.update(wind.value());
            assertEquals(c.stemLength, stemLength(flower), 1e-6);
        }
    }

    @Test
    void swayStaysWithinSwayWidth() {
        FlowerConfig c = new FlowerConfig();
        Flower flower = new Flower(c);
        Wind wind = new Wind(new Random(2));

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        for (int step = 0; step < 60 * 60; step++) {
            wind.update(c.swaySpeed);
            flower.update(wind.value());
            minX = Math.min(minX, flower.x);
            maxX = Math.max(maxX, flower.x);
        }
        // 左右に十分揺れていて、幅は swayWidth を大きく超えない（ばねの行き過ぎの分だけ少し超えてよい）
        assertTrue(maxX - minX > c.swayWidth * 0.5, "揺れ幅 " + (maxX - minX));
        assertTrue(maxX - minX < c.swayWidth * 1.6, "揺れ幅 " + (maxX - minX));
    }

    @Test
    void windIsIrregular() {
        Wind wind = new Wind(new Random(3));
        double[] first = new double[300];
        for (int i = 0; i < first.length; i++) {
            wind.update(1.0);
            first[i] = wind.value();
        }
        // 5秒後の300ステップは、最初の300ステップと同じ動きにならない
        double diff = 0;
        for (int i = 0; i < first.length; i++) {
            wind.update(1.0);
            diff += Math.abs(wind.value() - first[i]);
            assertTrue(Math.abs(wind.value()) <= 1.0);
        }
        assertTrue(diff > 1.0);
    }

    private static double stemLength(Flower f) {
        double len = 0;
        for (int i = 0; i < Flower.SEGMENTS; i++) {
            len += Math.hypot(f.stemX[i + 1] - f.stemX[i], f.stemY[i + 1] - f.stemY[i]);
        }
        return len;
    }
}
