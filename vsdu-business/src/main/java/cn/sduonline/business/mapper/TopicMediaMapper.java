package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Media;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicMediaMapper {

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