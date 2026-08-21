package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateAnnouncementRequest;
import cn.sduonline.business.data.dto.UpdateAnnouncementRequest;
import cn.sduonline.business.data.enums.AnnouncementStatus;
import cn.sduonline.business.data.po.Announcement;
import cn.sduonline.business.data.vo.AdminAnnouncementVO;
import cn.sduonline.business.data.vo.AnnouncementDetailVO;
import cn.sduonline.business.data.vo.AnnouncementSummaryVO;
import cn.sduonline.business.mapper.AnnouncementMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final long MAX_PAGE = 10000;
    private static final long MAX_PAGE_SIZE = 50;

    private final AnnouncementMapper announcementMapper;

    public PageResult<AnnouncementSummaryVO> listPublished(long page, long size) {
        long safePage = Math.clamp(page, 1, MAX_PAGE);
        long safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long total = announcementMapper.countPublished();
        if (total == 0) {
            return new PageResult<>(0, safePage, safeSize, List.of());
        }

        long offset = (safePage - 1) * safeSize;
        List<AnnouncementSummaryVO> items = announcementMapper
                .selectPublishedPage(offset, safeSize)
                .stream()
                .map(this::toSummary)
                .toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    public AnnouncementDetailVO publishedDetail(Long announcementId) {
        LocalDateTime now = LocalDateTime.now();
        Announcement announcement = announcementMapper.selectOne(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getId, announcementId)
                        .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED)
                        .isNotNull(Announcement::getPublishedAt)
                        .le(Announcement::getPublishedAt, now)
        );
        if (announcement == null) {
            throw new BizException(BizCode.ANNOUNCEMENT_NOT_FOUND);
        }
        return toDetail(announcement);
    }

    public PageResult<AdminAnnouncementVO> adminList(
            AnnouncementStatus status,
            String keyword,
            long page,
            long size
    ) {
        long safePage = Math.clamp(page, 1, MAX_PAGE);
        long safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        String normalizedKeyword = normalizeOptionalText(keyword);
        Integer statusValue = status == null ? null : status.getValue();
        long total = announcementMapper.countAdmin(statusValue, normalizedKeyword);
        if (total == 0) {
            return new PageResult<>(0, safePage, safeSize, List.of());
        }

        long offset = (safePage - 1) * safeSize;
        List<AdminAnnouncementVO> items = announcementMapper
                .selectAdminPage(statusValue, normalizedKeyword, offset, safeSize)
                .stream()
                .map(this::toAdminVO)
                .toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    @Transactional
    public AdminAnnouncementVO create(CreateAnnouncementRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = CurrentUser.id();
        Announcement announcement = new Announcement();
        announcement.setTitle(requireNonBlank(request.title(), "公告标题不能为空"));
        announcement.setSummary(normalizeOptionalText(request.summary()));
        announcement.setContent(requireNonBlankContent(request.content()));
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setIsPinned(Boolean.TRUE.equals(request.isPinned()));
        announcement.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        announcement.setPublishedAt(null);
        announcement.setCreatedBy(operatorId);
        announcement.setUpdatedBy(operatorId);
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);
        announcementMapper.insert(announcement);
        return toAdminVO(announcement);
    }

    @Transactional
    public AdminAnnouncementVO update(
            Long announcementId,
            UpdateAnnouncementRequest request
    ) {
        if (isEmptyUpdate(request)) {
            throw new BizException(BizCode.ANNOUNCEMENT_UPDATE_EMPTY);
        }

        Announcement announcement = requireAnnouncement(announcementId);
        if (request.title() != null) {
            announcement.setTitle(requireNonBlank(request.title(), "公告标题不能为空"));
        }
        if (request.summary() != null) {
            announcement.setSummary(normalizeOptionalText(request.summary()));
        }
        if (request.content() != null) {
            announcement.setContent(requireNonBlankContent(request.content()));
        }
        if (request.isPinned() != null) {
            announcement.setIsPinned(request.isPinned());
        }
        if (request.sortOrder() != null) {
            announcement.setSortOrder(request.sortOrder());
        }
        announcement.setUpdatedBy(CurrentUser.id());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        return toAdminVO(announcement);
    }

    @Transactional
    public AdminAnnouncementVO changeStatus(
            Long announcementId,
            AnnouncementStatus targetStatus
    ) {
        if (targetStatus != AnnouncementStatus.PUBLISHED
                && targetStatus != AnnouncementStatus.OFFLINE) {
            throw new BizException(
                    BizCode.ANNOUNCEMENT_STATUS_INVALID,
                    "状态接口只允许发布或下线公告"
            );
        }

        Announcement announcement = requireAnnouncement(announcementId);
        AnnouncementStatus currentStatus = announcement.getStatus();
        if (currentStatus == targetStatus) {
            return toAdminVO(announcement);
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetStatus == AnnouncementStatus.PUBLISHED) {
            if (currentStatus != AnnouncementStatus.DRAFT
                    && currentStatus != AnnouncementStatus.OFFLINE) {
                throw new BizException(BizCode.ANNOUNCEMENT_STATUS_INVALID);
            }
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
            announcement.setPublishedAt(now);
        } else {
            if (currentStatus != AnnouncementStatus.PUBLISHED) {
                throw new BizException(
                        BizCode.ANNOUNCEMENT_STATUS_INVALID,
                        "只有已发布公告可以下线"
                );
            }
            announcement.setStatus(AnnouncementStatus.OFFLINE);
        }

        announcement.setUpdatedBy(CurrentUser.id());
        announcement.setUpdatedAt(now);
        announcementMapper.updateById(announcement);
        return toAdminVO(announcement);
    }

    private Announcement requireAnnouncement(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BizException(BizCode.ANNOUNCEMENT_NOT_FOUND);
        }
        return announcement;
    }

    private boolean isEmptyUpdate(UpdateAnnouncementRequest request) {
        return request.title() == null
                && request.summary() == null
                && request.content() == null
                && request.isPinned() == null
                && request.sortOrder() == null;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(BizCode.BAD_REQUEST, message);
        }
        return value.strip();
    }

    private String requireNonBlankContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BizException(BizCode.BAD_REQUEST, "公告正文不能为空");
        }
        return content;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private AnnouncementSummaryVO toSummary(Announcement announcement) {
        return new AnnouncementSummaryVO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getSummary(),
                announcement.getIsPinned(),
                announcement.getPublishedAt()
        );
    }

    private AnnouncementDetailVO toDetail(Announcement announcement) {
        return new AnnouncementDetailVO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getSummary(),
                announcement.getContent(),
                announcement.getIsPinned(),
                announcement.getPublishedAt(),
                announcement.getUpdatedAt()
        );
    }

    private AdminAnnouncementVO toAdminVO(Announcement announcement) {
        return new AdminAnnouncementVO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getSummary(),
                announcement.getContent(),
                announcement.getStatus(),
                announcement.getIsPinned(),
                announcement.getSortOrder(),
                announcement.getPublishedAt(),
                announcement.getCreatedBy(),
                announcement.getUpdatedBy(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
