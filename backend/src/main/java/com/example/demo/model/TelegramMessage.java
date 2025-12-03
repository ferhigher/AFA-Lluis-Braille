package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true)
    private Integer messageId;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "channel_username")
    private String channelUsername;

    @Column(name = "message_date")
    private LocalDateTime messageDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
