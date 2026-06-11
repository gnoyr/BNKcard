package com.bnk;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication // 💡 AI 설정 제외(exclude) 및 패키지 필터링(ComponentScan)을 모두 제거했습니다.
public class BnKcardApplication {

    public static void main(String[] args) {
    	TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(BnKcardApplication.class, args);
    }

}