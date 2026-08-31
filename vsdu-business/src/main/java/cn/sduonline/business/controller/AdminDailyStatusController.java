package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.StatsResponse;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminDailyStatusService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDailyStatusController {

    private final AdminDailyStatusService adminDailyStatusService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    public void snapshotYesterdayStats() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        adminDailyStatusService.snapshotDailyStats(statDate);
        log.info("管理员首页每日统计快照完成，statDate={}", statDate);
    }

    @AdminApi
    @GetMapping("/dashboard/stats")
    public Result<StatsResponse> stats() {
        return Result.success(adminDailyStatusService.stats());
    }
}
