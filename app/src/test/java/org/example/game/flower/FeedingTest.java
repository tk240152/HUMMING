package org.example.game.flower;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.geometry.Point2D;

import org.example.state.GameState;
import org.example.state.GameState.Status;
import org.junit.jupiter.api.Test;

class FeedingTest {

    private final FlowerConfig config = new FlowerConfig();
    private final Flower flower = new Flower(config);
    private final Feeding feeding = new Feeding();

    @Test
    void nectarIncreasesOnlyWhileBeakTouchesFlower() {
        GameState state = new GameState();
        Point2D onFlower = new Point2D(flower.x, flower.y);
        Point2D away = new Point2D(flower.x - 400, flower.y);

        feeding.update(away, flower, 1.0, 350, state);
        assertEquals(0, state.nectar);
        assertEquals(Status.FLYING, state.status);

        feeding.update(onFlower, flower, 1.0, 350, state);
        feeding.update(onFlower, flower, 1.0, 350, state);
        assertEquals(2, state.nectar);
        assertEquals(Status.FEEDING, state.status);

        feeding.update(away, flower, 1.0, 350, state);
        assertEquals(2, state.nectar); // 離れたら増えない
    }

    @Test
    void clearsWhenNectarReachesClearValue() {
        GameState state = new GameState();
        state.maxNectar = 5;
        Point2D onFlower = new Point2D(flower.x, flower.y);

        for (int i = 0; i < 4; i++) feeding.update(onFlower, flower, 1.0, 350, state);
        assertEquals(Status.FEEDING, state.status);

        feeding.update(onFlower, flower, 1.0, 350, state);
        assertEquals(Status.CLEAR, state.status);
        assertEquals(5, state.nectar);
    }
}
