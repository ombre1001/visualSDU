package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Media;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicMediaMapper {

    @Select("SELECT COUNT(*) FROM topic_media WHERE topic_id = #{topicId}")
    long countAllMedia(@Param("topicId") Long topicId);

    @Select("SELECT media_id FROM topic_media WHERE topic_id = #{topicId} ORDER BY sort_order, media_id")
    List<Long> selectAllMediaIds(@Param("topicId") Long topicId);

    @Insert("""
            INSERT INTO topic_media(topic_id, media_id, sort_order, created_at)
            VALUES(#{topicId}, #{mediaId}, #{sortOrder}, CURRENT_TIMESTAMP)
            """)
    int upsertRelation(
            @Param("topicId") Long topicId,
            @Param("mediaId") Long mediaId,
            @Param("sortOrder") Integer sortOrder
    );

    @Delete("DELETE FROM topic_media WHERE topic_id = #{topicId}")
    int deleteByTopic(@Param("topicId") Long topicId);

    @Delete("DELETE FROM topic_media WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);

    @Select("""
            SELECT COUNT(*)
            FROM topic_media tm
            INNER JOIN media m
                ON m.id = tm.media_id
                AND m.status = 1
            INNER JOIN location l
                ON l.id = m.location_id
                AND l.status = 1
            INNER JOIN campus c
                ON c.id = l.campus_id
                AND c.status = 1
            INNER JOIN city ci
                ON ci.id = c.city_id
                AND ci.status = 1
            WHERE tm.topic_id = #{topicId}
            """)
    long countVisibleMedia(
            @Param("topicId") Long topicId
    );

    @Select("""
            SELECT m.*
            FROM topic_media tm
            INNER JOIN media m
                ON m.id = tm.media_id
                AND m.status = 1
            INNER JOIN location l
                ON l.id = m.location_id
                AND l.status = 1
            INNER JOIN campus c
                ON c.id = l.campus_id
                AND c.status = 1
            INNER JOIN city ci
                ON ci.id = c.city_id
                AND ci.status = 1
            WHERE tm.topic_id = #{topicId}
            ORDER BY tm.sort_order ASC,
                     m.created_at DESC,
                     m.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<Media> selectVisibleMedia(
            @Param("topicId") Long topicId,
            @Param("offset") long offset,
            @Param("limit") long limit
    );
}
