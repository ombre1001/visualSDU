package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.MediaFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MediaFavoriteMapper extends BaseMapper<MediaFavorite> {

    /**
     * 收藏媒体。
     *
     * media_favorite表必须具有(user_id, media_id)唯一索引，
     * 用于保证同一用户不能重复收藏同一个媒体。
     */
    @Insert("""
            INSERT IGNORE INTO media_favorite(user_id, folder_id, media_id)
            VALUES(#{userId}, #{folderId}, #{mediaId})
            """)
    int insertIgnore(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId,
            @Param("mediaId") Long mediaId
    );

    /**
     * 取消用户对指定媒体的收藏。
     */
    @Delete("""
            DELETE FROM media_favorite
            WHERE user_id = #{userId}
              AND media_id = #{mediaId}
            """)
    int deleteAllForUser(
            @Param("userId") Long userId,
            @Param("mediaId") Long mediaId
    );

    /**
     * 删除收藏夹中的所有收藏关系。
     */
    @Delete("""
            DELETE FROM media_favorite
            WHERE user_id = #{userId}
              AND folder_id = #{folderId}
            """)
    int deleteByFolder(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    /**
     * 将已经收藏的媒体移动到目标收藏夹。
     */
    @Update("""
        UPDATE media_favorite
        SET folder_id = #{targetFolderId}
        WHERE user_id = #{userId}
          AND folder_id = #{sourceFolderId}
          AND media_id = #{mediaId}
          AND NOT EXISTS (
              SELECT 1
              FROM (
                  SELECT id
                  FROM media_favorite
                  WHERE user_id = #{userId}
                    AND folder_id = #{targetFolderId}
                    AND media_id = #{mediaId}
              ) target_favorite
          )
        """)
    int moveToFolder(
            @Param("userId") Long userId,
            @Param("sourceFolderId") Long sourceFolderId,
            @Param("targetFolderId") Long targetFolderId,
            @Param("mediaId") Long mediaId
    );

    /**
     * 判断指定媒体是否位于指定收藏夹。
     */
    @Select("""
            SELECT COUNT(*)
            FROM media_favorite
            WHERE user_id = #{userId}
              AND folder_id = #{folderId}
              AND media_id = #{mediaId}
            """)
    long countFolderMedia(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId,
            @Param("mediaId") Long mediaId
    );

    /**
     * 查询收藏夹中所有收藏关系。
     * 删除收藏夹时用于同步减少媒体收藏数。
     */
    @Select("""
            SELECT id, user_id, folder_id, media_id, created_at
            FROM media_favorite
            WHERE user_id = #{userId}
              AND folder_id = #{folderId}
            ORDER BY id ASC
            """)
    List<MediaFavorite> selectByFolder(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    /**
     * 查询收藏夹中当前可见媒体数量。
     */
    @Select("""
            SELECT COUNT(*)
            FROM media_favorite mf
            INNER JOIN media m ON m.id = mf.media_id
            WHERE mf.user_id = #{userId}
              AND mf.folder_id = #{folderId}
              AND m.status = 1
            """)
    long countVisibleByFolder(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    /**
     * 分页查询收藏夹中当前可见的媒体ID。
     */
    @Select("""
            SELECT mf.media_id
            FROM media_favorite mf
            INNER JOIN media m ON m.id = mf.media_id
            WHERE mf.user_id = #{userId}
              AND mf.folder_id = #{folderId}
              AND m.status = 1
            ORDER BY mf.created_at DESC, mf.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<Long> selectVisibleMediaIds(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId,
            @Param("offset") long offset,
            @Param("size") long size
    );

    /**
     * 查询收藏夹中最近收藏的一条可见媒体。
     * 没有手动设置封面时用于生成默认封面。
     */
    @Select("""
            SELECT mf.media_id
            FROM media_favorite mf
            INNER JOIN media m ON m.id = mf.media_id
            WHERE mf.user_id = #{userId}
              AND mf.folder_id = #{folderId}
              AND m.status = 1
            ORDER BY mf.created_at DESC, mf.id DESC
            LIMIT 1
            """)
    Long selectLatestVisibleMediaId(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );


    @Delete("""
        DELETE FROM media_favorite
        WHERE user_id = #{userId}
          AND folder_id = #{sourceFolderId}
          AND media_id = #{mediaId}
          AND EXISTS (
              SELECT 1
              FROM (
                  SELECT id
                  FROM media_favorite
                  WHERE user_id = #{userId}
                    AND folder_id = #{targetFolderId}
                    AND media_id = #{mediaId}
              ) target_favorite
          )
        """)
    int deleteSourceWhenTargetExists(
            @Param("userId") Long userId,
            @Param("sourceFolderId") Long sourceFolderId,
            @Param("targetFolderId") Long targetFolderId,
            @Param("mediaId") Long mediaId
    );

    @Delete("DELETE FROM media_favorite WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);
}
