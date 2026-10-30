package com.backend.bangbangbwa.domain.session.signaling;

import com.backend.bangbangbwa.domain.session.signaling.dto.SignalMessage;
import com.backend.bangbangbwa.domain.session.signaling.dto.SignalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SignalHandler extends TextWebSocketHandler {

    private static final int MAX_PEERS = 2;

    private final ObjectMapper objectMapper;

    // roomToken -> (userId -> WebSocketSession)
    private final Map<String, Map<Long, WebSocketSession>> rooms =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomToken = getRoomToken(session);
        Long userId = getUserId(session);

        Map<Long, WebSocketSession> room = rooms.computeIfAbsent(
                roomToken,
                ignored -> new ConcurrentHashMap<>()
        );

        synchronized (room) {
            WebSocketSession previous = room.get(userId);
            if (previous != null && previous.isOpen()) {
                previous.close(
                        CloseStatus.NORMAL.withReason("새 연결로 교체되었습니다.")
                );
            }

            if (!room.containsKey(userId) && room.size() >= MAX_PEERS) {
                send(session, SignalMessage.error("1:1 RTC 방의 정원이 초과되었습니다."));
                session.close(
                        CloseStatus.POLICY_VIOLATION.withReason("Room is full")
                );
                return;
            }

            Long peerId = room.keySet().stream()
                    .filter(id -> !id.equals(userId))
                    .findFirst()
                    .orElse(null);

            room.put(userId, session);
            send(session, SignalMessage.joined(userId, peerId));

            if (peerId != null) {
                send(room.get(peerId), SignalMessage.peerJoined(userId));
            }
        }
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage textMessage
    ) throws Exception {
        SignalMessage incoming = objectMapper.readValue(
                textMessage.getPayload(),
                SignalMessage.class
        );

        if (!isRelayType(incoming.getType())) {
            send(session, SignalMessage.error("지원하지 않는 시그널 메시지입니다."));
            return;
        }

        String roomToken = getRoomToken(session);
        Long senderId = getUserId(session);
        Map<Long, WebSocketSession> room = rooms.get(roomToken);

        if (room == null) {
            send(session, SignalMessage.error("RTC 방을 찾을 수 없습니다."));
            return;
        }

        WebSocketSession peerSession = room.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(senderId))
                .map(Map.Entry::getValue)
                .filter(WebSocketSession::isOpen)
                .findFirst()
                .orElse(null);

        if (peerSession == null) {
            send(session, SignalMessage.error("상대방이 아직 입장하지 않았습니다."));
            return;
        }

        SignalMessage outgoing = SignalMessage.builder()
                .type(incoming.getType())
                .senderId(senderId)
                .sdp(incoming.getSdp())
                .candidate(incoming.getCandidate())
                .sdpMid(incoming.getSdpMid())
                .sdpMLineIndex(incoming.getSdpMLineIndex())
                .build();

        send(peerSession, outgoing);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) throws IOException {
        removeSession(session);
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) throws IOException {
        removeSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * leave API에서 특정 사용자만 실제로 퇴장시킵니다.
     */
    public boolean closeConnection(String roomToken, Long userId) {
        Map<Long, WebSocketSession> room = rooms.get(roomToken);
        if (room == null) {
            return false;
        }

        WebSocketSession session = room.get(userId);
        if (session == null) {
            return false;
        }

        try {
            // close 호출 후 afterConnectionClosed에서 방 제거 및 peerLeft 알림 처리
            if (session.isOpen()) {
                session.close(
                        CloseStatus.NORMAL.withReason("사용자가 RTC 세션에서 퇴장했습니다.")
                );
            } else {
                removeSession(session);
            }
            return true;
        } catch (IOException exception) {
            try {
                removeSession(session);
            } catch (IOException ignored) {
                // 연결 정리 중 발생한 예외는 이미 종료된 연결로 간주
            }
            return false;
        }
    }

    public void closeRoom(String roomToken) {
        Map<Long, WebSocketSession> room = rooms.remove(roomToken);
        if (room == null) {
            return;
        }

        room.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close(
                            CloseStatus.NORMAL.withReason("RTC 세션이 종료되었습니다.")
                    );
                }
            } catch (IOException ignored) {
                // 이미 끊어진 연결은 무시
            }
        });
    }

    private void removeSession(WebSocketSession session) throws IOException {
        String roomToken = getRoomToken(session);
        Long userId = getUserId(session);
        Map<Long, WebSocketSession> room = rooms.get(roomToken);

        if (room == null) {
            return;
        }

        boolean removed = room.remove(userId, session);
        if (!removed) {
            return;
        }

        for (WebSocketSession peerSession : room.values()) {
            send(peerSession, SignalMessage.peerLeft(userId));
        }

        if (room.isEmpty()) {
            rooms.remove(roomToken, room);
        }
    }

    private boolean isRelayType(SignalType type) {
        return type == SignalType.OFFER
                || type == SignalType.ANSWER
                || type == SignalType.ICE_CANDIDATE;
    }

    private String getRoomToken(WebSocketSession session) {
        return (String) session.getAttributes()
                .get(SignalingHandshakeInterceptor.ROOM_TOKEN);
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes()
                .get(SignalingHandshakeInterceptor.USER_ID);
    }

    private void send(
            WebSocketSession session,
            SignalMessage message
    ) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(message))
            );
        }
    }
}
