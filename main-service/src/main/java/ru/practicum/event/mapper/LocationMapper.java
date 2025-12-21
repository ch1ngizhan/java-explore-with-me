package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.EventLocation;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    EventLocation toEventLocation(LocationDto dto);

    LocationDto toDto(EventLocation entity);
}
