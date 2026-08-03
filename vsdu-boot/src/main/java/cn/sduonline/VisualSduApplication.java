package cn.sduonline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("cn.sduonline.business.mapper")
@SpringBootApplication
public class VisualSduApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisualSduApplication.class, args);
    }
}