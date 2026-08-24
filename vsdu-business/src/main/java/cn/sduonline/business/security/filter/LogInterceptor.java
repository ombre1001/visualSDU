package cn.sduonline.business.security.filter;


import cn.sduonline.business.security.context.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME_ATTR = "rst";

    private static final String PUBLIC_REQUEST_HAS_NO_USER_ID = "N/A";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        request.setAttribute(
                REQUEST_START_TIME_ATTR,
                System.nanoTime()
        );

        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex
    ) {

        String userId = CurrentUser.isLogin()
                ? CurrentUser.id().toString()
                : PUBLIC_REQUEST_HAS_NO_USER_ID;

        Long startTime =
                (Long) request.getAttribute(REQUEST_START_TIME_ATTR);
        if (startTime == null) return;

        long costNs = System.nanoTime() - startTime;
        double costMs = costNs / 1_000_000.0;

        log.info("处理请求 | 请求接口：{} {} | httpStatus={} userId={} costMs={} ex={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                userId,
                costMs,
                ex == null ? null : ex.getClass().getSimpleName()
        );
    }
}
