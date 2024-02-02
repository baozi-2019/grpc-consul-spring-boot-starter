package com.baozi.config;

import com.baozi.properties.GrpcConsulProperties;
import com.baozi.properties.HttpClientProperties;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrpcConsulProperties.class)
public class HttpClientConfig {

    @Bean
    public CloseableHttpClient consulHttpClient(GrpcConsulProperties grpcConsulProperties) {
        HttpClientProperties httpClientProperties = grpcConsulProperties.getHttpClient();
        HttpClientProperties.DefaultSocketProperties defaultSocket = httpClientProperties.getDefaultSocket();
        HttpClientProperties.DefaultConnectionProperties defaultConnection = httpClientProperties.getDefaultConnection();
        HttpClientProperties.RequestProperties requestProperties = httpClientProperties.getRequest();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.of(defaultSocket.getSoTimeout()))
                        .build())
                .setMaxConnTotal(httpClientProperties.getMaxConnTotal())
                .setMaxConnPerRoute(httpClientProperties.getMaxConnPerRoute())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setSocketTimeout(Timeout.of(defaultConnection.getSocketTimeout()))
                        .setConnectTimeout(Timeout.of(defaultConnection.getConnectTimeout()))
                        .setTimeToLive(TimeValue.of(defaultConnection.getTimeToLive()))
                        .build())
                .build();

        connectionManager.closeIdle(TimeValue.of(httpClientProperties.getCloseIdle()));

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.of(requestProperties.getConnectionRequestTimeout()))
                        .setConnectionKeepAlive(TimeValue.of(requestProperties.getConnectionKeepAlive()))
                        .build())
                .build();
    }
}
