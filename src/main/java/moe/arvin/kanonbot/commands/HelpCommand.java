package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

@Component
public class HelpCommand implements Command {

    private static final int MAX_FIELDS_PER_EMBED = 25;

    private final Collection<Command> commands;

    @Value("${kanonbot.prefix}")
    private char cmdPrefix;

    public HelpCommand(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show this menu";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        MessageCreateSpec response = MessageCreateSpec.builder()
                .addAllEmbeds(buildHelpEmbeds(commands, cmdPrefix))
                .build();
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(response))
                .then();
    }

    static List<EmbedCreateSpec> buildHelpEmbeds(Collection<Command> commands, char cmdPrefix) {
        List<Command> sortedCommands = commands.stream()
                .sorted(Comparator.comparing(Command::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<EmbedCreateSpec> embeds = new ArrayList<>();

        for (int start = 0; start < sortedCommands.size(); start += MAX_FIELDS_PER_EMBED) {
            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                    .color(Color.MOON_YELLOW)
                    .title(start == 0 ? "Commands" : "Commands (continued)");
            sortedCommands.stream()
                    .skip(start)
                    .limit(MAX_FIELDS_PER_EMBED)
                    .forEach(command -> builder.addField(
                            cmdPrefix + command.getName(),
                            command.getDescription(),
                            false
                    ));
            embeds.add(builder.build());
        }

        return List.copyOf(embeds);
    }
}
