package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.DailyStats;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {

    @Select("select stat_date, pending_review_count, daily_uploaded_media_count, visible_media_total, daily_registered_user_count, registered_user_total, created_at, updated_at from daily_stats where stat_date = #{yesterday}")
    DailyStats selectByDate(LocalDate yesterday);

    @Insert("""
            insert into daily_stats (
                stat_date,
                pending_review_count,
                daily_uploaded_media_count,
                visible_media_total,
                daily_registered_user_count,
                registered_user_total
            ) values (
                #{stats.statDate},
                #{stats.pendingReviewCount},
                #{stats.dailyUploadedMediaCount},
                #{stats.visibleMediaTotal},
                #{stats.dailyRegisteredUserCount},
                #{stats.registeredUserTotal}
            )
            on duplicate key update
                pending_review_count = values(pending_review_count),
                daily_uploaded_media_count = values(daily_uploaded_media_count),
                visible_media_total = values(visible_media_total),
                daily_registered_user_count = values(daily_registered_user_count),
                registered_user_total = values(registered_user_total)
            """)
    int upsert(@Param("stats") DailyStats stats);
}
