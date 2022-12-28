package moe.arvin.kanonbot.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.commands.Command;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class CommandListener implements EventListener<MessageCreateEvent> {

    private final Collection<Command> commands;

    @Value("${kanonbot.prefix}")
    private char cmdPrefix;

    public CommandListener(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        // check if bot msg or doesn't start with prefix
        String msgContent = event.getMessage().getContent();
        if (msgContent.isEmpty()) {
            return Mono.empty();
        }
        if (msgContent.charAt(0) != cmdPrefix ||
                !event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)) {
            return Mono.empty();
        }
        // create cmd name and args list
        String[] cmdSplit = msgContent.substring(1).trim().split(" ");
        String cmdName = cmdSplit[0];
        String[] cmdArgs = Arrays.copyOfRange(cmdSplit, 1, cmdSplit.length);

        return processCmd(cmdName, cmdArgs, event.getMessage());
    }

    public Mono<Void> processCmd(String cmdName, String[] cmdArgs, Message eventMessage) {
        return Flux.fromIterable(commands)
                .filter(command -> command.getName().equalsIgnoreCase(cmdName))
                .next()
                .flatMap(command -> command.handle(eventMessage));
    }
}
