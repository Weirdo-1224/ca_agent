package org.example.ca_agent.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 测试配置：将 @Async 方法替换为同步执行，
 * 避免 @Transactional 测试中异步线程看不到未提交事务数据的问题。
 */
@TestConfiguration
public class SyncAsyncTestConfig {

    @Bean
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
