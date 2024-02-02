package com.baozi.grpc.resover.provider;

import com.baozi.exception.IllegalGrpcSchemaException;
import com.baozi.exception.IllegalSyntaxException;
import com.baozi.grpc.resover.ConsulNameResolver;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHost;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ConsulNameResolverProvider extends NameResolverProvider {
    private final String SCHEMA = "consul";
    private final ScheduledExecutorService timerService;
    private final Duration resolveInterval;
    private final CloseableHttpClient httpClient;

    private ConsulNameResolverProvider(Builder builder) {
        this.timerService = builder.timerService;
        this.resolveInterval = builder.resolveInterval;
        this.httpClient = builder.httpClient;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!SCHEMA.equals(targetUri.getScheme()))
            throw new IllegalGrpcSchemaException("GRPC协议必须为consul");
        // 分离请求路径
        String targetPath = checkNotNull(targetUri.getPath(), "URL错误");
        checkArgument(targetPath.startsWith("/"), "路径错误");
        // 解析路径信息
        String serviceName = targetPath.substring(1);
        checkArgument(!serviceName.isEmpty(), "未解析到服务名");
        // 解析consul服务主机IP
        String consulHost = targetUri.getHost();
        checkArgument(!consulHost.isEmpty(), "未解析到consul服务主机IP");
        // 解析consul服务端口
        int consulPort = targetUri.getPort();
        checkArgument(consulPort >= 0 && consulPort <= 65535, "未解析到consul服务主机IP");

        try {
            return new ConsulNameResolver(this.timerService, this.resolveInterval,
                    this.httpClient, serviceName, HttpHost.create("http://" + consulHost + ":" + consulPort));
        } catch (URISyntaxException e) {
            throw new IllegalSyntaxException(e);
        }
    }

    public static Builder newBuilder(CloseableHttpClient httpClient) {
        return new Builder(httpClient);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }

    public static final class Builder {
        private ScheduledExecutorService timerService = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder()
                        .setNameFormat("consul-name-resolver")
                        .setUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
                            System.out.println("查询consul节点异常" + throwable.toString());
                        })
                        .build(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        private Duration resolveInterval = Duration.ofMinutes(1);
        private CloseableHttpClient httpClient;

        private Builder(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        public ScheduledExecutorService getTimerService() {
            return timerService;
        }

        public void setTimerService(ScheduledExecutorService timerService) {
            this.timerService = timerService;
        }

        public Duration getResolveInterval() {
            return resolveInterval;
        }

        public void setResolveInterval(Duration resolveInterval) {
            this.resolveInterval = resolveInterval;
        }

        public CloseableHttpClient getHttpClient() {
            return httpClient;
        }

        public void setHttpClient(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        public Builder withTimeService(ScheduledExecutorService timeService) {
            this.timerService = timeService;
            return this;
        }

        public Builder withResolveInterval(Duration resolveInterval) {
            this.resolveInterval = resolveInterval;
            return this;
        }

        public ConsulNameResolverProvider build() {
            return new ConsulNameResolverProvider(this);
        }
    }
}
