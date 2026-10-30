package com.backend.bangbangbwa.domain.session.service;

public class SessionConcurrencyException extends RuntimeException {

    public SessionConcurrencyException(String message) {
        super(message);
    }
}
