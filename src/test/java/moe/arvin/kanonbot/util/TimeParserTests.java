package moe.arvin.kanonbot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeParserTests {

    @Test
    void parsesSecondsToMilliseconds() {
        assertEquals(16000, TimeParser.secondsToMilliseconds("16"));
        assertEquals(16500, TimeParser.secondsToMilliseconds("16.5"));
        assertEquals(1, TimeParser.secondsToMilliseconds("0.001"));
        assertEquals(1235, TimeParser.secondsToMilliseconds("1.2345"));
    }

    @Test
    void rejectsInvalidTimes() {
        assertThrows(NumberFormatException.class, () -> TimeParser.secondsToMilliseconds(""));
        assertThrows(NumberFormatException.class, () -> TimeParser.secondsToMilliseconds("NaN"));
    }
}
