package com.api.file.share;

import com.api.file.share.configuration.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({JwtProperties.class})

public class ApiFileShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiFileShareApplication.class, args);
    }

}
