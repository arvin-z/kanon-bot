package moe.arvin.kanonbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class KanonBotApplicationTests {

    @Test
    void applicationEntryPointIsConfiguredForSpringBoot() {
        assertThat(KanonBotApplication.class)
                .hasAnnotation(SpringBootApplication.class);
    }
}
