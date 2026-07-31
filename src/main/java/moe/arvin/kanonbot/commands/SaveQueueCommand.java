package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.SavedQueueService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Component
public class SaveQueueCommand implements Command {

    private final GuildAudioManagerFactory audioManagerFactory;
    private final SavedQueueService savedQueueService;

    public SaveQueueCommand(GuildAudioManagerFactory audioManagerFactory, SavedQueueService savedQueueService) {
        this.audioManagerFactory = audioManagerFactory;
        this.savedQueueService = savedQueueService;
    }

    @Override
    public String getName() {
        return "savequeue";
    }

    @Override
    public String getDescription() {
        return "Save the current queue: savequeue <name>";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        final String name;
        try {
            name = savedQueueService.validateName(msgArg);
        } catch (IllegalArgumentException error) {
            return SavedQueueCommandSupport.reply(message, error.getMessage());
        }

        Optional<Snowflake> guildId = message.getGuildId();
        if (guildId.isEmpty()) {
            return SavedQueueCommandSupport.reply(
                    message,
                    "There is no player queue in a DM. Use this command in the server whose queue you want to save."
            );
        }

        List<String> mediaUrls = audioManagerFactory.get(guildId.get()).getScheduler().getMediaUrls();
        if (mediaUrls.isEmpty()) {
            return SavedQueueCommandSupport.reply(message, "The current queue is empty.");
        }

        long scopeId = SavedQueueCommandSupport.scopeId(message);
        return Mono.fromRunnable(() -> savedQueueService.save(scopeId, name, mediaUrls))
                .subscribeOn(Schedulers.boundedElastic())
                .then(SavedQueueCommandSupport.reply(
                        message,
                        "Saved queue **" + name + "** with " + mediaUrls.size() + " track(s)."
                ));
    }
}
