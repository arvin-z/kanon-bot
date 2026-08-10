package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class HelpCommandTests {

    @Test
    void splitsCommandsAcrossMultipleEmbedsWithoutExceedingDiscordFieldLimit() {
        List<Command> commands = IntStream.range(0, 27)
                .mapToObj(index -> command("command" + index))
                .toList();

        List<EmbedCreateSpec> embeds = HelpCommand.buildHelpEmbeds(commands, '/');

        assertThat(embeds).hasSize(2);
        assertThat(embeds.get(0).fields()).hasSize(25);
        assertThat(embeds.get(1).fields()).hasSize(2);
        assertThat(embeds)
                .allSatisfy(embed -> assertThat(embed.colorOrElse(Color.RED)).isEqualTo(Color.MOON_YELLOW));
        assertThat(embeds.get(0).fields().getFirst().name()).startsWith("/");
    }

    private Command command(String name) {
        return new Command() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return "Description for " + name;
            }

            @Override
            public Mono<Void> handle(Message message, String msgArg) {
                return Mono.empty();
            }
        };
    }
}
