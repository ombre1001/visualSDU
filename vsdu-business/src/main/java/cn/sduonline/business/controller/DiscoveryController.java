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