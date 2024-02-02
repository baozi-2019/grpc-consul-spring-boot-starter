package com.baozi.grpc.resover;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baozi.cosul.bean.catalog.CatalogService;
import com.baozi.cosul.bean.health.HealthService;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ConsulNameResolver extends NameResolver {
    private final Logger logger = LoggerFactory.getLogger(ConsulNameResolver.class);

    private final ScheduledExecutorService timerService;
    private final Duration resolveInterval;
    private final CloseableHttpClient httpClient;
    private final String serviceName;
    private final HttpHost consulHttpHost;
    private Listener listener = null;
    private boolean shutdown = false;
    private Set<HostAndPort> knownServiceAddresses = null;
    private ScheduledFuture resolutionTask = null;

    public ConsulNameResolver(ScheduledExecutorService timerService, Duration resolveInterval,
                              CloseableHttpClient httpClient, String serviceName,
                              HttpHost consulHttpHost) {
        this.timerService = timerService;
        this.resolveInterval = resolveInterval;
        this.httpClient = httpClient;
        this.serviceName = serviceName;
        this.consulHttpHost = consulHttpHost;
    }

    @Override
    public void start(Listener listener) {
        checkState(this.listener == null, "ConsulNameResolver已经启动了");
        this.listener = checkNotNull(listener, "listener不能是空");
        this.resolutionTask = timerService.scheduleAtFixedRate(
                this::run,0, resolveInterval.getSeconds(), TimeUnit.SECONDS
        );
    }

    private synchronized void run() {
        if (this.shutdown) return;
        checkNotNull(this.listener, "resolver未启动");
        checkNotNull(this.timerService, "resolver未启动");
        try {
            List<HealthService> results = httpClient.execute(this.consulHttpHost,
                    ClassicRequestBuilder.get("/v1/health/service/" + serviceName).addParameter("passing", "true").build(),
                    response -> {
                        HttpEntity entity = response.getEntity();
                        try (InputStream inputStream = entity.getContent()) {
                            JSONArray objects = JSON.parseArray(inputStream);
                            List<HealthService> healthServiceList = new ArrayList<>(objects.size());
                            for (int i = 0; i < objects.size(); i++) {
                                healthServiceList.add(objects.getObject(i, HealthService.class));
                            }
                            return healthServiceList;
                        }
                    }
            );

            final Set<HostAndPort> readAddressList = results.stream().map(healthService -> {
                HealthService.Service service = healthService.getService();
                final String host = service.getAddress();
                final int port = service.getPort();

                return HostAndPort.fromParts(host, port);

            }).collect(Collectors.toSet());


            if (readAddressList.isEmpty()) {
                logger.warn("从consul获取服务成功，但未找到可用服务");

            } else if (!readAddressList.equals(this.knownServiceAddresses)) {
                this.knownServiceAddresses = readAddressList;

                final List<EquivalentAddressGroup> servers = readAddressList.stream()
                        .map((hostAndPort) -> {
                            final SocketAddress address = new InetSocketAddress(
                                    hostAndPort.getHost(),
                                    hostAndPort.getPort()
                            );

                            return new EquivalentAddressGroup(address);
                        }).collect(Collectors.toList());

                this.listener.onAddresses(servers, Attributes.EMPTY);
            }
        } catch (Exception e) {
            if (this.shutdown) return;
            logger.error("动态解析GRPC服务失败", e);
            if (this.knownServiceAddresses == null) {
                this.listener.onError(Status.UNAVAILABLE.withCause(e));
            }
        }
    }

    @Override
    public void refresh() {
        checkState(listener != null, "ConsulNameResolver还未启动");
    }

    @Override
    public String getServiceAuthority() {
        return null;
    }

    @Override
    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        shutdown = true;

        if (resolutionTask != null) {
            resolutionTask.cancel(false);
            resolutionTask = null;
        }
    }
}
