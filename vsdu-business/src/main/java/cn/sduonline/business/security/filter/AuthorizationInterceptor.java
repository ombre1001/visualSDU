package cn.sduonline.business.security.filter;

import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean isPublicApi = handlerMethod.getMethodAnnotation(PublicApi.class) != null;
        boolean isAdminApi = handlerMethod.getMethodAnnotation(AdminApi.class) != null;
        boolean isLogin = CurrentUser.isLogin();
        boolean isAdmin = isLogin && CurrentUser.isAdmin();

        if (isPublicApi && isAdminApi) {
            log.error("公开的管理员接口？你想啥呢？目标方法：{}", handlerMethod);
            throw new BizException(BizCode.INTERNAL_SERVER_ERROR);
        }

        if (isPublicApi) return true;   // 公共接口直接放行
        if (isAdminApi) {
            if (isAdmin) return true;   // 管理员接口，只放行管理员用户
            else throw new BizException(BizCode.ADMIN_REQUIRED);
        }
        if (isLogin) return true;       // 普通接口，只放行已登录用户
        else throw new BizException(BizCode.UNAUTHORIZED);
    }
}







