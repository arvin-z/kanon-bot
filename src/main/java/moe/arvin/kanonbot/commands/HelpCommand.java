package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.List;

@Component
public class HelpCommand implements Command {

    private final Collection<Command> commands;

    @Value("${kanonbot.prefix}")
    private char cmdPrefix;

    public HelpCommand(List<Command> commands, GuildAudioManagerFactory gAMFactory) {
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
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        for (Command cmd : commands) {
            builder.addField(cmd.getName(), cmd.getDescription(), false);
        }
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(builder.build()))
                .then();
    }
}
