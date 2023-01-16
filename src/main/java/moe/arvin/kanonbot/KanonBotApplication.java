package moe.arvin.kanonbot;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "moe.arvin.kanonbot")
public class KanonBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanonBotApplication.class, args);
    }

}
