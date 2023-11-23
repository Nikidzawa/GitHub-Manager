package ru.nikidzawa.github_manager.telegram.config;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("classpath:application.properties")
public class BotConfiguration {
    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;
}
