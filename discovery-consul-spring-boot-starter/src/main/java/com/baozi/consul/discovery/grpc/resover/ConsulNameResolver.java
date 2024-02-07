package com.baozi.consul.discovery.grpc.resover;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.health.HealthService;
import com.google.common.net.HostAndPort;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
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
    private final ConsulClient consulClient;
    private final String serviceName;
    private Listener listener = null;
    private boolean shutdown = false;
    private Set<HostAndPort> knownServiceAddresses = null;
    private ScheduledFuture resolutionTask = null;

    public ConsulNameResolver(ScheduledExecutorService timerService, Duration resolveInterval,
                              ConsulClient consulClient, String serviceName) {
        this.timerService = timerService;
        this.resolveInterval = resolveInterval;
        this.consulClient = consulClient;
        this.serviceName = serviceName;
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
            List<HealthService> results = this.consulClient.listServiceInstance(serviceName, true);

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
