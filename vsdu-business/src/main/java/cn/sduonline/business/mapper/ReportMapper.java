package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Report;
import cn.sduonline.business.data.projection.AdminReportDetailRow;
import cn.sduonline.business.data.projection.AdminReportSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportMapper extends BaseMapper<Report> {
    long countSubmittedSince(
            @Param("reporterId") Long reporterId,
            @Param("since") LocalDateTime since
    );

    boolean existsActiveByReporterTarget(
            @Param("reporterId") Long reporterId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    boolean existsActiveByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    long countAdminReports(
            @Param("status") Integer status,
            @Param("targetType") String targetType,
            @Param("reasonCode") String reasonCode,
            @Param("reporterId") Long reporterId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo
    );

    List<AdminReportSummaryRow> selectAdminReportPage(
            @Param("status") Integer status,
            @Param("targetType") String targetType,
            @Param("reasonCode") String reasonCode,
            @Param("reporterId") Long reporterId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("offset") long offset,
            @Param("size") long size
    );

    AdminReportDetailRow selectAdminReportDetail(@Param("reportId") Long reportId);

    int updateDecisionWithVersion(
            @Param("reportId") Long reportId,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("afterStatus") Integer afterStatus,
            @Param("decisionReason") String decisionReason,
            @Param("processedBy") Long processedBy,
            @Param("processedAt") LocalDateTime processedAt
    );
}
