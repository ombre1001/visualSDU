package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateMediaReportRequest;
import cn.sduonline.business.data.dto.CreateUserReportRequest;
import cn.sduonline.business.data.enums.ReportTargetType;
import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.po.ReportReasonType;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.ReportReasonTypeVO;
import cn.sduonline.business.data.vo.ReportVO;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.ReportMapper;
import cn.sduonline.business.mapper.ReportReasonTypeMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final int VISIBLE_MEDIA = 1;
    private static final int MAX_REPORTS_PER_HOUR = 10;

    private final ReportMapper reportMapper;
    private final ReportReasonTypeMapper reasonTypeMapper;
    private final MediaMapper mediaMapper;
    private final UserMapper userMapper;

    public List<ReportReasonTypeVO> reasons() {
        return reasonTypeMapper.selectEnabled().stream()
                .map(reason -> new ReportReasonTypeVO(
                        reason.getCode(), reason.getName(), reason.getDescription()
                ))
                .toList();
    }

    @Transactional
    public ReportVO createMediaReport(
            Long reporterId,
            CreateMediaReportRequest request,
            String submitIp
    ) {
        checkRateLimit(reporterId);
        Media media = mediaMapper.selectByIdForUpdate(request.mediaId());
        if (media == null || !Integer.valueOf(VISIBLE_MEDIA).equals(media.getStatus())) {
            throw new BizException(BizCode.REPORT_TARGET_NOT_FOUND);
        }
        return create(
                reporterId, ReportTargetType.MEDIA, request.mediaId(),
                request.reasonType(), request.description(), submitIp
        );
    }

    @Transactional
    public ReportVO createUserReport(
            Long reporterId,
            CreateUserReportRequest request,
            String submitIp
    ) {
        checkRateLimit(reporterId);
        if (reporterId.equals(request.userId())) {
            throw new BizException(BizCode.REPORT_SELF_TARGET_FORBIDDEN);
        }
        User targetUser = userMapper.selectByIdForUpdate(request.userId());
        if (targetUser == null) {
            throw new BizException(BizCode.REPORT_TARGET_NOT_FOUND);
        }
        return create(
                reporterId, ReportTargetType.USER, request.userId(),
                request.reasonType(), request.description(), submitIp
        );
    }

    private ReportVO create(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            String requestedReasonType,
            String description,
            String submitIp
    ) {
        LocalDateTime now = LocalDateTime.now();

        String reasonCode = requestedReasonType.strip().toUpperCase(Locale.ROOT);
        ReportReasonType reason = reasonTypeMapper.selectEnabledByCode(reasonCode);
        if (reason == null) {
            throw new BizException(BizCode.REPORT_REASON_INVALID);
        }
        if (reportMapper.existsActiveByReporterTarget(reporterId, targetType.name(), targetId)) {
            throw new BizException(BizCode.REPORT_DUPLICATE_ACTIVE);
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType.name());
        report.setTargetId(targetId);
        report.setReasonCode(reasonCode);
        report.setDescription(normalize(description));
        report.setStatus(ReportStatus.PENDING.getValue());
        report.setVersion(0);
        report.setSubmitIp(normalizeIp(submitIp));
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        try {
            reportMapper.insert(report);
        } catch (DuplicateKeyException exception) {
            throw new BizException(BizCode.REPORT_DUPLICATE_ACTIVE);
        }

        return new ReportVO(
                report.getId(), targetType, targetId,
                reasonCode, reason.getName(), report.getDescription(),
                ReportStatus.PENDING, now, 0
        );
    }

    private void checkRateLimit(Long reporterId) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        if (reportMapper.countSubmittedSince(reporterId, since) >= MAX_REPORTS_PER_HOUR) {
            throw new BizException(BizCode.REPORT_RATE_LIMIT_EXCEEDED);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeIp(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() <= 45) return normalized;
        return normalized.substring(0, 45);
    }
}
