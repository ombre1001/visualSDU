package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.ChangePasswordRequest;
import cn.sduonline.business.data.dto.UpdateUserProfileRequest;
import cn.sduonline.business.data.vo.BrowseHistoryVO;
import cn.sduonline.business.data.vo.UserProfileVO;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.UserService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserProfileVO> profile() {
        return Result.success(userService.profile(CurrentUser.id()));
    }

    @PatchMapping("/me")
    public Result<UserProfileVO> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return Result.success(userService.updateProfile(CurrentUser.id(), request), "个人资料修改成功");
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileVO> updateAvatar(
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return Result.success(userService.updateAvatar(CurrentUser.id(), file), "头像修改成功");
    }

    @DeleteMapping("/me/avatar")
    public Result<UserProfileVO> deleteAvatar() {
        return Result.success(userService.deleteAvatar(CurrentUser.id()), "头像删除成功");
    }

    @PutMapping(value = "/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(), request);
        return Result.success(null, "密码修改成功，现有登录凭据已失效");
    }

    @GetMapping("/me/history")
    public Result<PageResult<BrowseHistoryVO>> history(
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数") long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数") long size
    ) {
        return Result.success(userService.history(CurrentUser.id(), page, size));
    }

    @DeleteMapping("/me/history")
    public Result<Void> clearHistory() {
        userService.clearHistory(CurrentUser.id());
        return Result.success(null, "浏览足迹已清空");
    }

    @DeleteMapping("/me/history/{mediaId}")
    public Result<Void> deleteHistory(
            @PathVariable @Positive(message = "媒体ID必须为正数") Long mediaId
    ) {
        userService.deleteHistory(CurrentUser.id(), mediaId);
        return Result.success(null, "浏览足迹已删除");
    }
}
