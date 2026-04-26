package ru.practicum.core.comment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@ToString
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text")
    private String text;

    @Column(name = "user_id")
    private Long user;

    @Column(name = "event_id")
    private Long event;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "edited_on")
    private LocalDateTime editedOn = null;
}