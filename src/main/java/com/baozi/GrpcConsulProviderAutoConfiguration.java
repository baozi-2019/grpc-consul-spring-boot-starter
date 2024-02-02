package com.baozi;

import com.alibaba.fastjson2.JSON;
import com.baozi.annotations.EnableRegister;
import com.baozi.annotations.GrpcService;
import com.baozi.cosul.bean.service.NewService;
import com.baozi.exception.ConsulException;
import com.baozi.exception.GrpcServerStartException;
import com.baozi.exception.IllegalServiceTypeException;
import com.baozi.properties.DiscoveryProperties;
import com.baozi.properties.GrpcConsulProperties;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Configuration
@ConditionalOnClass(GrpcService.class)
@ConditionalOnBean(annotation = EnableRegister.class)
@AutoConfigureOrder(value = Integer.MAX_VALUE)
@EnableConfigurationProperties(GrpcConsulProperties.class)
@DependsOn({"consulHttpClient"})
@ConditionalOnProperty("grpc-consul.discovery.register")
public class GrpcConsulProviderAutoConfiguration implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(GrpcConsulProviderAutoConfiguration.class);

    private final ApplicationContext applicationContext;
    private final GrpcConsulProperties grpcConsulProperties;
    private final DiscoveryProperties discoveryProperties;
    private final CloseableHttpClient consulHttpClient;
    private final HttpHost consulHost;
    private Server grpcServer;

    @Autowired
    public GrpcConsulProviderAutoConfiguration(
            ApplicationContext applicationContext,
            GrpcConsulProperties grpcConsulProperties,
            CloseableHttpClient consulHttpClient
    ) throws URISyntaxException {
        this.applicationContext = applicationContext;
        this.grpcConsulProperties = grpcConsulProperties;
        this.consulHttpClient = consulHttpClient;
        this.discoveryProperties = this.grpcConsulProperties.getDiscovery();
        this.consulHost = HttpHost.create("http://" + this.discoveryProperties.getHost() + ":" + this.discoveryProperties.getPort());
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
            throw new GrpcServerStartException("grpc服务启动失败", e);
        }

        NewService newService = getNewService(serviceProperties);
        try {
            String execute = this.consulHttpClient.execute(this.consulHost,
                    ClassicRequestBuilder.put("/v1/agent/service/register")
                            .setEntity(JSON.toJSONString(newService), ContentType.APPLICATION_JSON).build(),
                    response -> response.getEntity().toString());
            logger.info("服务注册{}", execute);
        } catch (IOException e) {
            throw new ConsulException("consul服务注册失败", e);
        }
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
            String execute = this.consulHttpClient.execute(this.consulHost,
                    ClassicRequestBuilder.put("/v1/agent/service/deregister/" + service.getId())
                            .build(),
                    response -> response.getEntity().toString());
            logger.info("服务注销{}", execute);
        }
        this.grpcServer.shutdown();
        this.grpcServer.awaitTermination();
    }
}
