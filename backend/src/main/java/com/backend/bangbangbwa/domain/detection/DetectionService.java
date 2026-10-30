package com.backend.bangbangbwa.domain.detection;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureBatchCreateRequest;
import com.backend.bangbangbwa.domain.detection.dto.AiCaptureSaveResult;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureCreateRequest;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureDefectResponse;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureResponse;
import com.backend.bangbangbwa.domain.detection.dto.DefectCreateRequest;
import com.backend.bangbangbwa.domain.detection.dto.DefectResponse;
import com.backend.bangbangbwa.domain.detection.entity.Defect;
import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import com.backend.bangbangbwa.domain.detection.event.CaptureImageUploadedEvent;
import com.backend.bangbangbwa.domain.detection.repository.DefectRepository;
import com.backend.bangbangbwa.domain.detection.repository.SessionCaptureRepository;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectionService {

    private static final long MAX_IMAGE_FILE_SIZE = 5L * 1024 * 1024;
    private static final Duration IMAGE_URL_DURATION = Duration.ofMinutes(60);
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private final SessionRepository sessionRepository;
    private final SessionCaptureRepository sessionCaptureRepository;
    private final DefectRepository defectRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<SessionCaptureDefectResponse> createCaptures(
            Long userId,
            Long sessionId,
            List<MultipartFile> images,
            SessionCaptureBatchCreateRequest request
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);
        return saveCaptures(session, images, request);
    }

    @Transactional
    public AiCaptureSaveResult createAiCaptures(
            Long sessionId,
            List<MultipartFile> images,
            SessionCaptureBatchCreateRequest request
    ) {
        RtcSession session = sessionRepository
                .findWithMeetingByIdForUpdate(sessionId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.SESSION_NOT_FOUND)
                );

        validateCaptureCount(images, request);
        validateAiImages(images);
        List<SessionCaptureDefectResponse> captures =
                saveValidatedCaptures(session, images, request);

        return AiCaptureSaveResult.created(captures);
    }

    private List<SessionCaptureDefectResponse> saveCaptures(
            RtcSession session,
            List<MultipartFile> images,
            SessionCaptureBatchCreateRequest request
    ) {
        validateCaptureCount(images, request);
        validateImages(images);

        return saveValidatedCaptures(session, images, request);
    }

    private List<SessionCaptureDefectResponse> saveValidatedCaptures(
            RtcSession session,
            List<MultipartFile> images,
            SessionCaptureBatchCreateRequest request
    ) {

        List<SessionCapture> captures = new ArrayList<>();
        List<SessionCaptureCreateRequest> captureRequests =
                request.getCaptures();

        for (int index = 0; index < images.size(); index++) {
            MultipartFile image = images.get(index);
            SessionCaptureCreateRequest captureRequest =
                    captureRequests.get(index);

            String imageUrl = storageService.upload(
                    image,
                    "session-capture/" + session.getSessionId()
            );
            eventPublisher.publishEvent(
                    new CaptureImageUploadedEvent(imageUrl)
            );

            SessionCapture capture = SessionCapture.builder()
                    .session(session)
                    .imageUrl(imageUrl)
                    .capturedAt(captureRequest.getCapturedAt())
                    .build();

            addDefects(capture, captureRequest.getDefects());
            captures.add(capture);
        }

        return sessionCaptureRepository.saveAllAndFlush(captures).stream()
                .map(this::toDefectResponse)
                .toList();
    }

    @Transactional
    public SessionCaptureResponse createUserCapture(
            Long userId,
            Long sessionId,
            MultipartFile image
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);
        validateImage(image);

        String imageUrl = storageService.upload(
                image,
                "session-capture/" + sessionId
        );
        eventPublisher.publishEvent(
                new CaptureImageUploadedEvent(imageUrl)
        );

        SessionCapture capture = SessionCapture.builder()
                .session(session)
                .imageUrl(imageUrl)
                .build();

        return toResponse(sessionCaptureRepository.saveAndFlush(capture));
    }

    public List<SessionCaptureResponse> getCaptures(
            Long userId,
            Long sessionId
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);

        return sessionCaptureRepository
                .findBySession_SessionIdOrderByCapturedAtDesc(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DefectResponse> getDefects(
            Long userId,
            Long sessionId,
            Long captureId
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);
        validateCapture(sessionId, captureId);

        return defectRepository
                .findByCapture_IdOrderByCreatedAtAsc(captureId)
                .stream()
                .map(DefectResponse::from)
                .toList();
    }

    /**
     * 세션 캡처 중 AI 모델이 하자를 검출한 이미지만 추려서 반환합니다.
     * 오검출 확인 및 삭제 대상 선별 화면에서 사용합니다.
     */
    public List<SessionCaptureDefectResponse> getDefectCaptures(
            Long userId,
            Long sessionId
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);

        return sessionCaptureRepository
                .findWithDefectsBySession_SessionIdOrderByCapturedAtDesc(
                        sessionId
                )
                .stream()
                .filter(capture -> !capture.getDefects().isEmpty())
                .map(this::toDefectResponse)
                .toList();
    }

    /**
     * 오검출 등으로 불필요한 캡처 이미지를 한 번에 삭제합니다.
     * 세입자 본인만 삭제할 수 있으며, 요청한 캡처가 하나라도 해당 세션에 속하지
     * 않으면 아무것도 지우지 않고 실패합니다(부분 삭제 방지). 삭제 시 S3 원본
     * 이미지와 연관된 하자(defect)가 함께 제거됩니다(연관 관계의 cascade + orphanRemoval).
     */
    @Transactional
    public void deleteCaptures(
            Long userId,
            Long sessionId,
            List<Long> captureIds
    ) {
        RtcSession session = getSession(sessionId);
        validateTenant(session, userId);

        List<Long> distinctIds = captureIds.stream()
                .distinct()
                .toList();

        List<SessionCapture> captures = sessionCaptureRepository
                .findByIdInAndSession_SessionId(distinctIds, sessionId);

        if (captures.size() != distinctIds.size()) {
            throw new CustomException(ErrorCode.CAPTURE_NOT_FOUND);
        }

        captures.forEach(capture ->
                storageService.delete(capture.getImageUrl())
        );
        sessionCaptureRepository.deleteAll(captures);
    }

    private RtcSession getSession(Long sessionId) {
        return sessionRepository
                .findWithMeetingById(sessionId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.SESSION_NOT_FOUND)
                );
    }

    private void validateTenant(
            RtcSession session,
            Long userId
    ) {
        if (!session.getMeeting().isTenant(userId)) {
            throw new CustomException(ErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    private void validateCapture(
            Long sessionId,
            Long captureId
    ) {
        if (!sessionCaptureRepository
                .existsByIdAndSession_SessionId(
                        captureId,
                        sessionId
                )) {
            throw new CustomException(ErrorCode.CAPTURE_NOT_FOUND);
        }
    }

    private void validateCaptureCount(
            List<MultipartFile> images,
            SessionCaptureBatchCreateRequest request
    ) {
        if (images.size() != request.getCaptures().size()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateImages(List<MultipartFile> images) {
        if (images.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        for (MultipartFile image : images) {
            validateImage(image);
        }
    }

    private void validateAiImages(List<MultipartFile> images) {
        for (MultipartFile image : images) {
            validateImage(image);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (image.getSize() > MAX_IMAGE_FILE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(
                image.getContentType()
        )) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private void addDefects(
            SessionCapture capture,
            List<DefectCreateRequest> defectRequests
    ) {
        for (DefectCreateRequest request : defectRequests) {
            Defect defect = Defect.builder()
                    .type(request.getType())
                    .confidence(request.getConfidence())
                    .description(request.getDescription())
                    .bboxXCenter(request.getBboxXCenter())
                    .bboxYCenter(request.getBboxYCenter())
                    .bboxWidth(request.getBboxWidth())
                    .bboxHeight(request.getBboxHeight())
                    .build();
            capture.addDefect(defect);
        }
    }

    private SessionCaptureResponse toResponse(SessionCapture capture) {
        return SessionCaptureResponse.from(
                capture,
                createPresignedImageUrl(capture)
        );
    }

    private SessionCaptureDefectResponse toDefectResponse(
            SessionCapture capture
    ) {
        return SessionCaptureDefectResponse.from(
                capture,
                createPresignedImageUrl(capture)
        );
    }

    private String createPresignedImageUrl(SessionCapture capture) {
        return storageService.createPresignedGetUrl(
                capture.getImageUrl(),
                IMAGE_URL_DURATION
        );
    }
}
