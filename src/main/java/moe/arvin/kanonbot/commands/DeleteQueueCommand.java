package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.SavedQueueService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class DeleteQueueCommand implements Command {

    private final SavedQueueService savedQueueService;

    public DeleteQueueCommand(SavedQueueService savedQueueService) {
        this.savedQueueService = savedQueueService;
    }

    @Override
    public String getName() {
        return "deletequeue";
    }

    @Override
    public String getDescription() {
        return "Delete a saved queue: deletequeue <name>";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        final String name;
        try {
            name = savedQueueService.validateName(msgArg);
        } catch (IllegalArgumentException error) {
            return SavedQueueCommandSupport.error(message, error.getMessage());
        }

        long scopeId = SavedQueueCommandSupport.scopeId(message);
        return Mono.fromCallable(() -> savedQueueService.delete(scopeId, name))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(deleted -> {
                    if (deleted) {
                        return SavedQueueCommandSupport.success(message, "Deleted saved queue **" + name + "**");
                    }
                    return SavedQueueCommandSupport.error(
                            message,
                            "No saved queue named **" + name + "** was found!"
                    );
                });
    }
}
