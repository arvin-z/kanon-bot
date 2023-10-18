package moe.arvin.kanonbot.music.filter;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

public interface Config {

    double MINIMUM_FP_DIFF = 0.1;

    String name();

    boolean enabled();

    AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output);

    static boolean isSet(double value, double defaultValue) {
        return Math.abs(value - defaultValue) >= MINIMUM_FP_DIFF;
    }

}
