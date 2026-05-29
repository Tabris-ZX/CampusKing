package zx.campusking.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomCode = roomCode(session);
        if (roomCode == null || roomCode.isBlank()) {
            return;
        }
        // Each room keeps its own subscriber set so only room members receive broadcasts.
        roomSessions.computeIfAbsent(roomCode, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomCode = roomCode(session);
        if (roomCode == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomCode);
        }
    }

    public void broadcast(String roomCode, String payload) {
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions == null) {
            return;
        }
        // Closed sessions are lazily removed during broadcast to keep the room set clean.
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ignored) {
            }
        }
    }

    private String roomCode(WebSocketSession session) {
        return session.getUri() == null ? null : session.getUri().getQuery() == null ? null :
                session.getUri().getQuery().replace("roomCode=", "").toUpperCase();
    }
}
