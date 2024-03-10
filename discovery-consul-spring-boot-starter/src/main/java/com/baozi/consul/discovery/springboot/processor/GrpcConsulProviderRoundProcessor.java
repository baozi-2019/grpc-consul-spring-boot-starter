package com.baozi.consul.discovery.springboot.processor;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.service.NewService;
import com.baozi.consul.discovery.exception.IllegalServiceTypeException;
import com.baozi.consul.discovery.grpc.interceptor.ErrorServerInterceptor;
import com.baozi.consul.discovery.properties.DiscoveryProperties;
import com.baozi.consul.discovery.springboot.annotations.GrpcService;
import com.baozi.consul.discovery.springboot.exception.DiscoveryStarterException;
import com.baozi.consul.discovery.springboot.properties.ConsulProperties;
import com.baozi.consul.exception.ConsulClientException;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.Map;

public class GrpcConsulProviderRoundProcessor implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(GrpcConsulProviderRoundProcessor.class);

    private final DiscoveryProperties discoveryProperties;
    private final ConsulClient consulClient;
    private final ApplicationContext applicationContext;
    private final HealthGrpc.HealthImplBase healthImplBase;
    private final ErrorServerInterceptor errorServerInterceptor;
    private Server grpcServer;

    public GrpcConsulProviderRoundProcessor(ConsulProperties consulProperties,
                                            ConsulClient consulClient,
                                            ApplicationContext applicationContext,
                                            HealthGrpc.HealthImplBase healthImplBase,
                                            ErrorServerInterceptor errorServerInterceptor) {
        this.discoveryProperties = consulProperties.getDiscovery();
        this.consulClient = consulClient;
        this.applicationContext = applicationContext;
        this.healthImplBase = healthImplBase;
        this.errorServerInterceptor = errorServerInterceptor;
    }

    private static NewService getNewService(DiscoveryProperties.Service serviceProperties) {
        NewService newService = new NewService();
        newService.setName(serviceProperties.getName());
        newService.setId(serviceProperties.getId());
        newService.setAddress(serviceProperties.getHost());
        newService.setPort(serviceProperties.getPort());

        DiscoveryProperties.Service.ServiceCheckProperties check = serviceProperties.getCheck();
        NewService.Check serviceCheck = new NewService.Check();
        serviceCheck.setGrpc(check.getUrl());
        serviceCheck.setInterval(check.getInterval());
        serviceCheck.setGrpcUseTLS(check.isGrpcUseTls());
        newService.setCheck(serviceCheck);
        return newService;
    }

    @Override
    public void destroy() throws Exception {
        DiscoveryProperties.Service service = this.discoveryProperties.getService();
        if (service.isTemporary()) {
            try {
                boolean result = this.consulClient.deregisterService(service.getId());
                logger.info("服务注销{}", result);
            } catch (ConsulClientException e) {
                logger.error("跳过服务注销", e);
            }
        }
        this.grpcServer.shutdown();
        this.grpcServer.awaitTermination();
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        DiscoveryProperties.Service serviceProperties = discoveryProperties.getService();
        Map<String, Object> beanMap = this.applicationContext.getBeansWithAnnotation(GrpcService.class);

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serviceProperties.getPort());
        serverBuilder.addService(this.healthImplBase);
        for (Object object : beanMap.values()) {
            if (object instanceof BindableService bindableService) {
                serverBuilder.addService(bindableService);
            } else if (object instanceof ServerServiceDefinition serverServiceDefinition) {
                serverBuilder.addService(serverServiceDefinition);
            } else {
                throw new IllegalServiceTypeException(object.getClass().getPackageName() + "不是合法的service类型");
            }
        }

        // 启动grpc服务
        try {
            this.grpcServer = serverBuilder.intercept(this.errorServerInterceptor).build().start();
        } catch (IOException e) {
            throw new DiscoveryStarterException("grpc服务启动失败", e);
        }

        NewService newService = getNewService(serviceProperties);
        boolean result;
        try {
            result = consulClient.registerService(newService);
        } catch (ConsulClientException e) {
            throw new DiscoveryStarterException("注册服务错误", e);
        }
        logger.info("服务注册{}", result);
    }
}
