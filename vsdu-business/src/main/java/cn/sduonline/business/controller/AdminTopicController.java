package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminCreateTopicRequest;
import cn.sduonline.business.data.dto.AdminUpdateTopicRequest;
import cn.sduonline.business.data.vo.AdminTopicVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminTopicService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/topics")
public class AdminTopicController {
    private final AdminTopicService service;

    @AdminApi
    @PostMapping
    public Result<AdminTopicVO> create(@Valid @RequestBody AdminCreateTopicRequest request) {
        return Result.success(service.create(request), "专题创建成功");
    }

    @AdminApi
    @PatchMapping("/{topicId}")
    public Result<AdminTopicVO> update(@PathVariable @Positive Long topicId,
                                       @Valid @RequestBody AdminUpdateTopicRequest request) {
        return Result.success(service.update(topicId, request), "专题修改成功");
    }
}
