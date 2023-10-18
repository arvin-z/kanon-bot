package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import moe.arvin.kanonbot.music.filter.Config;
import moe.arvin.kanonbot.music.filter.TimescaleConfig;

import java.util.*;

public class FilterChainConfiguration {
    private final TimescaleConfig timescale = new TimescaleConfig();
    private final Map<Class<? extends Config>, Config> filters = new HashMap<>();

    public FilterChainConfiguration() {
        filters.put(timescale.getClass(), timescale);
    }

    public boolean isEnabled() {
        for (var config : filters.values()) {
            if (config.enabled()) {
                return true;
            }
        }
        return false;
    }

    public PcmFilterFactory factory() {
        return isEnabled() ? new Factory(this) : null;
    }

    public TimescaleConfig timescale() {
        return timescale;
    }

    private static class Factory implements PcmFilterFactory {
        private final FilterChainConfiguration configuration;

        private Factory(FilterChainConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output) {
            var list = new ArrayList<AudioFilter>();
            list.add(output);
            for (var config : configuration.filters.values()) {
                var filter = config.enabled() ? config.create(format, (FloatPcmAudioFilter) list.get(0)) : null;
                if (filter != null) {
                    list.add(0, filter);
                }
            }
            return list.subList(0, list.size() - 1);
        }
    }
}

