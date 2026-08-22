package cn.sduonline.business.mapper;

import cn.sduonline.business.data.projection.SubmissionSummaryRow;
import cn.sduonline.business.data.po.Submission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
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
}
