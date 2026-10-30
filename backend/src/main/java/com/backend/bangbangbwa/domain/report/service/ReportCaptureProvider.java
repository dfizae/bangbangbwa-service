package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;

import java.util.List;

/**
 * 리포트에 표시할 캡쳐 이미지 + 하자(defect) 데이터를 공급하는 포트.
 *
 * <p>실제 캡쳐/하자 데이터는 detection 모듈(domain.detection)에서 관리하므로,
 * 해당 모듈이 이 인터페이스를 구현하여 데이터를 제공합니다.
 * 리포트 도메인은 detection 내부 엔티티에 의존하지 않습니다.
 */
public interface ReportCaptureProvider {

    /**
     * 미팅에 속한 세션의 캡쳐 이미지 목록을 캡쳐 시각 순으로 반환합니다.
     *
     * @param meetingId 미팅 식별자
     * @return 캡쳐 목록(하자 포함). 데이터가 없으면 빈 목록
     */
    List<CaptureResponse> getCapturesByMeetingId(Long meetingId);
}
