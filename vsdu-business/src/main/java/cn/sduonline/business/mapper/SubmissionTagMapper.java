package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.SubmissionTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubmissionTagMapper {
    List<SubmissionTag> selectBySubmissionId(@Param("submissionId") Long submissionId);

    List<SubmissionTag> selectBySubmissionIds(@Param("submissionIds") Collection<Long> submissionIds);

    List<Long> selectSubmissionIdsByTagId(@Param("tagId") Long tagId);

    List<SubmissionTag> selectBySubmissionIdsForUpdate(@Param("submissionIds") Collection<Long> submissionIds);

    int insertBatch(@Param("relations") List<SubmissionTag> relations);

    int deleteBySubmissionId(@Param("submissionId") Long submissionId);

    int deleteBySubmissionIds(@Param("submissionIds") Collection<Long> submissionIds);

    int deleteByTagId(@Param("tagId") Long tagId);

    long countByTagId(@Param("tagId") Long tagId);
}
