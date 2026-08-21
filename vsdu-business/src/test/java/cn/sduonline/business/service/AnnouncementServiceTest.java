package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateAnnouncementRequest;
import cn.sduonline.business.data.enums.AnnouncementStatus;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.po.Announcement;
import cn.sduonline.business.mapper.AnnouncementMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementService announcementService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUser.removeDetail();
    }

    @Test
    void createShouldInitializeDraftAndAuditFields() {
        CurrentUser.setDetail(7L, UserRole.ADMIN);
        doAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(11L);
            return 1;
        }).when(announcementMapper).insert(any(Announcement.class));

        var result = announcementService.create(
                new CreateAnnouncementRequest(
                        " 测试公告 ",
                        " 公告摘要 ",
                        "公告正文",
                        null,
                        null
                )
        );

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.title()).isEqualTo("测试公告");
        assertThat(result.summary()).isEqualTo("公告摘要");
        assertThat(result.status()).isEqualTo(AnnouncementStatus.DRAFT);
        assertThat(result.isPinned()).isFalse();
        assertThat(result.sortOrder()).isZero();
        assertThat(result.publishedAt()).isNull();
        assertThat(result.createdBy()).isEqualTo(7L);
        assertThat(result.updatedBy()).isEqualTo(7L);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();
        verify(announcementMapper).insert(any(Announcement.class));
    }

    @Test
    void changeStatusShouldPublishDraft() {
        CurrentUser.setDetail(8L, UserRole.ADMIN);
        Announcement announcement = announcement(AnnouncementStatus.DRAFT);
        when(announcementMapper.selectById(1L)).thenReturn(announcement);

        var result = announcementService.changeStatus(
                1L,
                AnnouncementStatus.PUBLISHED
        );

        assertThat(result.status()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(result.publishedAt()).isNotNull();
        assertThat(result.updatedBy()).isEqualTo(8L);
        verify(announcementMapper).updateById(announcement);
    }

    @Test
    void changeStatusShouldRejectTakingDraftOffline() {
        CurrentUser.setDetail(8L, UserRole.ADMIN);
        when(announcementMapper.selectById(1L))
                .thenReturn(announcement(AnnouncementStatus.DRAFT));

        assertThatThrownBy(() -> announcementService.changeStatus(
                1L,
                AnnouncementStatus.OFFLINE
        ))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode())
                                .isEqualTo(BizCode.ANNOUNCEMENT_STATUS_INVALID)
                );
    }

    private Announcement announcement(AnnouncementStatus status) {
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("测试公告");
        announcement.setContent("公告正文");
        announcement.setStatus(status);
        announcement.setIsPinned(false);
        announcement.setSortOrder(0);
        announcement.setCreatedBy(7L);
        announcement.setUpdatedBy(7L);
        return announcement;
    }
}
