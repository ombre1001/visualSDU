package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    long countPublished();

    List<Announcement> selectPublishedPage(
            @Param("offset") long offset,
            @Param("size") long size
    );

    long countAdmin(
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    List<Announcement> selectAdminPage(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("size") long size
    );
}
