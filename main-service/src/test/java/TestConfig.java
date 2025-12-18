import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import ru.practicum.StatClient;

// TestConfig.java в test/resources
@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public StatClient testStatClient() {
        return new MockStatClient();
    }
}