package com.backend.bangbangbwa.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(409, "이미 사용 중인 닉네임입니다."),
    USER_ALREADY_DELETED(400, "이미 탈퇴한 회원입니다."),

    EMPTY_FILE(400, "업로드할 파일이 없습니다."),
    INVALID_IMAGE_TYPE(400, "JPEG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다."),
    FILE_SIZE_EXCEEDED(400, "프로필 이미지는 5MB 이하여야 합니다."),
    FILE_UPLOAD_FAILED(500, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DOWNLOAD_FAILED(500, "파일 다운로드 중 오류가 발생했습니다."),

    // OAuth
    INVALID_SOCIAL_TOKEN(401, "유효하지 않은 소셜 로그인 토큰입니다."),
    OAUTH_ACCOUNT_NOT_FOUND(404, "소셜 계정을 찾을 수 없습니다."),

    // JWT
    INVALID_ACCESS_TOKEN(401, "유효하지 않은 Access Token입니다."),
    EXPIRED_ACCESS_TOKEN(401, "Access Token이 만료되었습니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),

    // Property
    PROPERTY_NOT_FOUND(404, "매물을 찾을 수 없습니다."),
    PROPERTY_ACCESS_DENIED(403, "해당 매물에 대한 권한이 없습니다."),
    INVALID_TRANSACTION_TYPE(400, "유효하지 않은 거래 유형입니다."),
    INVALID_ROOM_TYPE(400, "유효하지 않은 방 종류입니다."),
    INVALID_PROPERTY_STATUS(400, "유효하지 않은 매물 상태입니다."),
    MONTHLY_RENT_REQUIRED(400, "월세는 필수입니다."),
    DEPOSIT_REQUIRED(400, "거래 유형 변경 시 보증금(매매가)은 필수입니다."),

    // Meeting
    MEETING_NOT_FOUND(404, "회의 예약을 찾을 수 없습니다."),
    MEETING_REQUEST_ALREADY_EXISTS(409, "해당 매물에 진행 중인 회의 요청이 이미 존재합니다."),
    MEETING_SCHEDULE_CONFLICT(409, "해당 시간에는 이미 확정된 회의가 있습니다."),
    MEETING_ALREADY_CANCELED(409, "이미 취소된 회의 예약입니다."),
    MEETING_CANCEL_DEADLINE_PASSED(400, "회의 시작 3시간 전까지만 예약을 취소할 수 있습니다."),
    MEETING_SAME_PARTICIPANT(400, "임차인과 공인중개사는 동일한 사용자일 수 없습니다."),
    INVALID_MEETING_DATE(400, "회의 일시는 현재 시간 이후여야 합니다."),
    INVALID_MEETING_STATUS(409, "현재 회의 상태에서는 요청한 작업을 수행할 수 없습니다."),
    DUPLICATE_PREFERRED_TIME(400, "희망 회의 시간은 중복될 수 없습니다."),
    INVALID_MEETING_TIME_SLOT(400, "회의 시간은 정각 또는 30분 단위여야 합니다."),

    // Property Image
    IMAGE_NOT_FOUND(404, "이미지를 찾을 수 없습니다."),
    CAPTURE_NOT_FOUND(404, "세션 캡처를 찾을 수 없습니다."),
    IMAGE_COUNT_EXCEEDED(400, "매물당 최대 10장까지 등록할 수 있습니다."),
    IMAGE_SIZE_EXCEEDED(400, "이미지 파일 크기가 허용치를 초과했습니다."),
    STORAGE_NOT_CONFIGURED(503, "이미지 저장소가 아직 설정되지 않았습니다."),

    // Property Document (AI 서류 검증)
    INVALID_DOCUMENT_CATEGORY(400, "유효하지 않은 서류 종류입니다. (등기부등본/건축물대장)"),
    UNSUPPORTED_DOCUMENT_FILE_TYPE(400, "서류는 PDF 파일만 업로드할 수 있습니다."),
    DOCUMENT_FILE_SIZE_EXCEEDED(400, "서류 파일 크기가 허용치를 초과했습니다."),
    DOCUMENT_NOT_FOUND(404, "서류를 찾을 수 없습니다."),
    DOCUMENT_NOT_READY(409, "서류 분석이 아직 완료되지 않았습니다."),
    DOCUMENT_ANALYSIS_FAILED(500, "서류 AI 분석에 실패했습니다."),

    // Session
    SESSION_NOT_FOUND(404, "존재하지 않는 RTC 세션입니다."),
    SESSION_ACCESS_DENIED(403, "해당 RTC 세션에 접근할 권한이 없습니다."),
    SESSION_ALREADY_ENDED(409, "이미 종료된 RTC 세션입니다."),
    SESSION_EXPIRED(409, "세션이 만료되었습니다."),

    // Agent Verification
    INVALID_BROKER_LICENSE_FORMAT(400, "중개업자 등록번호 형식이 올바르지 않습니다."),
    BROKER_LICENSE_NOT_FOUND(404, "공공데이터에서 중개업자 정보를 찾을 수 없습니다."),
    INVALID_BROKER_BUSINESS_STATUS(400, "영업 중인 중개업소만 인증을 신청할 수 있습니다."),
    BROKER_VERIFICATION_ALREADY_PENDING(409, "이미 심사 중인 중개사 인증 요청이 있습니다."),
    BROKER_VERIFICATION_ALREADY_APPROVED(409, "이미 중개사 인증이 승인되었습니다."),
    BROKER_VERIFICATION_NOT_FOUND(404, "중개사 인증 요청을 찾을 수 없습니다."),
    AGENT_VERIFICATION_ALREADY_REVIEWED(409, "이미 심사가 완료된 중개사 인증 요청입니다."),
    INVALID_AGENT_VERIFICATION_STATUS(409, "현재 인증 상태에서는 요청한 작업을 수행할 수 없습니다."),
    INVALID_VERIFICATION_DECISION(400, "승인 또는 반려만 선택할 수 있습니다."),
    EXTERNAL_PUBLIC_API_FAILED(502, "공공데이터 API 호출에 실패했습니다."),
    EXTERNAL_PUBLIC_API_TIMEOUT(504, "공공데이터 API 응답 시간이 초과되었습니다."),
    INVALID_DOCUMENT_TYPE(400, "PDF, JPG, PNG, WEBP 서류만 업로드할 수 있습니다."),
    EMPTY_VERIFICATION_FILE(400, "업로드할 인증 서류가 없습니다."),
    VERIFICATION_FILE_SIZE_EXCEEDED(400, "인증 서류는 10MB 이하여야 합니다."),
    ADMIN_ACCESS_DENIED(403, "관리자 권한이 필요합니다."),

    // Checklist
    CHECKLIST_NOT_FOUND(404, "체크리스트를 찾을 수 없습니다."),
    CHECKLIST_ITEM_NOT_FOUND(404, "체크리스트 항목을 찾을 수 없습니다."),
    CHECKLIST_ALREADY_EXISTS(409, "해당 미팅의 체크리스트가 이미 존재합니다."),
    CHECKLIST_ACCESS_DENIED(403, "해당 체크리스트에 접근할 권한이 없습니다."),
    CHECKLIST_GENERATION_FAILED(500, "체크리스트 생성에 실패했습니다."),
    CHECKLIST_ITEM_LIMIT_EXCEEDED( 409, "체크리스트 항목은 최대 20개까지 등록할 수 있습니다." ),

    // Report
    REPORT_ALREADY_EXISTS(409, "리포트가 존재합니다."),
    REPORT_NOT_FOUND(404, "리포트를 찾을 수 없습니다."),
    REPORT_ACCESS_DENIED(403, "해당 리포트에 접근할 권한이 없습니다."),
    SESSION_NOT_ENDED(409, "세션이 아직 종료되지 않아 리포트를 생성할 수 없습니다."),
    REPORT_NOT_COMPLETED(409, "리포트 생성이 아직 완료되지 않았습니다."),
    REPORT_GENERATION_FAILED(500, "리포트 생성에 실패했습니다."),
    PDF_GENERATION_FAILED(500, "리포트 PDF 생성 중 오류가 발생했습니다."),

    // Common
    INVALID_REQUEST(400, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
