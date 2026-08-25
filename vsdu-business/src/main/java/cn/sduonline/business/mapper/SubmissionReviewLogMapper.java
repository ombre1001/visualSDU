package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.SubmissionReviewLog;
import cn.sduonline.business.data.projection.AdminSubmissionReviewLogRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SubmissionReviewLogMapper extends BaseMapper<SubmissionReviewLog> {

    SubmissionReviewLog selectBySubmissionAndVersion(
            @Param("submissionId") Long submissionId,
            @Param("submissionVersion") Integer submissionVersion
    );

    int selectNextRoundNo(@Param("submissionId") Long submissionId);

    long countBySubmission(@Param("submissionId") Long submissionId);

    List<AdminSubmissionReviewLogRow> selectPageBySubmission(
            @Param("submissionId") Long submissionId,
            @Param("offset") long offset,
            @Param("size") long size
    );
}
