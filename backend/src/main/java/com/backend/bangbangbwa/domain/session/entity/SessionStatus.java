package com.backend.bangbangbwa.domain.session.entity;

public enum SessionStatus {

    /**
     * RTC 세션은 생성되었지만 아직 참여자가 입장하지 않은 상태
     */
    CREATED,

    /**
     * 한 명 이상의 참여자가 입장하여 세션이 시작된 상태
     */
    ACTIVE,

    /**
     * 최종 종료된 상태
     */
    ENDED
}
