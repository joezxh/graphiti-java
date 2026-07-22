package com.ontograph.module.graphiti.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.ontograph.module.graphiti.service.impl.ai")
class AiProviderTestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    ToolCallingManager toolCallingManager() {
        return DefaultToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .toolCallbackResolver(new StaticToolCallbackResolver(List.of()))
                .toolExecutionExceptionProcessor(new DefaultToolExecutionExceptionProcessor(true))
                .build();
    }
}
