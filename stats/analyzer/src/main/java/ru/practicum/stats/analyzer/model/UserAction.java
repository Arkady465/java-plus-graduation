package ru.practicum.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "action")
    private ActionTypeAvro action;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}