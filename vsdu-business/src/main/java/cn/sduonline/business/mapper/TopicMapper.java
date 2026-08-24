package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.AdminUpdateTopicRequest;
import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.projection.TopicSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
    List<TopicSummaryRow> selectEnabledSummaries();

    int updatePartial(
            @Param("id") Long id,
            @Param("request") AdminUpdateTopicRequest request,
            @Param("value") Topic value,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
