package cn.sduonline.business.mapper;

import cn.sduonline.business.data.projection.MediaSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopicMediaMapper {

    long countAllMedia(@Param("topicId") Long topicId);

    List<Long> selectAllMediaIds(@Param("topicId") Long topicId);

    int upsertRelation(
            @Param("topicId") Long topicId,
            @Param("mediaId") Long mediaId,
            @Param("sortOrder") Integer sortOrder
    );

    int deleteByTopic(@Param("topicId") Long topicId);

    int deleteByMedia(@Param("mediaId") Long mediaId);

    long countVisibleMedia(
            @Param("topicId") Long topicId
    );

    List<MediaSummaryRow> selectVisibleMedia(
            @Param("topicId") Long topicId,
            @Param("offset") long offset,
            @Param("limit") long limit
    );
}
