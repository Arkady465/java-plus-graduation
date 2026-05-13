package ru.practicum.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@IdClass(EventSimilarityId.class)
@Table(name = "event_similarity")
@Getter
@Setter
@NoArgsConstructor
public class EventSimilarity {
    @Id
    @Column(name = "eventA")
    private long eventA;

    @Id
    @Column(name = "eventB")
    private long eventB;

    @Column(name = "score")
    private double score;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}