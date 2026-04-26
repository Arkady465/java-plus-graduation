package ru.practicum.core.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import ru.practicum.core.common.dto.event.*;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.model.*;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiator", source = "userDto.id")
    @Mapping(target = "location", source = "newEventDto.location")
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "category", source = "newEventDto.category", qualifiedByName = "categoryFromId")
    Event toEvent(NewEventDto newEventDto, UserDto userDto);

    @Named("categoryFromId")
    default Category categoryFromId(Long id) {
        if (id == null) {
            return null;
        }

        Category category = new Category();
        category.setId(id);

        return category;
    }

    @Mapping(target = "confirmedRequests", source = "requests")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "initiator", source = "userShortDto")
    @Mapping(target = "id", source = "event.id")
    EventShortDto toShortDto(Event event, Long requests, Double rating, Long comments, UserShortDto userShortDto);

    @Mapping(target = "confirmedRequests", source = "requests")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "initiator", source = "userDto", qualifiedByName = "toUserShortDto")
    @Mapping(target = "id", source = "event.id")
    EventFullDto toFullDto(Event event, Long requests, Double rating, Long comments, UserDto userDto);

    @Mapping(target = "confirmedRequests", source = "requests")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "initiator", source = "userShortDto")
    @Mapping(target = "id", source = "event.id")
    EventFullDto toFullDto(Event event, Long requests, Double rating, Long comments, UserShortDto userShortDto);

    Location toLocation(LocationDto locationDto);

    @Named("toUserShortDto")
    UserShortDto toUserShortDto(UserDto userDto);

    default void applyTo(UpdateEventRequest updateEventRequest, Event event,
                         Category category, Location location, EventState eventState) {
        if (updateEventRequest.hasTitle()) {
            event.setTitle(updateEventRequest.getTitle());
        }

        if (updateEventRequest.hasAnnotation()) {
            event.setAnnotation(updateEventRequest.getAnnotation());
        }

        if (updateEventRequest.hasDescription()) {
            event.setDescription(updateEventRequest.getDescription());
        }

        if (updateEventRequest.hasCategory()) {
            event.setCategory(category);
        }

        if (updateEventRequest.hasLocationDto()) {
            event.setLocation(location);
        }

        if (updateEventRequest.hasPaid()) {
            event.setPaid(updateEventRequest.getPaid());
        }

        if (updateEventRequest.hasParticipantLimit()) {
            event.setParticipantLimit(updateEventRequest.getParticipantLimit());
        }

        if (updateEventRequest.hasRequestModeration()) {
            event.setRequestModeration(updateEventRequest.getRequestModeration());
        }

        if (updateEventRequest.hasStateAction()) {
            event.setState(eventState);
        }

        if (updateEventRequest.hasEventDate()) {
            event.setEventDate(updateEventRequest.getEventDate());
        }
    }
}