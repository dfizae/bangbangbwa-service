package com.backend.bangbangbwa.domain.meeting.service;

import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import com.backend.bangbangbwa.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingCleanupScheduler {

    /**
     * 종료된 회의의 보관 기간(일). 이 기간이 지나면 목록에서 숨기고 연관 데이터를 삭제한다.
     */
    private static final int RETENTION_DAYS = 3;

    private final MeetingRepository meetingRepository;
    private final MeetingCleanupService meetingCleanupService;

    /**
     * 매일 새벽 3시(Asia/Seoul)에 보관 기간이 지난 종료 회의를 정리한다.
     *
     * <ul>
     *   <li>완료 회의: 세션 종료 시각(ended_at) + 3일 경과</li>
     *   <li>거절/취소 회의: 상태 전환 시각(updated_at) + 3일 경과</li>
     * </ul>
     *
     * 각 회의는 독립 트랜잭션에서 처리되며, 한 건이 실패해도 나머지는 계속 정리된다.
     */
    @Scheduled(
            cron = "${app.meeting.cleanup-cron:0 0 3 * * *}",
            zone = "Asia/Seoul"
    )
    public void cleanupExpiredMeetings() {
        LocalDateTime threshold =
                LocalDateTime.now().minusDays(RETENTION_DAYS);

        List<Long> targetIds = meetingRepository.findCleanupTargetIds(
                MeetingStatus.COMPLETED,
                List.of(
                        MeetingStatus.REJECTED,
                        MeetingStatus.CANCELED
                ),
                threshold
        );

        if (targetIds.isEmpty()) {
            return;
        }

        int deletedCount = 0;

        for (Long meetingId : targetIds) {
            try {
                meetingCleanupService.cleanupMeeting(meetingId);
                deletedCount++;
            } catch (Exception exception) {
                /*
                 * 개별 회의 정리 실패는 로그로 남기고 다음 회의를 계속 처리한다.
                 * 실패한 회의는 다음 스케줄에서 다시 정리 대상이 된다.
                 */
                log.warn(
                        "회의 정리에 실패했습니다. meetingId={}",
                        meetingId,
                        exception
                );
            }
        }

        log.info(
                "종료 회의 정리 완료. 대상={}, 삭제={}",
                targetIds.size(),
                deletedCount
        );
    }
}
