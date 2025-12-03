package com.example.demo.service;

import com.example.demo.model.TelegramMessage;
import com.example.demo.repository.TelegramMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    @Autowired
    private TelegramMessageRepository messageRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.channel.username}")
    private String channelUsername;

    private final WebClient webClient;

    public TelegramService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public List<TelegramMessage> getChannelMessages() {
        return messageRepository.findByChannelUsernameOrderByMessageDateDesc(channelUsername);
    }

    public void fetchAndSaveMessages() {
        try {
            String url = String.format("/bot%s/getUpdates", botToken);
            
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && (Boolean) response.get("ok")) {
                List<Map<String, Object>> updates = (List<Map<String, Object>>) response.get("result");
                
                for (Map<String, Object> update : updates) {
                    if (update.containsKey("channel_post")) {
                        Map<String, Object> post = (Map<String, Object>) update.get("channel_post");
                        saveMessage(post);
                    }
                }
            }
        } catch (WebClientResponseException e) {
            logger.error("Error al obtener mensajes de Telegram: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado: {}", e.getMessage());
        }
    }

    private void saveMessage(Map<String, Object> post) {
        try {
            Integer messageId = (Integer) post.get("message_id");
            
            if (!messageRepository.existsByMessageId(messageId)) {
                TelegramMessage message = new TelegramMessage();
                message.setMessageId(messageId);
                message.setText((String) post.get("text"));
                message.setChannelUsername(channelUsername);
                
                Integer date = (Integer) post.get("date");
                LocalDateTime messageDate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(date), ZoneId.systemDefault());
                message.setMessageDate(messageDate);
                
                messageRepository.save(message);
                logger.info("Mensaje guardado: {}", messageId);
            }
        } catch (Exception e) {
            logger.error("Error al guardar mensaje: {}", e.getMessage());
        }
    }

    public TelegramMessage createManualMessage(String text) {
        TelegramMessage message = new TelegramMessage();
        message.setMessageId((int) (System.currentTimeMillis() / 1000));
        message.setText(text);
        message.setChannelUsername(channelUsername);
        message.setMessageDate(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
}
