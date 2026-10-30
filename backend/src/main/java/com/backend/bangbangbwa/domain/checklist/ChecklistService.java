package com.backend.bangbangbwa.domain.checklist;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.checklist.dto.*;
import com.backend.bangbangbwa.domain.checklist.entity.Checklist;
import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItem;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistItemRepository;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;

    @Transactional(readOnly = true)
    //체크리스트 조회
    public ChecklistResponse getChecklist(
            Long userId,
            Long meetingId
    ) {
        Checklist checklist = checklistRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHECKLIST_NOT_FOUND)
                );

        validateParticipant(userId, checklist);

        List<ChecklistItem> items =
                checklistItemRepository
                        .findAllByChecklist_IdOrderByIdAsc(
                                checklist.getId()
                        );

        return ChecklistResponse.from(checklist, items);
    }

    //체크리스트 항목 추가
    @Transactional
    public ChecklistItemResponse addChecklistItem(
            Long userId,
            Long meetingId,
            ChecklistItemCreateRequest request
    ){


        Checklist checklist = checklistRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHECKLIST_NOT_FOUND)
                );

        validateParticipant(userId, checklist);

        long itemCount =
                checklistItemRepository.countByChecklist_Id(checklist.getId());

        if (itemCount >= 20) {
            throw new CustomException(
                    ErrorCode.CHECKLIST_ITEM_LIMIT_EXCEEDED
            );
        }

        ChecklistItem item = ChecklistItem.builder()
                .checklist(checklist)
                .content(request.getContent().trim())
                .memo(request.getMemo())
                .build();

        ChecklistItem savedItem =
                checklistItemRepository.save(item);

        return ChecklistItemResponse.from(savedItem);
    }

    //체크리스트 항목 수정
    @Transactional
    public ChecklistItemResponse updateChecklistItem(
            Long userId,
            Long meetingId,
            Long itemId,
            ChecklistItemUpdateRequest request
    ) {
        Checklist checklist = checklistRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHECKLIST_NOT_FOUND)
                );

        validateParticipant(userId, checklist);

        ChecklistItem item =
                checklistItemRepository
                        .findByIdAndChecklist_Id(
                                itemId,
                                checklist.getId()
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CHECKLIST_ITEM_NOT_FOUND
                                )
                        );

        item.update(
                request.getContent().trim(),
                request.getMemo()
        );

        return ChecklistItemResponse.from(item);
    }

    //체크리스트 항목 삭제
    @Transactional
    public void deleteChecklistItem(
            Long userId,
            Long meetingId,
            Long itemId
    ) {
        Checklist checklist = checklistRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHECKLIST_NOT_FOUND)
                );

        validateParticipant(userId, checklist);

        ChecklistItem item =
                checklistItemRepository
                        .findByIdAndChecklist_Id(
                                itemId,
                                checklist.getId()
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CHECKLIST_ITEM_NOT_FOUND
                                )
                        );

        checklistItemRepository.delete(item);
    }

    //체크리스트 항목 상태 수정
    @Transactional
    public ChecklistItemResponse updateChecklistItemStatus(
            Long userId,
            Long meetingId,
            Long itemId,
            ChecklistItemStatusUpdateRequest request
    ) {
        Checklist checklist = checklistRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHECKLIST_NOT_FOUND)
                );

        validateParticipant(userId, checklist);

        ChecklistItem item =
                checklistItemRepository
                        .findByIdAndChecklist_Id(
                                itemId,
                                checklist.getId()
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CHECKLIST_ITEM_NOT_FOUND
                                )
                        );

        item.updateStatus(request.getStatus());

        return ChecklistItemResponse.from(item);
    }


    //-----공통 메서드-----
    //요청자 검증.
    private void validateParticipant(
            Long userId,
            Checklist checklist
    ) {
        if (!checklist.getMeeting().isTenant(userId)) {
            throw new CustomException(
                    ErrorCode.CHECKLIST_ACCESS_DENIED
            );
        }
    }

}
