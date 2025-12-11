

package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.Validator;
import ru.practicum.service.StatService;

import java.time.LocalDateTime;
import java.util.List;



@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
public class StatServiceController {

    private final StatService statService;
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    @PostMapping("/hit")
    public ResponseEntity<StatDto> addStatEvent(
            @RequestBody @Validated(Validator.Create.class) StatDto statDto) {

        StatDto statEvent = statService.createStat(statDto);
        log.info("POST /hit - statDto saved: {}", statDto);
        return new ResponseEntity<>(statEvent, HttpStatus.CREATED);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<StatResponseDto>> readStatEvent(
            @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("GET /stats - start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        List<StatResponseDto> stats = statService.readStat(start, end, uris, unique);
        log.info("GET /stats - returning {} records", stats.size());
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }
}

