package com.example.demo.controller;

import com.example.demo.model.TelegramMessage;
import com.example.demo.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    @Autowired
    private TelegramService telegramService;

    @GetMapping("/messages")
    public ResponseEntity<List<TelegramMessage>> getMessages() {
        List<TelegramMessage> messages = telegramService.getChannelMessages();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchMessages() {
        try {
            telegramService.fetchAndSaveMessages();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Mensajes actualizados correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al actualizar mensajes: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/manual")
    public ResponseEntity<?> createManualMessage(@RequestBody Map<String, String> payload) {
        try {
            String text = payload.get("text");
            if (text == null || text.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El texto no puede estar vacío");
                return ResponseEntity.badRequest().body(error);
            }
            
            TelegramMessage message = telegramService.createManualMessage(text);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al crear mensaje: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
