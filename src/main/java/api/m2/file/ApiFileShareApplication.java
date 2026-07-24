package api.m2.file;

import api.m2.file.configuration.properties.JwtProperties;
import api.m2.file.configuration.properties.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class})

public class ApiFileShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiFileShareApplication.class, args);
    }

}
