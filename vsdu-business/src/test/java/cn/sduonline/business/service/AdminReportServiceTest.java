package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminReportDecisionActionRequest;
import cn.sduonline.business.data.dto.AdminReportDecisionRequest;
import cn.sduonline.business.data.enums.ReportActionType;
import cn.sduonline.business.data.enums.ReportDecision;
import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.po.ReportOperationLog;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.ReportMapper;
import cn.sduonline.business.mapper.ReportOperationLogMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReportOperationLogMapper operationLogMapper;
    @Mock
    private MediaMapper mediaMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AdminMediaService adminMediaService;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private FileStorage fileStorage;

    private AdminReportService service;

    @BeforeEach
    void setUp() {
        service = new AdminReportService(
                reportMapper, operationLogMapper, mediaMapper, userMapper,
                adminMediaService, adminUserService, fileStorage, new ObjectMapper()
        );
    }

    @Test
    void confirmWithNoActionShouldUpdateVersionAndWriteLog() {
        when(reportMapper.selectById(10L)).thenReturn(pendingReport(10L, 2));
        when(reportMapper.updateDecisionWithVersion(
                eq(10L), eq(2), eq(ReportStatus.CONFIRMED.getValue()),
                eq("确认侵权"), eq(7L), any()
        )).thenReturn(1);

        var result = service.decide(
                7L,
                10L,
                new AdminReportDecisionRequest(
                        ReportDecision.CONFIRM,
                        "确认侵权",
                        List.of(new AdminReportDecisionActionRequest(
                                ReportActionType.NO_ACTION, null, null
                        )),
                        2
                )
        );

        assertThat(result.status()).isEqualTo(ReportStatus.CONFIRMED);
        assertThat(result.version()).isEqualTo(3);
        assertThat(result.actions()).singleElement().satisfies(action ->
                assertThat(action.type()).isEqualTo(ReportActionType.NO_ACTION)
        );
        verifyNoInteractions(mediaMapper, adminMediaService, adminUserService);

        ArgumentCaptor<ReportOperationLog> captor = ArgumentCaptor.forClass(ReportOperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getReportVersion()).isEqualTo(2);
        assertThat(captor.getValue().getBeforeStatus()).isEqualTo(ReportStatus.PENDING.getValue());
        assertThat(captor.getValue().getAfterStatus()).isEqualTo(ReportStatus.CONFIRMED.getValue());
    }

    @Test
    void decideShouldRejectStaleVersionBeforeUpdate() {
        when(reportMapper.selectById(10L)).thenReturn(pendingReport(10L, 3));

        assertThatThrownBy(() -> service.decide(
                7L,
                10L,
                new AdminReportDecisionRequest(
                        ReportDecision.CLOSE, null, List.of(), 2
                )
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode()).isEqualTo(BizCode.REPORT_VERSION_CONFLICT)
        );

        verify(reportMapper, never()).updateDecisionWithVersion(
                anyLong(), anyInt(), anyInt(), any(), anyLong(), any()
        );
        verifyNoInteractions(operationLogMapper);
    }

    @Test
    void confirmShouldRequireReasonAndAction() {
        when(reportMapper.selectById(10L)).thenReturn(pendingReport(10L, 2));

        assertThatThrownBy(() -> service.decide(
                7L,
                10L,
                new AdminReportDecisionRequest(ReportDecision.CONFIRM, " ", List.of(), 2)
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode()).isEqualTo(BizCode.REPORT_DECISION_REASON_REQUIRED)
        );

        verify(reportMapper, never()).updateDecisionWithVersion(
                anyLong(), anyInt(), anyInt(), any(), anyLong(), any()
        );
    }

    @Test
    void confirmUserReportShouldFreezeTargetUser() {
        Report report = pendingReport(10L, 2);
        report.setTargetType("USER");
        User targetUser = new User();
        targetUser.setId(20L);
        LocalDateTime frozenUntil = LocalDateTime.now().plusDays(1);

        when(reportMapper.selectById(10L)).thenReturn(report);
        when(userMapper.selectByIdForUpdate(20L)).thenReturn(targetUser);
        when(reportMapper.updateDecisionWithVersion(
                eq(10L), eq(2), eq(ReportStatus.CONFIRMED.getValue()),
                eq("确认违规"), eq(7L), any()
        )).thenReturn(1);

        var result = service.decide(
                7L,
                10L,
                new AdminReportDecisionRequest(
                        ReportDecision.CONFIRM,
                        "确认违规",
                        List.of(new AdminReportDecisionActionRequest(
                                ReportActionType.FREEZE_USER, frozenUntil, "发布违规内容"
                        )),
                        2
                )
        );

        assertThat(result.actions()).singleElement().satisfies(action -> {
            assertThat(action.type()).isEqualTo(ReportActionType.FREEZE_USER);
            assertThat(action.targetId()).isEqualTo(20L);
        });
        verify(adminUserService).updateStatus(
                7L, 20L, UserStatus.FROZEN, frozenUntil, "发布违规内容"
        );
        verifyNoInteractions(mediaMapper, adminMediaService);
    }

    private Report pendingReport(Long id, int version) {
        Report report = new Report();
        report.setId(id);
        report.setTargetType("MEDIA");
        report.setTargetId(20L);
        report.setStatus(ReportStatus.PENDING.getValue());
        report.setVersion(version);
        return report;
    }
}
