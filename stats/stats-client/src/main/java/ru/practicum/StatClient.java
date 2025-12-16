package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.exception.StatsClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StatClient extends BaseClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(@Value("${stats-service.url:http://localhost:9090}") String serverUrl,
                      RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .build()
        );
    }

    public ResponseEntity<Object> addStatEvent(StatDto stat) {
        return post("/hit", stat);
    }

    public ResponseEntity<Object> readStatEvent(String start, String end, @Nullable List<String> uris, boolean unique) {
        Map<String, Object> parameters = Map.of(
                "start", start,
                "end", end,
                "unique", unique
        );

        String path;
        if (uris == null || uris.isEmpty()) {
            path = "/stats?start={start}&end={end}&unique={unique}";
        } else {
            parameters.put("uris", String.join(",", uris));
            path = "/stats?start={start}&end={end}&uris={uris}&unique={unique}";
        }

        log.debug("Requesting stats with path: {}, parameters: {}", path, parameters);
        return get(path, parameters);
    }

    public List<StatResponseDto> getStats(LocalDateTime start,
                                          LocalDateTime end,
                                          List<String> uris,
                                          boolean unique) {
        try {
            Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("start", start.format(FORMATTER));
            parameters.put("end", end.format(FORMATTER));
            parameters.put("unique", unique);

            String path;
            if (uris == null || uris.isEmpty()) {
                path = "/stats?start={start}&end={end}&unique={unique}";
            } else {
                parameters.put("uris", String.join(",", uris));
                path = "/stats?start={start}&end={end}&uris={uris}&unique={unique}";
            }

            log.debug("Requesting stats with path: {}, parameters: {}", path, parameters);

            ResponseEntity<Object> response = get(path, parameters);

            @SuppressWarnings("unchecked")
            List<StatResponseDto> result = (List<StatResponseDto>) response.getBody();

            log.debug("Получено записей статистики: {}", result != null ? result.size() : 0);

            return result;
        } catch (Exception exception) {
            log.error("Ошибка при получении статистики", exception);
            throw new StatsClientException("Statistics could not be retrieved", exception);
        }
    }
}
