package com.firstticket.queueservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
    "com.firstticket.queueservice",
    "com.firstticket.common.messaging.inbox"
})
@EnableJpaRepositories(basePackages = {
    "com.firstticket.queueservice",
    "com.firstticket.common.messaging.inbox"
})
public class JpaConfig {
}
