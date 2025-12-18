package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StatClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        restTemplate.exchange(
                "/hit",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(statDto),
                Void.class
        );
    }



    public List<StatResponseDto> getStats(LocalDateTime start,
                                          LocalDateTime end,
                                          List<String> uris,
                                          boolean unique,
                                          String app) {

        Map<String, Object> params = new HashMap<>();
        params.put("start", start.format(FORMATTER));
        params.put("end", end.format(FORMATTER));
        params.put("unique", unique);
        params.put("app", app);

        String path;
        if (uris == null || uris.isEmpty()) {
            path = "/stats?start={start}&end={end}&unique={unique}&app={app}";
        } else {
            params.put("uris", uris);
            path = "/stats?start={start}&end={end}&uris={uris}&unique={unique}&app={app}";
        }

        ResponseEntity<List<StatResponseDto>> response =
                restTemplate.exchange(
                        path,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {},
                        params
                );

        List<StatResponseDto> body = response.getBody();
        log.debug("GET /stats <- {} records", body == null ? 0 : body.size());

        return body == null ? List.of() : body;
    }
}
