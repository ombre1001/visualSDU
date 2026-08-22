package cn.sduonline.business.controller;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class PingController {

    /**
     * 公开健康检查
     * 检查无需登录即可访问的服务链路。
     */
    @PublicApi
    @GetMapping("/public")
    public Result<Void> publicPing() {
        return Result.success(null, "public ping successfully"
                + (CurrentUser.isLogin() ? ", No. " + CurrentUser.id() : "")
        );
    }

    /**
     * 登录健康检查
     * 检查访问令牌鉴权及当前用户上下文是否正常。
     */
    @GetMapping("/auth")
    public Result<Void> authPing() {
        return Result.success(null, "auth ping successfully. Welcome, "
                + (CurrentUser.role() == UserRole.USER ? "USER" : "ADMIN") + " "
                + CurrentUser.id() + " !");
    }

    /**
     * 管理员健康检查
     * 检查管理员鉴权及当前管理员上下文是否正常。
     */
    @AdminApi
    @GetMapping("/admin")
    public Result<Void> adminPing() {
        return Result.success(null, "admin ping successfully. Welcome, ADMIN " + CurrentUser.id() + " !");
    }
}
