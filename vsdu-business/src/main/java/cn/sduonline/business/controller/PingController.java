package cn.sduonline.business.controller;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class PingController {

    @GetMapping("/public")
    public Result<Void> publicPing() {
        return Result.success(null, "public ping successfully");
    }

    @GetMapping("/auth")
    public Result<Void> authPing() {
        return Result.success(null, "auth ping successfully. Welcome, "
                + (CurrentUser.role() == UserRole.USER ? "USER" : "ADMIN") + " "
                + CurrentUser.id() + " !");
    }

    @GetMapping("/admin")
    public Result<Void> adminPing() {
        return Result.success(null, "admin ping successfully. Welcome, ADMIN " + CurrentUser.id() + " !");
    }
}
