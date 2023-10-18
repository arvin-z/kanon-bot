package moe.arvin.kanonbot.util;

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader;

public class FilterUtil {
    public static final boolean TIMESCALE_AVAILABLE = tryLoad(TimescaleNativeLibLoader::loadTimescaleLibrary);

    private static boolean tryLoad(Runnable load) {
        try {
            load.run();
            return true;
        } catch (Throwable error) {
            return false;
        }
    }
}
