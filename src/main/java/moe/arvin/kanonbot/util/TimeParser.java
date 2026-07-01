package moe.arvin.kanonbot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TimeParser {

    private TimeParser() {
    }

    public static long secondsToMilliseconds(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new NumberFormatException("time is required");
        }

        return new BigDecimal(input.trim())
                .movePointRight(3)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
