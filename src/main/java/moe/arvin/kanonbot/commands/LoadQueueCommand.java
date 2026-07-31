package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.SavedQueue;
import moe.arvin.kanonbot.music.SavedQueueService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Component
public class LoadQueueCommand implements Command {

    private final GuildAudioManagerFactory audioManagerFactory;
    private final SavedQueueService savedQueueService;

    public LoadQueueCommand(GuildAudioManagerFactory audioManagerFactory, SavedQueueService savedQueueService) {
        this.audioManagerFactory = audioManagerFactory;
        this.savedQueueService = savedQueueService;
    }

    @Override
    public String getName() {
        return "loadqueue";
    }

    @Override
    public String getDescription() {
        return "Append a saved queue: loadqueue <name>";
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
                    "There is no player queue in a DM. Use this command in a server to load music."
            );
        }

        long scopeId = SavedQueueCommandSupport.scopeId(message);
        Mono<Optional<SavedQueue>> savedQueue = Mono
                .fromCallable(() -> savedQueueService.find(scopeId, name))
                .subscribeOn(Schedulers.boundedElastic());

        GuildAudioManager audioManager = audioManagerFactory.get(guildId.get());
        return savedQueue.flatMap(queue -> {
            if (queue.isEmpty()) {
                return SavedQueueCommandSupport.reply(message, "No saved queue named **" + name + "** was found.");
            }

            SavedQueue foundQueue = queue.get();
            return Mono.zip(message.getAuthorAsMember(), message.getChannel())
                    .flatMap(context -> audioManager.getVoiceChatHandler().loadSavedQueue(
                            context.getT1(),
                            foundQueue.mediaUrls(),
                            context.getT2()
                    ))
                    .flatMap(loadedCount -> {
                        int failedCount = foundQueue.mediaUrls().size() - loadedCount;
                        String response = "Loaded " + loadedCount + " track(s) from **" + foundQueue.name() + "**.";
                        if (failedCount > 0) {
                            response += " " + failedCount + " track(s) could not be loaded.";
                        }
                        return SavedQueueCommandSupport.reply(message, response);
                    })
                    .onErrorResume(IllegalStateException.class,
                            error -> SavedQueueCommandSupport.reply(message, error.getMessage()));
        });
    }
}
