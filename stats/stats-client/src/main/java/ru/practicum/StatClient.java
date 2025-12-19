package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class StatClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final org.springframework.web.client.RestTemplate restTemplate;

    public StatClient(
            @Value("${stats-service.url:http://localhost:9090}") String serverUrl,
            RestTemplateBuilder builder
    ) {
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .build();
    }


    public void addStatEvent(StatDto statDto) {
        log.debug("POST /hit -> {}", statDto);
        try {
            restTemplate.exchange(
                    "/hit",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(statDto),
                    Void.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("Не удалось отправить статистику: {} {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage(), e);
        }
    }


    public List<StatResponseDto> getStats(LocalDateTime start,
                                          LocalDateTime end,
                                          List<String> uris,
                                          boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromPath("/stats")
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique);

            if (uris != null) {
                uris.forEach(uri -> builder.queryParam("uris", uri));
            }

            ResponseEntity<List<StatResponseDto>> response =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {
                            }
                    );

            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
