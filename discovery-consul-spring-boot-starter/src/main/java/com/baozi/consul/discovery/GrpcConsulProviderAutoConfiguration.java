package com.baozi.consul.discovery;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.common.Constant;
import com.baozi.consul.discovery.annotations.GrpcService;
import com.baozi.consul.discovery.grpc.interceptor.ErrorServerInterceptor;
import com.baozi.consul.discovery.processor.GrpcConsulProviderRoundProcessor;
import com.baozi.consul.discovery.processor.GrpcExceptionProcessor;
import com.baozi.consul.discovery.properties.ConsulProperties;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn({"consulClient"})
@ConditionalOnClass(GrpcService.class)
@AutoConfigureOrder(value = Integer.MAX_VALUE)
@EnableConfigurationProperties(ConsulProperties.class)
@ConditionalOnProperty(Constant.CONFIG_PREFIX + ".discovery.register")
public class GrpcConsulProviderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public HealthGrpc.HealthImplBase healthCheckImpl() {
        return new HealthGrpc.HealthImplBase() {
            @Override
            public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
                responseObserver.onNext(HealthCheckResponse.newBuilder()
                        .setStatus(HealthCheckResponse.ServingStatus.NOT_SERVING).build());
                responseObserver.onCompleted();
            }

            @Override
            public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
                super.watch(request, responseObserver);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcExceptionProcessor grpcExceptionProcessor() {
        return new GrpcExceptionProcessor();
    }

    @Bean
    public GrpcConsulProviderRoundProcessor grpcConsulProviderRoundProcessor(
            ConsulProperties consulProperties,
            ConsulClient consulClient,
            ApplicationContext applicationContext,
            HealthGrpc.HealthImplBase healthCheckImpl,
            GrpcExceptionProcessor grpcExceptionProcessor) {
        ErrorServerInterceptor errorServerInterceptor = new ErrorServerInterceptor(grpcExceptionProcessor, consulProperties.getDiscovery());
        return new GrpcConsulProviderRoundProcessor(consulProperties, consulClient,
                applicationContext, healthCheckImpl, errorServerInterceptor);
    }
}
