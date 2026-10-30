package com.backend.bangbangbwa.domain.session.signaling.dto;

public enum SignalType {
    JOINED,
    PEER_JOINED,
    PEER_LEFT,
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    ERROR
}
