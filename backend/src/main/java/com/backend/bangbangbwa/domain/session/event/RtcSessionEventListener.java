package com.backend.bangbangbwa.domain.session.event;

import com.backend.bangbangbwa.domain.session.signaling.SignalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RtcSessionEventListener {

    private final SignalHandler signalHandler;

    /**
     * DB 트랜잭션이 실제로 커밋된 후에만 WebSocket 방을 닫습니다.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSessionClosed(
            RtcSessionClosedEvent event
    ) {
        signalHandler.closeRoom(event.roomToken());
    }
}
