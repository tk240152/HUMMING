package org.example.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameStateTest {

    private static final double[] TIMES = {30, 40, 60, 80, 100}; // SS・S・A・B・C

    @Test
    void rankForEachRange() {
        assertEquals("SS", GameState.rankFor(29.9, TIMES));
        assertEquals("S", GameState.rankFor(30, TIMES));  // ちょうどの秒数は1つ下のランク
        assertEquals("S", GameState.rankFor(39.9, TIMES));
        assertEquals("A", GameState.rankFor(59.9, TIMES));
        assertEquals("B", GameState.rankFor(79.9, TIMES));
        assertEquals("C", GameState.rankFor(99.9, TIMES));
        assertEquals("D", GameState.rankFor(100, TIMES));
    }

    @Test
    void rankUsesThisStageTimes() {
        GameState state = new GameState();
        state.rankTimes = new double[] {20, 40, 60, 80, 100};
        state.clearTime = 25;
        assertEquals("S", state.getRank()); // いつもの基準なら SS だが、このステージは 20 秒より速くないと SS ではない
    }
}
