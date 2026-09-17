package cn.sduonline.infrastructure.cdn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AliyunCdnClient {

    private final AliyunCdnUrlSigner aliyunCdnUrlSigner;

    public AliyunCdnClient(
            @Value("${aliyun.cdn.domain}") String domain,
            @Value("${aliyun.cdn.private-key}") String privateKey
    ) {
        this.aliyunCdnUrlSigner = new AliyunCdnUrlSigner(
                domain,
                privateKey
        );
    }


    public String generateCdnUrl(String objectKey) {
        return aliyunCdnUrlSigner.sign(objectKey);
    }
}
