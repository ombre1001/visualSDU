package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.projection.AdminSubmissionDetailRow;
import cn.sduonline.business.data.projection.AdminSubmissionSummaryRow;
import cn.sduonline.business.data.projection.SubmissionSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
    long countPendingSubmissions();

    long countMine(
            @Param("userId") Long userId,
            @Param("status") Integer status
    );

    List<SubmissionSummaryRow> selectMinePage(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("offset") long offset,
            @Param("size") long size
    );

    long countAdminSubmissions(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("locationId") Long locationId,
            @Param("submittedFrom") LocalDateTime submittedFrom,
            @Param("submittedTo") LocalDateTime submittedTo
    );

    List<AdminSubmissionSummaryRow> selectAdminSubmissionPage(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("locationId") Long locationId,
            @Param("submittedFrom") LocalDateTime submittedFrom,
            @Param("submittedTo") LocalDateTime submittedTo,
            @Param("sort") String sort,
            @Param("offset") long offset,
            @Param("size") long size
    );

    AdminSubmissionDetailRow selectAdminSubmissionDetail(
            @Param("submissionId") Long submissionId
    );

    int updateReviewWithVersion(
            @Param("submissionId") Long submissionId,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("beforeStatus") Integer beforeStatus,
            @Param("afterStatus") Integer afterStatus,
            @Param("reviewReason") String reviewReason,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    int updateEditableWithVersion(
            @Param("submission") Submission submission,
            @Param("expectedStatus") Integer expectedStatus,
            @Param("expectedVersion") Integer expectedVersion
    );

    int resubmitWithVersion(
            @Param("submissionId") Long submissionId,
            @Param("userId") Long userId,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("afterStatus") Integer afterStatus,
            @Param("submittedAt") LocalDateTime submittedAt,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    int withdrawWithVersion(
            @Param("submissionId") Long submissionId,
            @Param("userId") Long userId,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
