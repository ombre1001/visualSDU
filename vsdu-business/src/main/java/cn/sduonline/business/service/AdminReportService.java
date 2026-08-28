package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminReportDecisionActionRequest;
import cn.sduonline.business.data.dto.AdminReportDecisionRequest;
import cn.sduonline.business.data.enums.*;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.po.ReportOperationLog;
import cn.sduonline.business.data.projection.AdminReportDetailRow;
import cn.sduonline.business.data.projection.AdminReportOperationLogRow;
import cn.sduonline.business.data.projection.AdminReportSummaryRow;
import cn.sduonline.business.data.vo.*;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.ReportMapper;
import cn.sduonline.business.mapper.ReportOperationLogMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private static final int HIDDEN_MEDIA = 0;
    private static final int VISIBLE_MEDIA = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DETAIL_LOGS = 100;

    private final ReportMapper reportMapper;
    private final ReportOperationLogMapper operationLogMapper;
    private final MediaMapper mediaMapper;
    private final AdminMediaService adminMediaService;
    private final AdminUserService adminUserService;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;

    public PageResult<AdminReportSummaryVO> list(
            ReportStatus status,
            ReportTargetType targetType,
            String reasonType,
            Long reporterId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            long page,
            long size
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BizException(BizCode.BAD_REQUEST, "举报开始时间不能晚于结束时间");
        }
        Integer statusValue = status == null ? null : status.getValue();
        String targetTypeValue = targetType == null ? null : targetType.name();
        String reasonCode = normalizeCode(reasonType);
        long safePage = Math.max(1, page);
        long safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long total = reportMapper.countAdminReports(
                statusValue, targetTypeValue, reasonCode, reporterId, createdFrom, createdTo
        );
        long offset = (safePage - 1) * safeSize;
        List<AdminReportSummaryVO> items = total == 0
                ? List.of()
                : reportMapper.selectAdminReportPage(
                        statusValue, targetTypeValue, reasonCode, reporterId,
                        createdFrom, createdTo, offset, safeSize
                ).stream().map(this::toSummaryVO).toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    public AdminReportDetailVO detail(Long reportId) {
        AdminReportDetailRow row = reportMapper.selectAdminReportDetail(reportId);
        if (row == null) throw new BizException(BizCode.REPORT_NOT_FOUND);

        List<AdminReportOperationLogVO> logs = operationLogMapper
                .selectByReport(reportId, MAX_DETAIL_LOGS)
                .stream()
                .map(this::toOperationLogVO)
                .toList();

        AdminReportReporterVO reporter = new AdminReportReporterVO(
                row.getReporterId(), row.getReporterCasId(), row.getReporterName(),
                row.getReporterNickname(), url(row.getReporterAvatarKey()),
                Objects.requireNonNullElse(row.getReporterReportCount(), 0L),
                Objects.requireNonNullElse(row.getReporterConfirmedCount(), 0L)
        );
        AdminReportTargetVO target = new AdminReportTargetVO(
                ReportTargetType.valueOf(row.getTargetType()), row.getTargetId(),
                Boolean.TRUE.equals(row.getTargetExists()), row.getTargetTitle(),
                row.getTargetDescription(), row.getTargetUploaderId(), row.getTargetStatus(),
                url(row.getTargetThumbnailKey())
        );
        return new AdminReportDetailVO(
                row.getId(), reporter, target, row.getReasonCode(), row.getReasonName(),
                row.getReasonDescription(), row.getDescription(), ReportStatus.valueOf(row.getStatus()),
                row.getDecisionReason(), row.getProcessedBy(), row.getProcessorName(), row.getProcessedAt(),
                Objects.requireNonNullElse(row.getRelatedActiveReportCount(), 0L),
                row.getCreatedAt(), row.getUpdatedAt(), row.getVersion(), logs
        );
    }

    @Transactional
    public AdminReportDecisionResultVO decide(
            Long operatorId,
            Long reportId,
            AdminReportDecisionRequest request
    ) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BizException(BizCode.REPORT_NOT_FOUND);

        ReportStatus beforeStatus = ReportStatus.valueOf(report.getStatus());
        if (!beforeStatus.isActive()) throw new BizException(BizCode.REPORT_STATUS_INVALID);
        if (!Objects.equals(report.getVersion(), request.expectedVersion())) {
            throw new BizException(BizCode.REPORT_VERSION_CONFLICT);
        }

        String reason = normalize(request.reason());
        List<AdminReportDecisionActionRequest> actions = normalizeActions(request.actions());
        validateDecision(request.decision(), reason, actions);
        Media targetMedia = requiresTarget(actions) ? lockTargetMedia(report) : null;

        LocalDateTime processedAt = LocalDateTime.now();
        ReportStatus afterStatus = request.decision().getTargetStatus();
        int updated = reportMapper.updateDecisionWithVersion(
                reportId, request.expectedVersion(), afterStatus.getValue(), reason,
                operatorId, processedAt
        );
        if (updated != 1) throw new BizException(BizCode.REPORT_VERSION_CONFLICT);

        List<ReportActionResultVO> actionResults = executeActions(
                operatorId, report, targetMedia, actions
        );
        ReportOperationLog log = new ReportOperationLog();
        log.setReportId(reportId);
        log.setOperatorId(operatorId);
        log.setOperationType("DECISION");
        log.setDecision(request.decision().getValue());
        log.setBeforeStatus(beforeStatus.getValue());
        log.setAfterStatus(afterStatus.getValue());
        log.setReason(reason);
        log.setActionsJson(toJson(actions));
        log.setResultJson(toJson(actionResults));
        log.setReportVersion(request.expectedVersion());
        log.setCreatedAt(processedAt);
        operationLogMapper.insert(log);

        return new AdminReportDecisionResultVO(
                reportId, afterStatus, reason, operatorId, processedAt,
                request.expectedVersion() + 1, actionResults
        );
    }

    private void validateDecision(
            ReportDecision decision,
            String reason,
            List<AdminReportDecisionActionRequest> actions
    ) {
        if (decision == ReportDecision.CONFIRM && reason == null) {
            throw new BizException(BizCode.REPORT_DECISION_REASON_REQUIRED);
        }
        if (decision == ReportDecision.CONFIRM && actions.isEmpty()) {
            throw new BizException(BizCode.REPORT_ACTION_REQUIRED);
        }

        EnumSet<ReportActionType> types = EnumSet.noneOf(ReportActionType.class);
        for (AdminReportDecisionActionRequest action : actions) {
            if (!types.add(action.type())) {
                throw new BizException(BizCode.REPORT_ACTION_INVALID, "举报处置动作不能重复");
            }
        }
        if (types.contains(ReportActionType.NO_ACTION) && types.size() > 1) {
            throw new BizException(BizCode.REPORT_ACTION_INVALID, "NO_ACTION不能与其他处置动作同时使用");
        }
        if (types.contains(ReportActionType.HIDE_MEDIA)
                && types.contains(ReportActionType.RESTORE_MEDIA)) {
            throw new BizException(BizCode.REPORT_ACTION_INVALID, "不能同时隐藏和恢复同一媒体");
        }
        if (decision != ReportDecision.CONFIRM
                && types.stream().anyMatch(type -> type != ReportActionType.NO_ACTION)) {
            throw new BizException(BizCode.REPORT_ACTION_INVALID, "举报不成立或关闭时不能执行资源处置");
        }
        for (AdminReportDecisionActionRequest action : actions) {
            if (action.type() == ReportActionType.FREEZE_USER) {
                if (action.frozenUntil() == null
                        || !action.frozenUntil().isAfter(LocalDateTime.now())
                        || normalize(action.reason()) == null) {
                    throw new BizException(
                            BizCode.REPORT_ACTION_INVALID,
                            "冻结用户时必须提供未来的冻结截止时间和原因"
                    );
                }
            }
        }
    }

    private Media lockTargetMedia(Report report) {
        if (!ReportTargetType.MEDIA.name().equals(report.getTargetType())) {
            throw new BizException(BizCode.REPORT_ACTION_INVALID, "当前举报目标不支持该处置动作");
        }
        Media media = mediaMapper.selectByIdForUpdate(report.getTargetId());
        if (media == null) throw new BizException(BizCode.REPORT_TARGET_NOT_FOUND);
        return media;
    }

    private List<ReportActionResultVO> executeActions(
            Long operatorId,
            Report report,
            Media targetMedia,
            List<AdminReportDecisionActionRequest> actions
    ) {
        List<ReportActionResultVO> results = new ArrayList<>(actions.size());
        for (AdminReportDecisionActionRequest action : actions) {
            switch (action.type()) {
                case NO_ACTION -> results.add(new ReportActionResultVO(
                        action.type(), report.getTargetId(), "仅记录处理结论"
                ));
                case HIDE_MEDIA -> {
                    if (Objects.equals(targetMedia.getStatus(), HIDDEN_MEDIA)) {
                        results.add(new ReportActionResultVO(
                                action.type(), targetMedia.getId(), "媒体已处于隐藏状态"
                        ));
                    } else {
                        adminMediaService.hide(targetMedia.getId());
                        targetMedia.setStatus(HIDDEN_MEDIA);
                        results.add(new ReportActionResultVO(
                                action.type(), targetMedia.getId(), "媒体已隐藏"
                        ));
                    }
                }
                case RESTORE_MEDIA -> {
                    if (Objects.equals(targetMedia.getStatus(), VISIBLE_MEDIA)) {
                        results.add(new ReportActionResultVO(
                                action.type(), targetMedia.getId(), "媒体已处于可见状态"
                        ));
                    } else {
                        adminMediaService.restore(targetMedia.getId());
                        targetMedia.setStatus(VISIBLE_MEDIA);
                        results.add(new ReportActionResultVO(
                                action.type(), targetMedia.getId(), "媒体已恢复"
                        ));
                    }
                }
                case FREEZE_USER -> {
                    if (targetMedia.getUploaderId() == null) {
                        throw new BizException(BizCode.REPORT_ACTION_INVALID, "该媒体没有可冻结的上传用户");
                    }
                    adminUserService.updateStatus(
                            operatorId, targetMedia.getUploaderId(), UserStatus.FROZEN,
                            action.frozenUntil(), normalize(action.reason())
                    );
                    results.add(new ReportActionResultVO(
                            action.type(), targetMedia.getUploaderId(), "媒体上传用户已冻结"
                    ));
                }
            }
        }
        return List.copyOf(results);
    }

    private AdminReportSummaryVO toSummaryVO(AdminReportSummaryRow row) {
        return new AdminReportSummaryVO(
                row.getId(), row.getReporterId(), row.getReporterName(),
                ReportTargetType.valueOf(row.getTargetType()), row.getTargetId(),
                row.getTargetTitle(), url(row.getTargetThumbnailKey()),
                row.getReasonCode(), row.getReasonName(), row.getDescription(),
                ReportStatus.valueOf(row.getStatus()), row.getProcessedBy(),
                row.getProcessorName(), row.getProcessedAt(), row.getCreatedAt(), row.getVersion()
        );
    }

    private AdminReportOperationLogVO toOperationLogVO(AdminReportOperationLogRow row) {
        return new AdminReportOperationLogVO(
                row.getId(), row.getOperationType(),
                row.getDecision() == null ? null : ReportDecision.valueOf(row.getDecision()),
                ReportStatus.valueOf(row.getBeforeStatus()), ReportStatus.valueOf(row.getAfterStatus()),
                row.getReason(), parseJson(row.getActionsJson()), parseJson(row.getResultJson()),
                row.getOperatorId(), row.getOperatorName(), row.getReportVersion(), row.getCreatedAt()
        );
    }

    private List<AdminReportDecisionActionRequest> normalizeActions(
            List<AdminReportDecisionActionRequest> actions
    ) {
        return actions == null ? List.of() : List.copyOf(actions);
    }

    private boolean requiresTarget(List<AdminReportDecisionActionRequest> actions) {
        return actions.stream().anyMatch(action -> action.type() != ReportActionType.NO_ACTION);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        return objectMapper.valueToTree(value).toString();
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new BizException(BizCode.INTERNAL_SERVER_ERROR, "举报操作日志JSON无法解析");
        }
    }

    private String url(String objectKey) {
        return objectKey == null || objectKey.isBlank() ? null : fileStorage.getUrl(objectKey);
    }
}
