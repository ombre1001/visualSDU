package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminReviewSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.SubmissionReviewLog;
import cn.sduonline.business.data.vo.AdminSubmissionReviewResultVO;
import cn.sduonline.business.mapper.SubmissionMapper;
import cn.sduonline.business.mapper.SubmissionReviewLogMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminSubmissionReviewExecutor {
    private final SubmissionMapper submissionMapper;
    private final SubmissionReviewLogMapper reviewLogMapper;
    private final SubmissionPublicationService publicationService;

    @Transactional
    public AdminSubmissionReviewResultVO review(
            Long reviewerId,
            Long submissionId,
            AdminReviewSubmissionRequest request
    ) {
        String reason = normalizeReason(request.decision(), request.reason());

        SubmissionReviewLog existingLog = reviewLogMapper.selectBySubmissionAndVersion(
                submissionId,
                request.expectedVersion()
        );
        if (existingLog != null) {
            return existingReviewResult(reviewerId, request.decision(), existingLog);
        }

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_NOT_FOUND);
        }
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_NOT_PENDING);
        }
        if (!Objects.equals(submission.getVersion(), request.expectedVersion())) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_VERSION_CONFLICT);
        }

        SubmissionStatus afterStatus = afterStatus(request.decision());
        LocalDateTime reviewedAt = LocalDateTime.now();
        int updated = submissionMapper.updateReviewWithVersion(
                submissionId,
                request.expectedVersion(),
                SubmissionStatus.PENDING.getValue(),
                afterStatus.getValue(),
                reason,
                reviewerId,
                reviewedAt
        );
        if (updated != 1) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_VERSION_CONFLICT);
        }

        submission.setStatus(afterStatus);
        submission.setReviewReason(reason);
        submission.setReviewedBy(reviewerId);
        submission.setReviewedAt(reviewedAt);
        submission.setUpdatedAt(reviewedAt);
        submission.setVersion(request.expectedVersion() + 1);

        if (request.decision() == SubmissionReviewDecision.APPROVE) {
            publicationService.publishAssets(submission);
        }

        SubmissionReviewLog log = new SubmissionReviewLog();
        log.setSubmissionId(submissionId);
        log.setRoundNo(reviewLogMapper.selectNextRoundNo(submissionId));
        log.setSubmissionVersion(request.expectedVersion());
        log.setDecision(request.decision());
        log.setReason(reason);
        log.setBeforeStatus(SubmissionStatus.PENDING);
        log.setAfterStatus(afterStatus);
        log.setReviewedBy(reviewerId);
        log.setReviewedAt(reviewedAt);
        reviewLogMapper.insert(log);

        return toResult(submission);
    }

    private String normalizeReason(SubmissionReviewDecision decision, String reason) {
        String normalized = reason == null || reason.isBlank() ? null : reason.strip();
        if (decision != SubmissionReviewDecision.APPROVE && normalized == null) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_REVIEW_REASON_REQUIRED);
        }
        return normalized;
    }

    private SubmissionStatus afterStatus(SubmissionReviewDecision decision) {
        return switch (decision) {
            case APPROVE -> SubmissionStatus.APPROVED;
            case RETURN -> SubmissionStatus.RETURNED;
            case REJECT -> SubmissionStatus.REJECTED;
        };
    }

    private AdminSubmissionReviewResultVO existingReviewResult(
            Long reviewerId,
            SubmissionReviewDecision decision,
            SubmissionReviewLog log
    ) {
        if (!Objects.equals(log.getReviewedBy(), reviewerId) || log.getDecision() != decision) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_VERSION_CONFLICT);
        }
        return new AdminSubmissionReviewResultVO(
                log.getSubmissionId(),
                log.getAfterStatus(),
                log.getSubmissionVersion() + 1,
                log.getReason(),
                log.getReviewedBy(),
                log.getReviewedAt()
        );
    }

    private AdminSubmissionReviewResultVO toResult(Submission submission) {
        return new AdminSubmissionReviewResultVO(
                submission.getId(),
                submission.getStatus(),
                submission.getVersion(),
                submission.getReviewReason(),
                submission.getReviewedBy(),
                submission.getReviewedAt()
        );
    }
}
