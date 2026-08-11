package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.UpdateSubmissionReviewSettingRequest;
import cn.sduonline.business.data.po.SubmissionReviewSetting;
import cn.sduonline.business.data.vo.SubmissionReviewSettingVO;
import cn.sduonline.business.mapper.SubmissionReviewSettingMapper;
import cn.sduonline.business.security.context.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionReviewSettingService {
    private static final int SINGLETON_ID = 1;

    private final SubmissionReviewSettingMapper settingMapper;

    /**
     * 配置缺失时按“开启审核”处理，避免因错误初始化而直接发布用户投稿。
     */
    public boolean isReviewEnabled() {
        SubmissionReviewSetting setting = settingMapper.selectById(SINGLETON_ID);
        return setting == null || !Boolean.FALSE.equals(setting.getReviewEnabled());
    }

    @Transactional
    public SubmissionReviewSettingVO update(
            UpdateSubmissionReviewSettingRequest request
    ) {
        Long operatorId = CurrentUser.id();

        SubmissionReviewSetting setting = settingMapper.selectById(SINGLETON_ID);
        if (setting == null) {
            setting = SubmissionReviewSetting.builder()
                    .id(SINGLETON_ID)
                    .reviewEnabled(request.reviewEnabled())
                    .updatedBy(operatorId)
                    .updatedAt(LocalDateTime.now())
                    .build();
            settingMapper.insert(setting);
        } else {
            setting.setReviewEnabled(request.reviewEnabled());
            setting.setUpdatedBy(operatorId);
            setting.setUpdatedAt(LocalDateTime.now());
            settingMapper.updateById(setting);
        }

        return toVO(setting);
    }

    private SubmissionReviewSettingVO toVO(SubmissionReviewSetting setting) {
        return new SubmissionReviewSettingVO(
                !Boolean.FALSE.equals(setting.getReviewEnabled()),
                setting.getUpdatedBy(),
                setting.getUpdatedAt()
        );
    }
}