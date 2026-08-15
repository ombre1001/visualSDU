package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.po.Media;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MediaSearchMapper {

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM media m
            INNER JOIN location l
                ON l.id = m.location_id
                AND l.status = 1
            INNER JOIN campus c
                ON c.id = l.campus_id
                AND c.status = 1
            INNER JOIN city ci
                ON ci.id = c.city_id
                AND ci.status = 1
            WHERE m.status = 1

            <if test="query.q != null and query.q != ''">
                AND (
                    m.title LIKE CONCAT('%', #{query.q}, '%')
                    OR m.description LIKE CONCAT('%', #{query.q}, '%')
                    OR m.tags LIKE CONCAT('%', #{query.q}, '%')
                    OR l.name LIKE CONCAT('%', #{query.q}, '%')
                    OR l.address LIKE CONCAT('%', #{query.q}, '%')
                    OR c.name LIKE CONCAT('%', #{query.q}, '%')
                    OR ci.name LIKE CONCAT('%', #{query.q}, '%')
                )
            </if>

            <if test="query.cityId != null">
                AND ci.id = #{query.cityId}
            </if>

            <if test="query.campusId != null">
                AND c.id = #{query.campusId}
            </if>

            <if test="query.locationId != null">
                AND l.id = #{query.locationId}
            </if>

            <if test="query.tag != null and query.tag != ''">
                AND CONCAT('|', IFNULL(m.tags, ''), '|')
                    LIKE CONCAT('%|', #{query.tag}, '|%')
            </if>

            <if test="query.shotYear != null">
                AND YEAR(m.shot_at) = #{query.shotYear}
            </if>

            <if test="query.topicId != null">
                AND EXISTS (
                    SELECT 1
                    FROM topic_media tm
                    INNER JOIN topic t
                        ON t.id = tm.topic_id
                        AND t.status = 1
                    WHERE tm.media_id = m.id
                      AND tm.topic_id = #{query.topicId}
                )
            </if>
            </script>
            """)
    long countSearchMedia(
            @Param("query") SearchMediaQueryDTO query
    );

    @Select("""
            <script>
            SELECT m.*
            FROM media m
            INNER JOIN location l
                ON l.id = m.location_id
                AND l.status = 1
            INNER JOIN campus c
                ON c.id = l.campus_id
                AND c.status = 1
            INNER JOIN city ci
                ON ci.id = c.city_id
                AND ci.status = 1
            WHERE m.status = 1

            <if test="query.q != null and query.q != ''">
                AND (
                    m.title LIKE CONCAT('%', #{query.q}, '%')
                    OR m.description LIKE CONCAT('%', #{query.q}, '%')
                    OR m.tags LIKE CONCAT('%', #{query.q}, '%')
                    OR l.name LIKE CONCAT('%', #{query.q}, '%')
                    OR l.address LIKE CONCAT('%', #{query.q}, '%')
                    OR c.name LIKE CONCAT('%', #{query.q}, '%')
                    OR ci.name LIKE CONCAT('%', #{query.q}, '%')
                )
            </if>

            <if test="query.cityId != null">
                AND ci.id = #{query.cityId}
            </if>

            <if test="query.campusId != null">
                AND c.id = #{query.campusId}
            </if>

            <if test="query.locationId != null">
                AND l.id = #{query.locationId}
            </if>

            <if test="query.tag != null and query.tag != ''">
                AND CONCAT('|', IFNULL(m.tags, ''), '|')
                    LIKE CONCAT('%|', #{query.tag}, '|%')
            </if>

            <if test="query.shotYear != null">
                AND YEAR(m.shot_at) = #{query.shotYear}
            </if>

            <if test="query.topicId != null">
                AND EXISTS (
                    SELECT 1
                    FROM topic_media tm
                    INNER JOIN topic t
                        ON t.id = tm.topic_id
                        AND t.status = 1
                    WHERE tm.media_id = m.id
                      AND tm.topic_id = #{query.topicId}
                )
            </if>

            ORDER BY
            <choose>
                <when test='sort == "newest"'>
                    COALESCE(m.shot_at, m.created_at) DESC,
                    m.id DESC
                </when>

                <when test='sort == "oldest"'>
                    COALESCE(m.shot_at, m.created_at) ASC,
                    m.id ASC
                </when>

                <when test='sort == "hot"'>
                    (
                        COALESCE(m.favorite_count, 0) * 5
                        + COALESCE(m.like_count, 0) * 3
                        + COALESCE(m.view_count, 0)
                    ) DESC,
                    m.id DESC
                </when>

                <otherwise>
                    CASE
                        WHEN m.title = #{query.q} THEN 100
                        WHEN l.name = #{query.q} THEN 95
                        WHEN c.name = #{query.q} THEN 90
                        WHEN ci.name = #{query.q} THEN 85
                        WHEN m.title LIKE CONCAT(#{query.q}, '%') THEN 80
                        WHEN m.tags LIKE CONCAT('%', #{query.q}, '%') THEN 60
                        WHEN m.description LIKE CONCAT('%', #{query.q}, '%') THEN 40
                        ELSE 10
                    END DESC,
                    (
                        COALESCE(m.favorite_count, 0) * 5
                        + COALESCE(m.like_count, 0) * 3
                        + COALESCE(m.view_count, 0)
                    ) DESC,
                    m.id DESC
                </otherwise>
            </choose>

            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<Media> searchMedia(
            @Param("query") SearchMediaQueryDTO query,
            @Param("sort") String sort,
            @Param("offset") long offset,
            @Param("limit") long limit
    );
}