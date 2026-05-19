package com.firstticket.queueservice.queuetoken.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis Lua 스크립트 빈 등록.
 *
 * <p>{@code DefaultRedisScript} 는 스크립트 본문을 캐시하고 SHA-1 으로 Redis 에 EVALSHA 호출하여
 * 매 호출마다 본문 전송을 피한다.</p>
 */
@Configuration
public class RedisScriptConfig {
    /**
     * enqueue 원자 처리 스크립트.
     * 반환: Long (1=성공, 0=중복)
     */
    @Bean
    public DefaultRedisScript<Long> enqueueScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/enqueue.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * delete 원자 처리 스크립트.
     * 반환: Long (1=역인덱스 같이 삭제, 2=역인덱스 보존)
     */
    @Bean
    public DefaultRedisScript<Long> deleteScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/delete.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
