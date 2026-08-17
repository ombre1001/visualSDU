package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.UserBrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBrowseHistoryMapper {

    void upsertView(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    long countVisibleByUser(@Param("userId") Long userId);

    List<UserBrowseHistory> selectVisiblePage(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") long size
    );

    void deleteByUser(@Param("userId") Long userId);

    void deleteByUserAndMedia(@Param("userId") Long userId, @Param("mediaId") Long mediaId);
}
