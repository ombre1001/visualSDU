package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.UpdateSubmissionReviewSettingRequest;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.SubmissionReviewSetting;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.SubmissionReviewSettingVO;
import cn.sduonline.business.mapper.SubmissionReviewSettingMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionReviewSettingService {
    private static final int SINGLETON_ID = 1;

    private final SubmissionReviewSettingMapper settingMapper;
    private final UserMapper userMapper;

    /**
     * 配置缺失时按“开启审核”处理，避免因错误初始化而直接发布用户投稿。
     */
    public boolean isReviewEnabled() {
        SubmissionReviewSetting setting = settingMapper.selectById(SINGLETON_ID);
        return setting == null || !Boolean.FALSE.equals(setting.getReviewEnabled());
    }

    @Transactional
    public SubmissionReviewSettingVO update(
            Long operatorId,
            UserRole tokenRole,
            UpdateSubmissionReviewSettingRequest request
    ) {
        requireAdmin(operatorId, tokenRole);

        SubmissionReviewSetting setting = settingMapper.selectById(SINGLETON_ID);
        if (setting == null) {
            setting = new SubmissionReviewSetting();
            setting.setId(SINGLETON_ID);
            setting.setReviewEnabled(request.reviewEnabled());
            setting.setUpdatedBy(operatorId);
            setting.setUpdatedAt(LocalDateTime.now());
            settingMapper.insert(setting);
        } else {
            setting.setReviewEnabled(request.reviewEnabled());
            setting.setUpdatedBy(operatorId);
            setting.setUpdatedAt(LocalDateTime.now());
            settingMapper.updateById(setting);
        }

        return toVO(setting);
    }

    private void requireAdmin(Long operatorId, UserRole tokenRole) {
        if (tokenRole != UserRole.ADMIN) {
            throw new BizException(BizCode.ADMIN_REQUIRED);
        }

        User user = userMapper.selectById(operatorId);
        if (user == null
                || Boolean.TRUE.equals(user.getDeleted())
                || user.getRole() != UserRole.ADMIN
                || user.getStatus() != UserStatus.NORMAL) {
            throw new BizException(BizCode.ADMIN_REQUIRED);
        }
    }

    private SubmissionReviewSettingVO toVO(SubmissionReviewSetting setting) {
        return new SubmissionReviewSettingVO(
                !Boolean.FALSE.equals(setting.getReviewEnabled()),
                setting.getUpdatedBy(),
                setting.getUpdatedAt()
        );
    }
}