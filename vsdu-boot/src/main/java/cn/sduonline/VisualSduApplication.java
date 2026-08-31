package cn.sduonline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("cn.sduonline.business.mapper")
@SpringBootApplication
public class VisualSduApplication {

    static void main(String[] args) {
        SpringApplication.run(VisualSduApplication.class, args);
    }
}