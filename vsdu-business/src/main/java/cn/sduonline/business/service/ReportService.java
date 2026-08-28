package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateReportRequest;
import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.po.ReportReasonType;
import cn.sduonline.business.data.vo.ReportReasonTypeVO;
import cn.sduonline.business.data.vo.ReportVO;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.ReportMapper;
import cn.sduonline.business.mapper.ReportReasonTypeMapper;
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

    public List<ReportReasonTypeVO> reasons() {
        return reasonTypeMapper.selectEnabled().stream()
                .map(reason -> new ReportReasonTypeVO(
                        reason.getCode(), reason.getName(), reason.getDescription()
                ))
                .toList();
    }

    @Transactional
    public ReportVO create(
            Long reporterId,
            CreateReportRequest request,
            String submitIp
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (reportMapper.countSubmittedSince(reporterId, now.minusHours(1)) >= MAX_REPORTS_PER_HOUR) {
            throw new BizException(BizCode.REPORT_RATE_LIMIT_EXCEEDED);
        }

        String targetType = request.targetType().name();
        Media media = mediaMapper.selectByIdForUpdate(request.targetId());
        if (media == null || !Integer.valueOf(VISIBLE_MEDIA).equals(media.getStatus())) {
            throw new BizException(BizCode.REPORT_TARGET_NOT_FOUND);
        }

        String reasonCode = request.reasonType().strip().toUpperCase(Locale.ROOT);
        ReportReasonType reason = reasonTypeMapper.selectEnabledByCode(reasonCode);
        if (reason == null) {
            throw new BizException(BizCode.REPORT_REASON_INVALID);
        }
        if (reportMapper.existsActiveByReporterTarget(reporterId, targetType, request.targetId())) {
            throw new BizException(BizCode.REPORT_DUPLICATE_ACTIVE);
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.targetId());
        report.setReasonCode(reasonCode);
        report.setDescription(normalize(request.description()));
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
                report.getId(), request.targetType(), request.targetId(),
                reasonCode, reason.getName(), report.getDescription(),
                ReportStatus.PENDING, now, 0
        );
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
