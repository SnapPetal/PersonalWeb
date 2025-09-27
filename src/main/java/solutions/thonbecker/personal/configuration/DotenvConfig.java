package solutions.thonbecker.personal.configuration;

import io.github.cdimascio.dotenv.Dotenv;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DotenvConfig {

    @PostConstruct
    public void loadEnvironmentVariables() {
        try {
            Dotenv dotenv = Dotenv.configure().directory("./").ignoreIfMissing().load();

            // Set system properties so Spring can access them
            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });

            System.out.println("✅ Loaded environment variables from .env file");
        } catch (Exception e) {
            System.out.println("⚠️ Could not load .env file: " + e.getMessage());
        }
    }
}
