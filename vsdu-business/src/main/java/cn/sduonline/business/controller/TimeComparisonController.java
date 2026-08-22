package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.TimeComparisonDetailVO;
import cn.sduonline.business.data.vo.TimeComparisonSummaryVO;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.TimeComparisonService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/time-comparisons")
public class TimeComparisonController {
    private final TimeComparisonService timeComparisonService;

    /**
     * 时光对比列表
     * 按可选地点筛选可见的时光对比摘要。
     */
    @GetMapping
    public Result<List<TimeComparisonSummaryVO>> list(
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "30") int size
    ) {
        return Result.success(timeComparisonService.list(locationId, size));
    }

    /**
     * 时光对比详情
     * 查询指定时光对比及其按展示顺序排列的媒体项。
     */
    @GetMapping("/{comparisonId}")
    public Result<TimeComparisonDetailVO> detail(@PathVariable Long comparisonId) {
        Long userId = CurrentUser.isLogin() ? CurrentUser.id() : null;
        return Result.success(timeComparisonService.detail(comparisonId, userId));
    }
}
