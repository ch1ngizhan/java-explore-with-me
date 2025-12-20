package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.exception.StatsClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Slf4j
@Component
public class StatClient {
    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(@Value("${stats-service.url:http://localhost:9090}") String serverUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void addStatEvent(StatDto statDto) {
        try {
            restClient.post()
                    .uri("/hit")
                    .body(statDto)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Запись обращения к эндпоинту успешно сохранена: приложение {}, URI {}",
                    statDto.getApp(), statDto.getUri());
        } catch (Exception exception) {
            log.error("Ошибка при сохранении статистики: {}", exception.getMessage());
            throw new StatsClientException("Endpoint statistics could not be saved", exception);
        }
    }

    public List<StatResponseDto> getStats(LocalDateTime start, LocalDateTime end,
                                          List<String> uris, boolean unique) {
        try {
            List<StatResponseDto> result = restClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder.path("/stats")
                                .queryParam("start", start.format(formatter))
                                .queryParam("end", end.format(formatter));

                        if (uris != null && !uris.isEmpty()) {
                            uris.forEach(uri -> builder.queryParam("uris", uri));
                        }

                        builder.queryParam("unique", unique);

                        return builder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            log.debug("Получено записей статистики: {}", result != null ? result.size() : 0);

            return result;
        } catch (Exception exception) {
            log.error("Ошибка при получении статистики: {}", exception.getMessage());
            throw new StatsClientException("Statistics could not be retrieved", exception);
        }
    }
}