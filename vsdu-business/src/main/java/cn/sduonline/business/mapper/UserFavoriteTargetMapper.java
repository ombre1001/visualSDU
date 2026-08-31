package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.po.UserFavoriteTarget;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFavoriteTargetMapper extends BaseMapper<UserFavoriteTarget> {

    @Insert("""
            INSERT IGNORE INTO user_favorite_target(user_id, target_type, target_id)
            VALUES(#{userId}, #{targetType}, #{targetId})
            """)
    int insertIgnore(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Delete("""
            DELETE FROM user_favorite_target
            WHERE user_id = #{userId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteRelation(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT COUNT(*)
            FROM user_favorite_target
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    long countByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT EXISTS(
                SELECT 1
                FROM user_favorite_target
                WHERE user_id = #{userId}
                  AND target_type = #{targetType}
                  AND target_id = #{targetId}
            )
            """)
    boolean existsByUserTarget(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT COUNT(*)
            FROM user_favorite_target favorite
            INNER JOIN location l ON l.id = favorite.target_id AND l.status = 1
            INNER JOIN campus c ON c.id = l.campus_id AND c.status = 1
            INNER JOIN city ci ON ci.id = c.city_id AND ci.status = 1
            WHERE favorite.user_id = #{userId}
              AND favorite.target_type = 'LOCATION'
            """)
    long countFavoriteLocations(@Param("userId") Long userId);

    @Select("""
            SELECT l.*
            FROM user_favorite_target favorite
            INNER JOIN location l ON l.id = favorite.target_id AND l.status = 1
            INNER JOIN campus c ON c.id = l.campus_id AND c.status = 1
            INNER JOIN city ci ON ci.id = c.city_id AND ci.status = 1
            WHERE favorite.user_id = #{userId}
              AND favorite.target_type = 'LOCATION'
            ORDER BY favorite.created_at DESC, favorite.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<Location> selectFavoriteLocations(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") long size
    );

    @Select("""
            SELECT COUNT(*)
            FROM user_favorite_target favorite
            INNER JOIN topic t ON t.id = favorite.target_id AND t.status = 1
            WHERE favorite.user_id = #{userId}
              AND favorite.target_type = 'TOPIC'
            """)
    long countFavoriteTopics(@Param("userId") Long userId);

    @Select("""
            SELECT t.*
            FROM user_favorite_target favorite
            INNER JOIN topic t ON t.id = favorite.target_id AND t.status = 1
            WHERE favorite.user_id = #{userId}
              AND favorite.target_type = 'TOPIC'
            ORDER BY favorite.created_at DESC, favorite.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<Topic> selectFavoriteTopics(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") long size
    );
}
