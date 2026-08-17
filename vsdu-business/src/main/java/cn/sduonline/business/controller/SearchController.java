package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.SearchSuggestionVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.SearchService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private static final int DEFAULT_SUGGESTION_LIMIT = 10;

    private final SearchService searchService;

    /**
     * 搜索建议。
     * <p>
     * 推荐请求：
     * GET /search/suggestions?keyword=中心校区&limit=10
     * <p>
     * 同时兼容旧参数：
     * GET /search/suggestions?q=中心校区&limit=10
     */
    @PublicApi
    @GetMapping("/suggestions")
    public Result<List<SearchSuggestionVO>> suggestions(
            @RequestParam(
                    name = "keyword",
                    required = false
            )
            String keyword,

            @RequestParam(
                    name = "q",
                    required = false
            )
            String q,

            @RequestParam(
                    name = "limit",
                    required = false
            )
            Integer limit
    ) {
        // 优先使用keyword，兼容此前的q参数。
        String actualKeyword = StringUtils.hasText(keyword)
                ? keyword
                : q;

        // limit未传或传空字符串时，使用默认值10。
        int actualLimit = limit == null
                ? DEFAULT_SUGGESTION_LIMIT
                : limit;

        return Result.success(
                searchService.suggestions(
                        actualKeyword,
                        actualLimit
                )
        );
    }

    /**
     * 媒体综合搜索。
     */
    @PublicApi
    @GetMapping("/media")
    public Result<PageResult<MediaSummaryVO>> searchMedia(
            @Valid @ModelAttribute SearchMediaQueryDTO query
    ) {
        return Result.success(
                searchService.searchMedia(query)
        );
    }
}