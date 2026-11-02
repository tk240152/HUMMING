package org.example.game.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameTimerTest {

    @Test
    void formatsWithTwoDecimalsAndNoLeadingZeros() {
        assertEquals("0.00", GameTimer.format(0));
        assertEquals("0.05", GameTimer.format(5));
        assertEquals("12.34", GameTimer.format(1234));
        assertEquals("123.40", GameTimer.format(12340));
        assertEquals("9999.99", GameTimer.format(999_999));
    }

    @Test
    void neverShowsMoreThanFourDigitSeconds() {
        assertEquals("9999.99", GameTimer.format(5_000_000));
    }

    @Test
    void sixtyStepsIsExactlyOneSecond() {
        GameTimer timer = new GameTimer();
        for (int i = 0; i < 60; i++) timer.update();
        assertEquals("1.00", timer.text());
    }

    @Test
    void stopsAtMaximum() {
        GameTimer timer = new GameTimer();
        for (long i = 0; i < 60L * 10_001; i++) timer.update();
        assertEquals("9999.99", timer.text());
    }

    @Test
    void doesNotCountAfterStop() {
        GameTimer timer = new GameTimer();
        for (int i = 0; i < 30; i++) timer.update();
        timer.stop();
        for (int i = 0; i < 30; i++) timer.update();
        assertEquals("0.50", timer.text());
    }
}
