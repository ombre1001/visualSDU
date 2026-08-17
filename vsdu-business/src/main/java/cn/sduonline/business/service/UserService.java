package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.ChangePasswordRequest;
import cn.sduonline.business.data.dto.UpdateUserProfileRequest;
import cn.sduonline.business.data.enums.ImageScene;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.po.UserBrowseHistory;
import cn.sduonline.business.data.vo.BrowseHistoryVO;
import cn.sduonline.business.data.vo.UserProfileVO;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.UserBrowseHistoryMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.exception.BadFileException;
import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import cn.sduonline.infrastructure.jwt.local.TokenRedisOperator;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserBrowseHistoryMapper historyMapper;
    private final MediaMapper mediaMapper;
    private final MediaService mediaService;
    private final ImageFileUpload imageFileUpload;
    private final FileStorage fileStorage;
    private final PasswordEncoder passwordEncoder;
    private final TokenRedisOperator tokenRedisOperator;

    public UserProfileVO profile(Long userId) {
        return toProfile(requireActiveUser(userId));
    }

    @Transactional
    public UserProfileVO updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = requireActiveUser(userId);
        if (request.nickname() == null && request.bio() == null) {
            throw new BizException(BizCode.USER_PROFILE_UPDATE_EMPTY);
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getDeleted, false)
                .set(User::getUpdatedAt, now);

        if (request.nickname() != null) {
            String nickname = request.nickname().strip();
            if (nickname.length() < 2 || nickname.length() > 30) {
                throw new BizException(BizCode.USER_NICKNAME_INVALID);
            }
            update.set(User::getNickname, nickname);
            user.setNickname(nickname);
        }
        if (request.bio() != null) {
            String bio = request.bio().strip();
            String normalizedBio = bio.isEmpty() ? null : bio;
            update.set(User::getBio, normalizedBio);
            user.setBio(normalizedBio);
        }

        if (userMapper.update(null, update) != 1) {
            throw new BizException(BizCode.AUTH_USER_NOT_FOUND);
        }
        user.setUpdatedAt(now);
        return toProfile(user);
    }

    @Transactional
    public UserProfileVO updateAvatar(Long userId, MultipartFile file) {
        User user = requireActiveUser(userId);
        String newKey;
        try {
            newKey = imageFileUpload.uploadImageFile(ImageScene.AVATAR, userId, file);
        } catch (BadFileException exception) {
            BizCode code = switch (exception.getErrorCode()) {
                case FILE_EMPTY -> BizCode.USER_AVATAR_FILE_EMPTY;
                case FILE_TOO_LARGE -> BizCode.USER_AVATAR_FILE_TOO_LARGE;
                case FILE_TYPE_NOT_SUPPORT -> BizCode.USER_AVATAR_FILE_TYPE_NOT_SUPPORT;
            };
            throw new BizException(code);
        }

        deleteOnRollback(newKey);
        String oldKey = user.getAvatarKey();
        LocalDateTime now = LocalDateTime.now();
        try {
            int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .eq(User::getDeleted, false)
                    .set(User::getAvatarKey, newKey)
                    .set(User::getUpdatedAt, now));
            if (updated != 1) throw new BizException(BizCode.AUTH_USER_NOT_FOUND);
        } catch (RuntimeException exception) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                fileStorage.deleteQuietly(newKey);
            }
            throw exception;
        }

        user.setAvatarKey(newKey);
        user.setUpdatedAt(now);
        if (hasText(oldKey) && !Objects.equals(oldKey, newKey)) {
            deleteAfterCommit(oldKey);
        }
        return toProfile(user);
    }

    @Transactional
    public UserProfileVO deleteAvatar(Long userId) {
        User user = requireActiveUser(userId);
        String oldKey = user.getAvatarKey();
        if (!hasText(oldKey)) return toProfile(user);

        LocalDateTime now = LocalDateTime.now();
        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getDeleted, false)
                .set(User::getAvatarKey, null)
                .set(User::getUpdatedAt, now));
        if (updated != 1) throw new BizException(BizCode.AUTH_USER_NOT_FOUND);

        user.setAvatarKey(null);
        user.setUpdatedAt(now);
        deleteAfterCommit(oldKey);
        return toProfile(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireActiveUser(userId);
        String passwordHash = user.getPasswordHash();
        if (!hasText(passwordHash)) {
            throw new BizException(BizCode.USER_PASSWORD_NOT_CONFIGURED);
        }
        if (utf8Length(request.currentPassword()) > 72
                || !passwordEncoder.matches(request.currentPassword(), passwordHash)) {
            throw new BizException(BizCode.USER_CURRENT_PASSWORD_INCORRECT);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BizException(BizCode.USER_PASSWORD_CONFIRM_MISMATCH);
        }
        if (utf8Length(request.newPassword()) > 72) {
            throw new BizException(BizCode.USER_PASSWORD_TOO_LONG);
        }
        if (passwordEncoder.matches(request.newPassword(), passwordHash)) {
            throw new BizException(BizCode.USER_PASSWORD_UNCHANGED);
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getPasswordHash, passwordHash)
                .eq(User::getDeleted, false)
                .set(User::getPasswordHash, newHash)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BizException(BizCode.USER_CURRENT_PASSWORD_INCORRECT);
        }

        userMapper.increaseTokenVersion(userId);
        tokenRedisOperator.deleteAllRefreshTokens(userId);
        tokenRedisOperator.deleteTokenVersionCache(userId);
        deleteTokenVersionCacheAfterCommit(userId);
    }

    public PageResult<BrowseHistoryVO> history(Long userId, long page, long size) {
        requireFormalUser(userId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        long total = historyMapper.countVisibleByUser(userId);
        long offset = pageOffset(safePage, safeSize);
        if (total == 0 || offset >= total) {
            return new PageResult<>(total, safePage, safeSize, List.of());
        }

        List<BrowseHistoryVO> items = historyMapper.selectVisiblePage(userId, offset, safeSize)
                .stream()
                .map(this::toHistory)
                .filter(Objects::nonNull)
                .toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    @Transactional
    public void clearHistory(Long userId) {
        requireFormalUser(userId);
        historyMapper.deleteByUser(userId);
    }

    @Transactional
    public void deleteHistory(Long userId, Long mediaId) {
        requireFormalUser(userId);
        historyMapper.deleteByUserAndMedia(userId, mediaId);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BizException(BizCode.AUTH_USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BizException(BizCode.FROZEN_USER);
        }
        return user;
    }

    private void requireFormalUser(Long userId) {
        User user = requireActiveUser(userId);
        if (!hasText(user.getCasId())) {
            throw new BizException(BizCode.FORMAL_USER_REQUIRED);
        }
    }

    private UserProfileVO toProfile(User user) {
        String avatarUrl = hasText(user.getAvatarKey())
                ? fileStorage.getUrl(user.getAvatarKey())
                : null;
        return new UserProfileVO(
                user.getId(), maskPhone(user.getPhone()), user.getCasId(), user.getName(),
                user.getNickname(), avatarUrl, user.getBio(), user.getRole(), user.getStatus(),
                Boolean.TRUE.equals(user.getAllowUpload()), Boolean.TRUE.equals(user.getAllowDownload()),
                hasText(user.getPasswordHash()), user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    private BrowseHistoryVO toHistory(UserBrowseHistory history) {
        Media media = mediaMapper.selectById(history.getMediaId());
        if (media == null || !Objects.equals(media.getStatus(), 1)) return null;
        return new BrowseHistoryVO(
                mediaService.toSummary(media),
                Objects.requireNonNullElse(history.getViewCount(), 0L),
                history.getLastViewedAt()
        );
    }

    private long pageOffset(long page, long size) {
        long pageIndex = page - 1;
        if (pageIndex > Long.MAX_VALUE / size) return Long.MAX_VALUE;
        return pageIndex * size;
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private String maskPhone(String phone) {
        if (!hasText(phone)) return null;
        String normalized = phone.strip();
        if (normalized.length() < 7) return "*".repeat(normalized.length());
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void deleteOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) fileStorage.deleteQuietly(key);
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorage.deleteQuietly(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorage.deleteQuietly(key);
            }
        });
    }

    private void deleteTokenVersionCacheAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tokenRedisOperator.deleteTokenVersionCache(userId);
            }
        });
    }

}
