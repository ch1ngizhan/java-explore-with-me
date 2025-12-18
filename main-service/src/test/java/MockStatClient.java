import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.practicum.StatClient;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// MockStatClient.java
@Slf4j
@Profile("test")
@Component
public class MockStatClient extends StatClient {

    private final Map<String, AtomicInteger> hitCounts = new ConcurrentHashMap<>();
    private final Set<String> uniqueHits = new ConcurrentHashMap<String, String>().newKeySet();

    public MockStatClient() {
        super("http://localhost:9090", new RestTemplateBuilder());
    }

    @Override
    public void addStatEvent(StatDto statDto) {
        String key = statDto.getUri() + "_" + statDto.getIp();

        // Увеличиваем общий счетчик
        hitCounts.computeIfAbsent(statDto.getUri(), k -> new AtomicInteger(0))
                .incrementAndGet();

        // Добавляем уникальный хит
        uniqueHits.add(key);

        log.debug("Mock: сохранен хит для URI: {}, IP: {}", statDto.getUri(), statDto.getIp());
    }

    @Override
    public List<StatResponseDto> getStats(LocalDateTime start,
                                          LocalDateTime end,
                                          List<String> uris,
                                          boolean unique) {

        if (unique) {
            // Уникальные просмотры
            Map<String, Long> uniqueCounts = uris.stream()
                    .collect(Collectors.toMap(
                            uri -> uri,
                            uri -> uniqueHits.stream()
                                    .filter(hit -> hit.startsWith(uri + "_"))
                                    .count()
                    ));

            return uniqueCounts.entrySet().stream()
                    .map(entry -> new StatResponseDto("ewm-main-service", entry.getKey(), entry.getValue()))
                    .toList();
        } else {
            // Все просмотры
            return uris.stream()
                    .map(uri -> {
                        int count = hitCounts.getOrDefault(uri, new AtomicInteger(0)).get();
                        return new StatResponseDto("ewm-main-service", uri, count);
                    })
                    .toList();
        }
    }
}