package biz.thonbecker.personal.shared.platform.configuration;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous execution, primarily used for event listeners.
 * This ensures @Async event listeners execute in separate threads without blocking the publisher.
 * Creates a dedicated thread pool for event processing, separate from WebSocket operations.
 */
@Configuration
@Slf4j
class AsyncConfig implements AsyncConfigurer {

    /**
     * Dedicated thread pool executor for async event processing.
     * Configured separately from the WebSocket task executor for better isolation.
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info(
                "Initialized async event executor: core={}, max={}, queue={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());
        return executor;
    }

    /**
     * Handler for uncaught exceptions in async methods.
     * Logs exceptions that occur in event listeners without failing the application.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, _) -> log.error("Exception in async event listener: {}", method.getName(), ex);
    }
}
