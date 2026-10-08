package com.example.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling จำเป็นก่อน @Scheduled ใน PriceAlertScheduler จะทำงานได้จริง
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
