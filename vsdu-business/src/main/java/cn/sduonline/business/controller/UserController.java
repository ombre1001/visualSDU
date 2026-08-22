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

    /**
     * 查询个人资料
     * 查询当前登录用户的资料和功能权限。
     */
    @GetMapping("/me")
    public Result<UserProfileVO> profile() {
        return Result.success(userService.profile(CurrentUser.id()));
    }

    /**
     * 修改个人资料
     * 修改当前用户的昵称等可编辑资料。
     */
    @PatchMapping("/me")
    public Result<UserProfileVO> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return Result.success(userService.updateProfile(CurrentUser.id(), request), "个人资料修改成功");
    }

    /**
     * 上传或替换头像
     * 通过 multipart 表单的 file 字段上传头像，并返回更新后的个人资料。
     */
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileVO> updateAvatar(
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return Result.success(userService.updateAvatar(CurrentUser.id(), file), "头像修改成功");
    }

    /**
     * 删除头像
     * 删除当前用户头像并返回更新后的个人资料，重复删除仍视为成功。
     */
    @DeleteMapping("/me/avatar")
    public Result<UserProfileVO> deleteAvatar() {
        return Result.success(userService.deleteAvatar(CurrentUser.id()), "头像删除成功");
    }

    /**
     * 修改密码
     * 校验旧密码后更新密码，并使当前已有登录凭证全部失效。
     */
    @PutMapping(value = "/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(), request);
        return Result.success(null, "密码修改成功，现有登录凭据已失效");
    }

    /**
     * 分页查询浏览足迹
     * 按最近浏览时间倒序查询当前用户的媒体浏览记录。
     */
    @GetMapping("/me/history")
    public Result<PageResult<BrowseHistoryVO>> history(
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数") long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数") long size
    ) {
        return Result.success(userService.history(CurrentUser.id(), page, size));
    }

    /**
     * 清空浏览足迹
     * 删除当前用户的全部浏览记录，重复清空仍视为成功。
     */
    @DeleteMapping("/me/history")
    public Result<Void> clearHistory() {
        userService.clearHistory(CurrentUser.id());
        return Result.success(null, "浏览足迹已清空");
    }

    /**
     * 删除单条浏览足迹
     * 删除当前用户对指定媒体的浏览记录。
     */
    @DeleteMapping("/me/history/{mediaId}")
    public Result<Void> deleteHistory(
            @PathVariable @Positive(message = "媒体ID必须为正数") Long mediaId
    ) {
        userService.deleteHistory(CurrentUser.id(), mediaId);
        return Result.success(null, "浏览足迹已删除");
    }
}
