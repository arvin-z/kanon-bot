package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.SavedQueueService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Component
public class ListQueuesCommand implements Command {

    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    private final SavedQueueService savedQueueService;

    public ListQueuesCommand(SavedQueueService savedQueueService) {
        this.savedQueueService = savedQueueService;
    }

    @Override
    public String getName() {
        return "listqueues";
    }

    @Override
    public String getDescription() {
        return "List saved queues for this server or DM";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        long scopeId = SavedQueueCommandSupport.scopeId(message);
        return Mono.fromCallable(() -> savedQueueService.listNames(scopeId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(names -> SavedQueueCommandSupport.reply(message, formatNames(names)));
    }

    static String formatNames(List<String> names) {
        if (names.isEmpty()) {
            return "No queues have been saved here.";
        }

        StringBuilder result = new StringBuilder("Saved queues:\n");
        int displayed = 0;
        for (String name : names) {
            String line = "- " + name + "\n";
            String remaining = "\n…and " + (names.size() - displayed) + " more.";
            if (result.length() + line.length() + remaining.length() > DISCORD_MESSAGE_LIMIT) {
                result.append(remaining);
                break;
            }
            result.append(line);
            displayed++;
        }
        return result.toString().stripTrailing();
    }
}
