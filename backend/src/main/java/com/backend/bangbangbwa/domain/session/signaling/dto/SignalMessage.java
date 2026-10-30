package com.backend.bangbangbwa.domain.session.signaling.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalMessage {

    private SignalType type;
    private Long senderId;
    private Long peerId;

    // SDP offer/answer 원문
    private String sdp;

    // ICE candidate 정보
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    private String message;

    public static SignalMessage joined(Long currentUserId, Long peerId) {
        return SignalMessage.builder()
                .type(SignalType.JOINED)
                .senderId(currentUserId)
                .peerId(peerId)
                .build();
    }

    public static SignalMessage peerJoined(Long peerId) {
        return SignalMessage.builder()
                .type(SignalType.PEER_JOINED)
                .peerId(peerId)
                .build();
    }

    public static SignalMessage peerLeft(Long peerId) {
        return SignalMessage.builder()
                .type(SignalType.PEER_LEFT)
                .peerId(peerId)
                .build();
    }

    public static SignalMessage error(String message) {
        return SignalMessage.builder()
                .type(SignalType.ERROR)
                .message(message)
                .build();
    }
}
