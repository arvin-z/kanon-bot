package moe.arvin.kanonbot.music.filter;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import moe.arvin.kanonbot.util.FilterUtil;

public class TimescaleConfig implements Config {
    private double speed = 1.0;
    private double pitch = 1.0;

    public double speed() {
        return speed;
    }

    public void setSpeed(double speed) {
        if (speed <= 0 || speed > 2.0) {
            throw new IllegalArgumentException("speed must be between 0.1 and 2.0");
        }
        this.speed = speed;
    }

    public double pitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        if (pitch <= 0 || pitch > 2.0) {
            throw new IllegalArgumentException("pitch must be between 0.1 and 2.0");
        }
        this.pitch = pitch;
    }

    @Override
    public String name() {
        return "timescale";
    }

    @Override
    public boolean enabled() {
        return FilterUtil.TIMESCALE_AVAILABLE &&
                (Config.isSet(speed, 1.0) || Config.isSet(pitch, 1.0));
    }

    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate)
                .setSpeed(speed)
                .setPitch(pitch);
    }

}
