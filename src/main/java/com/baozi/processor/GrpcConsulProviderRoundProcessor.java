package com.baozi.processor;

import com.baozi.annotations.GrpcService;
import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.service.NewService;
import com.baozi.consul.exception.ConsulClientException;
import com.baozi.exception.StarterException;
import com.baozi.exception.IllegalServiceTypeException;
import com.baozi.properties.DiscoveryProperties;
import com.baozi.properties.GrpcConsulProperties;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
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

    private final GrpcConsulProperties grpcConsulProperties;
    private final DiscoveryProperties discoveryProperties;
    private final ConsulClient consulClient;
    private final ApplicationContext applicationContext;
    private Server grpcServer;

    public GrpcConsulProviderRoundProcessor(GrpcConsulProperties grpcConsulProperties,
                                            ConsulClient consulClient,
                                            ApplicationContext applicationContext) {
        this.grpcConsulProperties = grpcConsulProperties;
        this.discoveryProperties = this.grpcConsulProperties.getDiscovery();
        this.consulClient = consulClient;
        this.applicationContext = applicationContext;
    }

    private static NewService getNewService(DiscoveryProperties.Service serviceProperties) {
        NewService newService = new NewService();
        newService.setName(serviceProperties.getName());
        newService.setId(serviceProperties.getId());
        newService.setAddress(serviceProperties.getHost());
        newService.setPort(serviceProperties.getPort());

        DiscoveryProperties.Service.ServiceCheckProperties check = serviceProperties.getCheck();
        NewService.Check serviceCheck = new NewService.Check();
        serviceCheck.setHttp(check.getUrl());
        serviceCheck.setInterval(check.getInterval());
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
        DiscoveryProperties.Service serviceProperties = this.grpcConsulProperties.getDiscovery().getService();
        Map<String, Object> beanMap = this.applicationContext.getBeansWithAnnotation(GrpcService.class);

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serviceProperties.getPort());
        for (Object object : beanMap.values()) {
            System.out.println(object.getClass().getCanonicalName());

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
            this.grpcServer = serverBuilder.build().start();
        } catch (IOException e) {
            throw new StarterException("grpc服务启动失败", e);
        }

        NewService newService = getNewService(serviceProperties);
        boolean result;
        try {
            result = consulClient.registerService(newService);
        } catch (ConsulClientException e) {
            throw new StarterException("注册服务错误", e);
        }
        logger.info("服务注册{}", result);
    }
}
