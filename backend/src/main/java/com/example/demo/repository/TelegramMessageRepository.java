package com.example.demo.repository;

import com.example.demo.model.TelegramMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {
    
    Optional<TelegramMessage> findByMessageId(Integer messageId);
    
    List<TelegramMessage> findByChannelUsernameOrderByMessageDateDesc(String channelUsername);
    
    boolean existsByMessageId(Integer messageId);
}
