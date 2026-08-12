package cn.sduonline.infrastructure.jwt.sdupass;

import cn.sduonline.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public class SduPassClient {

    private final RestClient sduPass;
    private final ObjectMapper objectMapper;

    public SduPassClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sduPass = RestClient.builder()
                .baseUrl("https://i.sdu.edu.cn/pass-api")
                .build();
    }

    private static final String SDUPASS_WARN_TEMPLATE = "sdupass响应状态码=%d, sdupass响应体=%s";

    public StudentInfo getToken(String callbackCode) {
        Result<StudentInfo> rs;
        try {
            rs = sduPass.post()
                    .uri("/auth/token")
                    .body(new GetTokenDTO(callbackCode))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (HttpClientErrorException he) {
            String msg = String.format(SDUPASS_WARN_TEMPLATE, he.getStatusCode().value(), he.getResponseBodyAsString());
            log.warn(msg);
            throw new SduPassClientException(msg, he);
        }

        if (rs == null) {
            throw new RuntimeException("Sdu Pass不好使了");
        }
        return objectMapper.convertValue(rs.data(), StudentInfo.class);
    }

    public static class SduPassClientException extends RuntimeException {
        public SduPassClientException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
