package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminUpdateUserPermissionRequest;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.AdminUserVO;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import cn.sduonline.infrastructure.jwt.local.TokenRedisOperator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final FileStorage fileStorage;
    private final TokenRedisOperator tokenRedisOperator;

    public PageResult<AdminUserVO> list(
            String keyword,
            UserRole role,
            UserStatus status,
            long page,
            long size
    ) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 100);
        String normalizedKeyword = hasText(keyword) ? keyword.strip() : null;

        long total = userMapper.countAdmin(
                normalizedKeyword,
                role == null ? null : role.getValue(),
                status == null ? null : status.getValue()
        );
        long offset = (safePage - 1) * safeSize;
        var users = total == 0 ? java.util.List.<User>of() : userMapper.selectAdminPage(
                normalizedKeyword,
                role == null ? null : role.getValue(),
                status == null ? null : status.getValue(),
                offset,
                safeSize
        );
        return new PageResult<>(
                total, safePage, safeSize,
                users.stream().map(this::toVO).toList()
        );
    }

    public AdminUserVO detail(Long userId) {
        return toVO(requireUser(userId));
    }

    @Transactional
    public AdminUserVO updateRole(Long operatorId, Long userId, UserRole newRole) {
        User user = requireUser(userId);
        if (Objects.equals(operatorId, userId)) {
            throw new BizException(BizCode.ADMIN_USER_SELF_ROLE_FORBIDDEN);
        }
        if (user.getRole() == newRole) return toVO(user);
        if (user.getRole() == UserRole.ADMIN && newRole == UserRole.USER) {
            ensureAnotherAvailableAdmin(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userMapper.updateRoleAndIncreaseTokenVersion(
                userId, user.getRole().getValue(), newRole.getValue(), now
        );
        if (updated != 1) throw new BizException(BizCode.ADMIN_USER_NOT_FOUND);

        invalidateLoginCredentials(userId);
        user.setRole(newRole);
        user.setUpdatedAt(now);
        return toVO(user);
    }

    @Transactional
    public AdminUserVO updateStatus(
            Long operatorId,
            Long userId,
            UserStatus newStatus,
            LocalDateTime frozenUntil,
            String frozenReason
    ) {
        User user = requireUser(userId);
        if (Objects.equals(operatorId, userId) && newStatus != UserStatus.NORMAL) {
            throw new BizException(BizCode.ADMIN_USER_SELF_STATUS_FORBIDDEN);
        }
        if (user.getStatus() == newStatus
                && newStatus != UserStatus.FROZEN
                && user.getFrozenUntil() == null
                && !hasText(user.getFrozenReason())) {
            return toVO(user);
        }
        if (user.getRole() == UserRole.ADMIN
                && user.getStatus() == UserStatus.NORMAL
                && newStatus != UserStatus.NORMAL) {
            ensureAnotherAvailableAdmin(userId);
        }

        String normalizedReason = hasText(frozenReason) ? frozenReason.strip() : null;
        LocalDateTime normalizedUntil = null;
        if (newStatus == UserStatus.FROZEN) {
            if (normalizedReason == null) {
                throw new BizException(BizCode.ADMIN_USER_FROZEN_REASON_REQUIRED);
            }
            // 现有 AuthService 会在 frozenUntil 为空时立即解冻，因此这里必须要求截止时间。
            if (frozenUntil == null || !frozenUntil.isAfter(LocalDateTime.now())) {
                throw new BizException(BizCode.ADMIN_USER_FROZEN_UNTIL_INVALID);
            }
            normalizedUntil = frozenUntil;
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userMapper.updateAdminStatusAndIncreaseTokenVersion(
                userId,
                newStatus.getValue(),
                normalizedUntil,
                newStatus == UserStatus.FROZEN ? normalizedReason : null,
                now
        );
        if (updated != 1) throw new BizException(BizCode.ADMIN_USER_NOT_FOUND);

        invalidateLoginCredentials(userId);
        user.setStatus(newStatus);
        user.setFrozenUntil(normalizedUntil);
        user.setFrozenReason(newStatus == UserStatus.FROZEN ? normalizedReason : null);
        user.setUpdatedAt(now);
        return toVO(user);
    }

    @Transactional
    public AdminUserVO updatePermissions(Long userId, AdminUpdateUserPermissionRequest request) {
        User user = requireUser(userId);
        if (request.allowUpload() == null && request.allowDownload() == null) {
            throw new BizException(BizCode.ADMIN_USER_PERMISSION_UPDATE_EMPTY);
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.allowUpload() != null) {
            user.setAllowUpload(request.allowUpload());
        }
        if (request.allowDownload() != null) {
            user.setAllowDownload(request.allowDownload());
        }
        if (userMapper.updatePermissionsPartial(userId, request, now) != 1) {
            throw new BizException(BizCode.ADMIN_USER_NOT_FOUND);
        }

        user.setUpdatedAt(now);
        return toVO(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BizException(BizCode.ADMIN_USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureAnotherAvailableAdmin(Long excludedUserId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.ADMIN)
                .eq(User::getStatus, UserStatus.NORMAL)
                .eq(User::getDeleted, false)
                .ne(User::getId, excludedUserId));
        if (count == null || count == 0) {
            throw new BizException(BizCode.ADMIN_USER_LAST_ADMIN_FORBIDDEN);
        }
    }

    private void invalidateLoginCredentials(Long userId) {
        tokenRedisOperator.deleteAllRefreshTokens(userId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            tokenRedisOperator.deleteTokenVersionCache(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tokenRedisOperator.deleteTokenVersionCache(userId);
            }
        });
    }

    private AdminUserVO toVO(User user) {
        String avatarUrl = hasText(user.getAvatarKey())
                ? fileStorage.getUrl(user.getAvatarKey())
                : null;
        return new AdminUserVO(
                user.getId(), user.getPhone(), user.getCasId(), user.getName(), user.getNickname(),
                avatarUrl, user.getBio(), user.getRole().getValue(), user.getStatus().getValue(),
                user.getFrozenUntil(), user.getFrozenReason(),
                Boolean.TRUE.equals(user.getAllowUpload()),
                Boolean.TRUE.equals(user.getAllowDownload()),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
