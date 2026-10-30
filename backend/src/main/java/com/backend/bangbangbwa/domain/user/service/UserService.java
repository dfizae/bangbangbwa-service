package com.backend.bangbangbwa.domain.user.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.oauth.dto.OAuthUserInfo;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthAccount;
import com.backend.bangbangbwa.domain.oauth.service.OAuthAccountService;
import com.backend.bangbangbwa.domain.oauth.service.S3ImageService;
import com.backend.bangbangbwa.domain.user.dto.UserResponse;
import com.backend.bangbangbwa.domain.user.dto.UserUpdateRequest;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OAuthAccountService oauthAccountService;
    private final S3ImageService s3ImageService;
    /**
     * 소셜 로그인 최초 가입 사용자 생성
     */
    @Transactional
    public User createFromOAuth(OAuthUserInfo userInfo) {
        User user = User.builder()
                .nickname(userInfo.getNickname())
                .name(userInfo.getName())
                .build();

        return userRepository.save(user);
    }

    public UserResponse getMyInfo(Long userId) {
        return buildUserResponse(userId);
    }

    /**
     * 내 정보 수정 (닉네임, 핸드폰 번호, 생년월일)
     */
    @Transactional
    public UserResponse updateMyInfo(
            Long userId,
            UserUpdateRequest request
    ) {
        User user = getActiveUser(userId);

        // 닉네임 변경 요청이 있는 경우
        if (request.getNickname() != null
                && !request.getNickname().equals(user.getNickname())) {
            boolean exists =
                    userRepository.existsByNicknameAndDeletedAtIsNullAndIdNot(
                            request.getNickname(),
                            userId
                    );

            if (exists) {
                throw new CustomException(
                        ErrorCode.NICKNAME_ALREADY_EXISTS
                );
            }
        }

        user.update(
                request.getNickname(),
                request.getBirth(),
                request.getPhone()
        );

        return buildUserResponse(userId);
    }

    /**
     * 프로필 이미지 수정
     */
    @Transactional
    public String updateProfileImage(
            Long userId,
            MultipartFile file
    ) {
        getActiveUser(userId);

        OAuthAccount oauthAccount =
                oauthAccountService.getByUserId(userId);

        String previousImageUrl =
                oauthAccount.getProfileImage();

        String uploadedImageUrl =
                s3ImageService.uploadProfileImage(
                        userId,
                        file
                );

        oauthAccount.updateProfileImage(uploadedImageUrl);

        registerProfileImageCleanup(
                previousImageUrl,
                uploadedImageUrl
        );

        return uploadedImageUrl;
    }

    private void registerProfileImageCleanup(
            String previousImageUrl,
            String uploadedImageUrl
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (previousImageUrl != null
                    && !previousImageUrl.equals(uploadedImageUrl)) {
                s3ImageService.delete(previousImageUrl);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (previousImageUrl == null
                                || previousImageUrl.equals(uploadedImageUrl)) {
                            return;
                        }

                        s3ImageService.delete(previousImageUrl);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            return;
                        }

                        s3ImageService.delete(uploadedImageUrl);
                    }
                }
        );
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = getActiveUser(userId);
        List<OAuthAccount> oauthAccounts =
                oauthAccountService.findAllByUserId(userId);

        oauthAccountService.deleteAllByUserId(userId);
        user.delete();

        registerAccountDeletionCleanup(oauthAccounts);
    }

    /**
     * 중개사 인증 완료 후 권한 변경
     */
    @Transactional
    public void promoteToAgent(Long userId) {
        User user = getActiveUser(userId);
        user.promoteToAgent();
    }

    /**
     * 탈퇴하지 않은 사용자 조회
     */
    private User getActiveUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private UserResponse buildUserResponse(Long userId) {
        User user = getActiveUser(userId);

        List<OAuthAccount> oauthAccounts =
                oauthAccountService.findAllByUserId(userId);

        return UserResponse.of(user, oauthAccounts);
    }

    private void registerAccountDeletionCleanup(
            List<OAuthAccount> oauthAccounts
    ) {
        List<String> imageUrls =
                oauthAccounts.stream()
                        .map(OAuthAccount::getProfileImage)
                        .filter(image -> image != null && !image.isBlank())
                        .toList();

        if (imageUrls.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageUrls.forEach(s3ImageService::delete);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        imageUrls.forEach(s3ImageService::delete);
                    }
                }
        );
    }
}
