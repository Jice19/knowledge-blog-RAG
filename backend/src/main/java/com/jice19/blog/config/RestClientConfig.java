package com.jice19.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端配置。
 * 使用 HttpURLConnection（SimpleClientHttpRequestFactory）而非 JDK HttpClient，
 * 二者对 PUT/DELETE 的实现不同，可规避 JDK HttpClient 在本机环境下的异常行为。
 */
@Configuration
public class RestClientConfig {

    public RestClient buildLocalRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
