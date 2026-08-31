package cn.sduonline.business.service;

import cn.sduonline.business.data.po.DailyStats;
import cn.sduonline.business.data.vo.StatsResponse;
import cn.sduonline.business.mapper.DailyStatsMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.SubmissionMapper;
import cn.sduonline.business.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class AdminDailyStatusService {

    private final DailyStatsMapper dailyStatsMapper;
    private final SubmissionMapper submissionMapper;
    private final MediaMapper mediaMapper;
    private final UserMapper userMapper;

    @Transactional
    public void snapshotDailyStats(LocalDate statDate) {
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.plusDays(1).atStartOfDay();

        long pendingReviewCount = submissionMapper.countPendingSubmissions();
        long dailyUploadedMediaCount = mediaMapper.countVisibleCreatedBetween(start, end);
        long visibleMediaTotal = mediaMapper.countVisibleTotal();
        long dailyRegisteredUserCount = userMapper.countRegisteredBetween(start, end);
        long registeredUserTotal = userMapper.countRegisteredTotal();

        DailyStats dailyStats = DailyStats.builder()
                .statDate(statDate)
                .pendingReviewCount(pendingReviewCount)
                .dailyUploadedMediaCount(dailyUploadedMediaCount)
                .visibleMediaTotal(visibleMediaTotal)
                .dailyRegisteredUserCount(dailyRegisteredUserCount)
                .registeredUserTotal(registeredUserTotal)
                .build();
        dailyStatsMapper.upsert(dailyStats);
    }

    @Transactional
    public StatsResponse stats() {
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        LocalDate yesterday = LocalDate.now().minusDays(1);
        DailyStats yesterdayStats = dailyStatsMapper.selectByDate(yesterday);

        if (yesterdayStats == null || yesterdayStats.getStatDate() == null) {
            yesterdayStats = DailyStats.ZERO;
        }

        long totalMediaCount = mediaMapper.countVisibleTotal();
        long monthlyDeltaMediaCount = mediaMapper.countVisibleCreatedBetween(monthStart, now);
        long dailyDeltaMediaCount = totalMediaCount - yesterdayStats.getVisibleMediaTotal();
        long pendingSubmissionsCount = submissionMapper.countPendingSubmissions();
        long dailyDeltaPendingSubmissionsCount = pendingSubmissionsCount - yesterdayStats.getPendingReviewCount();
        long dayUploadedMediaCount = mediaMapper.countVisibleCreatedBetween(LocalDate.now().atStartOfDay(), now);
        long dailyDeltaDayUploadedMediaCount = dayUploadedMediaCount - yesterdayStats.getDailyUploadedMediaCount();
        long registeredUsers = userMapper.countRegisteredTotal();
        long dailyDeltaRegisteredUsers = registeredUsers - yesterdayStats.getRegisteredUserTotal();

        return StatsResponse.builder()
                .totalMediaCount(totalMediaCount)
                .monthlyDeltaMediaCount(monthlyDeltaMediaCount)
                .dailyDeltaMediaCount(dailyDeltaMediaCount)
                .pendingSubmissionsCount(pendingSubmissionsCount)
                .dailyDeltaPendingSubmissionsCount(dailyDeltaPendingSubmissionsCount)
                .dayUploadedMediaCount(dayUploadedMediaCount)
                .dailyDeltaDayUploadedMediaCount(dailyDeltaDayUploadedMediaCount)
                .registeredUsers(registeredUsers)
                .dailyDeltaRegisteredUsers(dailyDeltaRegisteredUsers)
                .build();
    }
}
