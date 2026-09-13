package com.paymentplatform.notification.infrastructure.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.application.dto.NotificationResponse;
import com.paymentplatform.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class NotificationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcaster.class);

    private final ConcurrentHashMap<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String key, SseEmitter emitter) {
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));
        log.debug("SSE client registered: key={}, total={}", key, emitters.getOrDefault(key, List.of()).size());
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(key);
            }
        }
    }

    public void broadcastNotification(Notification notification) {
        if (notification.recipientOrganizationId() != null) {
            sendToKey("org:" + notification.recipientOrganizationId(), notification);
        }
        if (notification.recipientUserId() != null) {
            sendToKey("user:" + notification.recipientUserId(), notification);
        }
    }

    private void sendToKey(String key, Notification notification) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null || list.isEmpty()) return;

        NotificationResponse dto = NotificationResponse.from(notification);
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(objectMapper.writeValueAsString(dto)));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    public int getActiveCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    public void sendHeartbeat() {
        List<SseEmitter> dead = new ArrayList<>();
        for (var entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(""));
                } catch (IOException | IllegalStateException e) {
                    dead.add(emitter);
                }
            }
            entry.getValue().removeAll(dead);
            dead.clear();
        }
    }
}
