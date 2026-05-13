package ru.practicum.stats.analyzer.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class EventSimilarityId implements Serializable {
    private long eventA;
    private long eventB;
}