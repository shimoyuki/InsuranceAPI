package com.lycoris.entity.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@PropertySource("classpath:threadpool.properties")
@EnableAsync
@Configuration
public class AsyncConfig {
 
		//接收报文核心线程数
		@Value("${threadpool.core.poolsize}")
		private int threadpoolCorePoolSize;
		//接收报文最大线程数
		@Value("${threadpool.max.poolsize}")
		private int threadpoolMaxPoolSize;
		//接收报文队列容量
		@Value("${threadpool.queue.capacity}")
		private int threadpoolQueueCapacity;
		//接收报文线程活跃时间（秒）
		@Value("${threadpool.keepAlive.seconds}")
		private int threadpoolKeepAliveSeconds;
		//接收报文默认线程名称
		@Value("${threadpool.thread.name.prefix}")
		private String threadpoolThreadNamePrefix;
		
		 /**
		  * threadpoolTaskExecutor:(接口的线程池). 
		  * @return TaskExecutor taskExecutor接口
		  * @since JDK 1.8
		  */
	    @Bean(name="threadpoolTask")
	    public ThreadPoolTaskExecutor threadpoolTaskExecutor() {
	    	//newFixedThreadPool
	        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	        // 设置核心线程数
	        executor.setCorePoolSize(threadpoolCorePoolSize);
	        // 设置最大线程数
	        executor.setMaxPoolSize(threadpoolMaxPoolSize);
	        // 设置队列容量
	        executor.setQueueCapacity(threadpoolQueueCapacity);
	        // 设置线程活跃时间（秒）
	        executor.setKeepAliveSeconds(threadpoolKeepAliveSeconds);
	        // 设置默认线程名称
	        executor.setThreadNamePrefix(threadpoolThreadNamePrefix);
	        // 设置拒绝策略
	        // rejection-policy：当pool已经达到max size的时候，如何处理新任务  
	        // CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行 
	        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
	        // 等待所有任务结束后再关闭线程池
	        executor.setWaitForTasksToCompleteOnShutdown(true);
	        executor.initialize();
	        return executor;
	    }
}
