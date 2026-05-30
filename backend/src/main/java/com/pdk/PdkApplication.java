package com.pdk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.pdk.module.*.mapper")
public class PdkApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdkApplication.class, args);
    }
}
