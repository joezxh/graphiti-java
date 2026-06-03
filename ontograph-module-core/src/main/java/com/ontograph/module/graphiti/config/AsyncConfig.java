package com.ontograph.module.graphiti.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 搜索 Pipeline 异步执行配置类
 *
 * <p>参考 Python graphiti 的 semaphore_gather 并发模式，为搜索流程配置专用线程池。
 * 包含三个专用 Executor：
 * <ul>
 *   <li>searchExecutor: 核心搜索 Scope 并发（4 线程，对应 Python 的 4 个 Scope 并行）</li>
 *   <li>searchRerankExecutor: 重排任务并发（10 线程，用于 Cross-Encoder 等重排）</li>
 *   <li>searchIOExecutor: I/O 操作并发（20 线程，用于 Neo4j 查询等）</li>
 * </ul>
 *
 * <p>参考 Python 实现：graphiti_core/search/search.py:semaphore_gather()
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 核心搜索 Scope 并发执行器
     * 对应 Python semaphore_gather(concurrency_limit=4)
     * 用于并行执行 edge/node/episode/community 四个搜索 Scope
     */
    @Bean(name = "searchExecutor")
    public Executor searchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("search-scope-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("searchExecutor initialized: core={}, max={}", 4, 8);
        return executor;
    }

    /**
     * 重排任务并发执行器
     * 用于 Cross-Encoder、RRF、MMR 等重排任务的并行执行
     */
    @Bean(name = "searchRerankExecutor")
    public Executor searchRerankExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("search-rerank-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("searchRerankExecutor initialized: core={}, max={}", 10, 20);
        return executor;
    }

    /**
     * I/O 操作并发执行器
     * 用于 Neo4j 查询、Redis 操作等 I/O 密集型任务
     */
    @Bean(name = "searchIOExecutor")
    public Executor searchIOExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("search-io-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("searchIOExecutor initialized: core={}, max={}", 20, 40);
        return executor;
    }

    /**
     * 异步任务未捕获异常处理器
     */
    @Bean
    public AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Async task exception in method {}: {}", method.getName(), ex.getMessage(), ex);
            }
        };
    }
}
