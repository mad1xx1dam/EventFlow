package com.eventflow.eventflow_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        // включаем логирование строки запроса (?param=value)
        filter.setIncludeQueryString(true);
        // включаем логирование тела запроса (payload)
        filter.setIncludePayload(true);
        // ограничиваем длину тела, чтобы логи не лопнули от больших файлов
        filter.setMaxPayloadLength(10000);
        // можно включить хедеры (true), но обычно они дают слишком много мусора
        filter.setIncludeHeaders(false);
        filter.setIncludeClientInfo(true);
        return filter;
    }
}