package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateMediaReportRequest;
import cn.sduonline.business.data.dto.CreateUserReportRequest;
import cn.sduonline.business.data.enums.ReportTargetType;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.po.ReportReasonType;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.ReportMapper;
import cn.sduonline.business.mapper.ReportReasonTypeMapper;
import cn.sduonline.business.mapper.UserMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReportReasonTypeMapper reasonTypeMapper;
    @Mock
    private MediaMapper mediaMapper;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private ReportService service;

    @Test
    void createShouldValidateAndPersistNormalizedReport() {
        Media media = new Media();
        media.setId(9L);
        media.setStatus(1);
        ReportReasonType reason = new ReportReasonType();
        reason.setCode("COPYRIGHT");
        reason.setName("侵犯版权");

        when(reportMapper.countSubmittedSince(eq(3L), any(LocalDateTime.class))).thenReturn(0L);
        when(mediaMapper.selectByIdForUpdate(9L)).thenReturn(media);
        when(reasonTypeMapper.selectEnabledByCode("COPYRIGHT")).thenReturn(reason);
        when(reportMapper.existsActiveByReporterTarget(3L, "MEDIA", 9L)).thenReturn(false);
        when(reportMapper.insert(any(Report.class))).thenAnswer(invocation -> {
            invocation.<Report>getArgument(0).setId(20L);
            return 1;
        });

        var result = service.createMediaReport(
                3L,
                new CreateMediaReportRequest(9L, " copyright ", "  未经授权  "),
                "127.0.0.1"
        );

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.targetType()).isEqualTo(ReportTargetType.MEDIA);
        assertThat(result.reasonType()).isEqualTo("COPYRIGHT");
        assertThat(result.description()).isEqualTo("未经授权");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersion()).isZero();
        assertThat(captor.getValue().getSubmitIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void createShouldRejectDuplicateActiveReport() {
        Media media = new Media();
        media.setId(9L);
        media.setStatus(1);
        ReportReasonType reason = new ReportReasonType();
        reason.setCode("COPYRIGHT");

        when(reportMapper.countSubmittedSince(eq(3L), any(LocalDateTime.class))).thenReturn(0L);
        when(mediaMapper.selectByIdForUpdate(9L)).thenReturn(media);
        when(reasonTypeMapper.selectEnabledByCode("COPYRIGHT")).thenReturn(reason);
        when(reportMapper.existsActiveByReporterTarget(3L, "MEDIA", 9L)).thenReturn(true);

        assertThatThrownBy(() -> service.createMediaReport(
                3L,
                new CreateMediaReportRequest(9L, "COPYRIGHT", null),
                "127.0.0.1"
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode()).isEqualTo(BizCode.REPORT_DUPLICATE_ACTIVE)
        );

        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void createShouldRejectHourlyRateLimitBeforeLockingTarget() {
        when(reportMapper.countSubmittedSince(eq(3L), any(LocalDateTime.class))).thenReturn(10L);

        assertThatThrownBy(() -> service.createMediaReport(
                3L,
                new CreateMediaReportRequest(9L, "COPYRIGHT", null),
                "127.0.0.1"
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode()).isEqualTo(BizCode.REPORT_RATE_LIMIT_EXCEEDED)
        );

        verifyNoInteractions(mediaMapper, reasonTypeMapper);
    }

    @Test
    void createUserReportShouldPersistUserTarget() {
        User target = new User();
        target.setId(8L);
        ReportReasonType reason = new ReportReasonType();
        reason.setCode("OTHER");
        reason.setName("其他");

        when(reportMapper.countSubmittedSince(eq(3L), any(LocalDateTime.class))).thenReturn(0L);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(target);
        when(reasonTypeMapper.selectEnabledByCode("OTHER")).thenReturn(reason);
        when(reportMapper.existsActiveByReporterTarget(3L, "USER", 8L)).thenReturn(false);
        when(reportMapper.insert(any(Report.class))).thenAnswer(invocation -> {
            invocation.<Report>getArgument(0).setId(21L);
            return 1;
        });

        var result = service.createUserReport(
                3L, new CreateUserReportRequest(8L, "other", null), "127.0.0.1"
        );

        assertThat(result.id()).isEqualTo(21L);
        assertThat(result.targetType()).isEqualTo(ReportTargetType.USER);
    }

    @Test
    void createUserReportShouldRejectSelfTarget() {
        when(reportMapper.countSubmittedSince(eq(3L), any(LocalDateTime.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.createUserReport(
                3L, new CreateUserReportRequest(3L, "OTHER", null), "127.0.0.1"
        )).isInstanceOfSatisfying(BizException.class, exception ->
                assertThat(exception.getBizCode()).isEqualTo(BizCode.REPORT_SELF_TARGET_FORBIDDEN)
        );

        verifyNoInteractions(userMapper, reasonTypeMapper);
    }
}
