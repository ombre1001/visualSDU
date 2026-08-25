package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminReviewSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.SubmissionReviewLog;
import cn.sduonline.business.mapper.SubmissionMapper;
import cn.sduonline.business.mapper.SubmissionReviewLogMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSubmissionReviewExecutorTest {
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private SubmissionReviewLogMapper reviewLogMapper;
    @Mock
    private SubmissionPublicationService publicationService;
    @InjectMocks
    private AdminSubmissionReviewExecutor executor;

    @Test
    void approveShouldAcquireVersionPublishAssetsAndWriteLog() {
        Submission submission = pendingSubmission(10L, 3);
        when(reviewLogMapper.selectBySubmissionAndVersion(10L, 3)).thenReturn(null);
        when(submissionMapper.selectById(10L)).thenReturn(submission);
        when(submissionMapper.updateReviewWithVersion(
                eq(10L), eq(3), eq(SubmissionStatus.PENDING.getValue()),
                eq(SubmissionStatus.APPROVED.getValue()), isNull(), eq(7L), any(LocalDateTime.class)
        )).thenReturn(1);
        when(reviewLogMapper.selectNextRoundNo(10L)).thenReturn(1);

        var result = executor.review(
                7L,
                10L,
                new AdminReviewSubmissionRequest(SubmissionReviewDecision.APPROVE, null, 3)
        );

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.version()).isEqualTo(4);
        assertThat(result.reviewedBy()).isEqualTo(7L);
        verify(publicationService).publishAssets(submission);

        ArgumentCaptor<SubmissionReviewLog> captor = ArgumentCaptor.forClass(SubmissionReviewLog.class);
        verify(reviewLogMapper).insert(captor.capture());
        SubmissionReviewLog log = captor.getValue();
        assertThat(log.getRoundNo()).isEqualTo(1);
        assertThat(log.getSubmissionVersion()).isEqualTo(3);
        assertThat(log.getDecision()).isEqualTo(SubmissionReviewDecision.APPROVE);
        assertThat(log.getBeforeStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(log.getAfterStatus()).isEqualTo(SubmissionStatus.APPROVED);
    }

    @Test
    void returnShouldRequireReasonBeforeAccessingDatabase() {
        assertThatThrownBy(() -> executor.review(
                7L,
                10L,
                new AdminReviewSubmissionRequest(SubmissionReviewDecision.RETURN, "  ", 3)
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode())
                        .isEqualTo(BizCode.ADMIN_SUBMISSION_REVIEW_REASON_REQUIRED)
        );

        verifyNoInteractions(submissionMapper, reviewLogMapper, publicationService);
    }

    @Test
    void reviewShouldRejectStaleVersion() {
        when(reviewLogMapper.selectBySubmissionAndVersion(10L, 3)).thenReturn(null);
        when(submissionMapper.selectById(10L)).thenReturn(pendingSubmission(10L, 4));

        assertThatThrownBy(() -> executor.review(
                7L,
                10L,
                new AdminReviewSubmissionRequest(SubmissionReviewDecision.APPROVE, null, 3)
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode())
                        .isEqualTo(BizCode.ADMIN_SUBMISSION_VERSION_CONFLICT)
        );

        verify(submissionMapper, never()).updateReviewWithVersion(
                anyLong(), anyInt(), anyInt(), anyInt(), any(), anyLong(), any()
        );
        verifyNoInteractions(publicationService);
    }

    @Test
    void repeatedSameReviewBySameAdminShouldBeIdempotent() {
        SubmissionReviewLog log = new SubmissionReviewLog();
        log.setSubmissionId(10L);
        log.setSubmissionVersion(3);
        log.setDecision(SubmissionReviewDecision.RETURN);
        log.setReason("地点不准确");
        log.setAfterStatus(SubmissionStatus.RETURNED);
        log.setReviewedBy(7L);
        log.setReviewedAt(LocalDateTime.now());
        when(reviewLogMapper.selectBySubmissionAndVersion(10L, 3)).thenReturn(log);

        var result = executor.review(
                7L,
                10L,
                new AdminReviewSubmissionRequest(SubmissionReviewDecision.RETURN, "地点不准确", 3)
        );

        assertThat(result.status()).isEqualTo(SubmissionStatus.RETURNED);
        assertThat(result.version()).isEqualTo(4);
        verifyNoInteractions(submissionMapper, publicationService);
        verify(reviewLogMapper, never()).insert(any(SubmissionReviewLog.class));
    }

    private Submission pendingSubmission(Long id, int version) {
        return Submission.builder()
                .id(id)
                .userId(5L)
                .locationId(8L)
                .status(SubmissionStatus.PENDING)
                .version(version)
                .deleted(false)
                .build();
    }
}
