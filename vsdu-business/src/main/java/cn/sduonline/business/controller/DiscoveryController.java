package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.DiscoveryHomeVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.DiscoveryService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * 发现首页
     * 聚合热门标签、最新媒体和校区内容；不传城市 ID 时查询全部城市。
     */
    @PublicApi
    @GetMapping("/home")
    public Result<DiscoveryHomeVO> home(
            @RequestParam(required = false) Long cityId
    ) {
        return Result.success(
                discoveryService.home(cityId)
        );
    }
}
