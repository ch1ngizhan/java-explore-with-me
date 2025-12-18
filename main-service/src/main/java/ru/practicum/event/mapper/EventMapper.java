package ru.practicum.event.mapper;

import org.mapstruct.*;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.user.model.User;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public interface EventMapper {

    @Named("toEventShortWithoutStats")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventShortDto toEventShortWithoutStats(Event event);

    @Named("toEventShortWithoutStatsSet")
    @IterableMapping(qualifiedByName = "toEventShortWithoutStats")
    Set<EventShortDto> toEventShortWithoutStatsSet(Set<Event> events);


    EventShortDto toEventShortDto(Event event);

    EventDto toEventDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "paid", defaultValue = "false")
    @Mapping(target = "participantLimit", defaultValue = "0")
    @Mapping(target = "requestModeration", defaultValue = "true")
    Event fromNewEvent(NewEventDto dto,
                       User initiator,
                       Category category,
                       EventState state);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "category", source = "category")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromUserRequest(UpdateEventUserRequest request,
                                    @MappingTarget Event event,
                                    Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "category", source = "category")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromAdminRequest(UpdateEventAdminRequest request,
                                     @MappingTarget Event event,
                                     Category category);
}