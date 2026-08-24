package cn.sduonline.business.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDetailFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String IP_MDC_KEY = "ip";

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString();
        String clientIp = request.getRemoteAddr();

        Map<String, String> previousMdc = MDC.getCopyOfContextMap();

        try {

            MDC.put(TRACE_ID_MDC_KEY, traceId);
            MDC.put(IP_MDC_KEY, clientIp);

            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.setContextMap(previousMdc);
        }

    }
}
