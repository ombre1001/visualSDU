package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminUpdateUserPermissionRequest;
import cn.sduonline.business.data.dto.AdminUpdateUserRoleRequest;
import cn.sduonline.business.data.dto.AdminUpdateUserStatusRequest;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.vo.AdminUserVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.AdminUserService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @AdminApi
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestParam(required = false)
            @Size(max = 50, message = "搜索关键字不能超过50个字符") String keyword,
            @RequestParam(required = false)
            @Min(value = 0, message = "role只能为0或1")
            @Max(value = 1, message = "role只能为0或1") Integer role,
            @RequestParam(required = false)
            @Min(value = 0, message = "status只能为0、1或2")
            @Max(value = 2, message = "status只能为0、1或2") Integer status,
            @RequestParam(defaultValue = "1") @Positive(message = "页码必须为正数") long page,
            @RequestParam(defaultValue = "20") @Positive(message = "每页数量必须为正数") long size
    ) {
        UserRole userRole = role == null ? null : UserRole.valueOf(role);
        UserStatus userStatus = status == null ? null : UserStatus.valueOf(status);
        return Result.success(adminUserService.list(keyword, userRole, userStatus, page, size));
    }

    @AdminApi
    @GetMapping("/{userId}")
    public Result<AdminUserVO> detail(
            @PathVariable @Positive(message = "用户ID必须为正数") Long userId
    ) {
        return Result.success(adminUserService.detail(userId));
    }

    @AdminApi
    @PatchMapping("/{userId}/role")
    public Result<AdminUserVO> updateRole(
            @PathVariable @Positive(message = "用户ID必须为正数") Long userId,
            @Valid @RequestBody AdminUpdateUserRoleRequest request
    ) {
        AdminUserVO result = adminUserService.updateRole(
                CurrentUser.id(), userId, UserRole.valueOf(request.role())
        );
        return Result.success(result, "用户角色修改成功");
    }

    @AdminApi
    @PatchMapping("/{userId}/status")
    public Result<AdminUserVO> updateStatus(
            @PathVariable @Positive(message = "用户ID必须为正数") Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        AdminUserVO result = adminUserService.updateStatus(
                CurrentUser.id(), userId, UserStatus.valueOf(request.status()),
                request.frozenUntil(), request.frozenReason()
        );
        return Result.success(result, "用户状态修改成功");
    }

    @AdminApi
    @PatchMapping("/{userId}/permissions")
    public Result<AdminUserVO> updatePermissions(
            @PathVariable @Positive(message = "用户ID必须为正数") Long userId,
            @Valid @RequestBody AdminUpdateUserPermissionRequest request
    ) {
        return Result.success(
                adminUserService.updatePermissions(userId, request),
                "用户权限修改成功"
        );
    }
}
