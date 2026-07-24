package com.srm;

import com.srm.config.SrmSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SrmSecurityProperties.class)
public class SrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrmApplication.class, args);
    }
}

