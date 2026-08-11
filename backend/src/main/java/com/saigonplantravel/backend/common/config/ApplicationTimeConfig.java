package com.saigonplantravel.backend.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTimeConfig {
    public static final ZoneId HO_CHI_MINH_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock applicationClock() {
        return Clock.system(HO_CHI_MINH_ZONE);
    }
}
