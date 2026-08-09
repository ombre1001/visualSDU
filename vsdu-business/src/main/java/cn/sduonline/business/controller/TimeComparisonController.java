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

    @GetMapping
    public Result<List<TimeComparisonSummaryVO>> list(
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "30") int size
    ) {
        return Result.success(timeComparisonService.list(locationId, size));
    }

    @GetMapping("/{comparisonId}")
    public Result<TimeComparisonDetailVO> detail(@PathVariable Long comparisonId) {
        Long userId = CurrentUser.isLogin() ? CurrentUser.id() : null;
        return Result.success(timeComparisonService.detail(comparisonId, userId));
    }
}
