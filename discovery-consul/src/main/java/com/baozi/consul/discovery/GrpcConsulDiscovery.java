package com.baozi.consul.discovery;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.service.NewService;
import com.baozi.consul.discovery.annotations.EnableGrpc;
import com.baozi.consul.discovery.annotations.GrpcAdvice;
import com.baozi.consul.discovery.annotations.GrpcExceptionHandler;
import com.baozi.consul.discovery.annotations.GrpcService;
import com.baozi.consul.discovery.exception.DiscoveryException;
import com.baozi.consul.discovery.exception.IllegalServiceTypeException;
import com.baozi.consul.discovery.grpc.interceptor.ErrorServerInterceptor;
import com.baozi.consul.discovery.grpc.record.ClassInstanceWithMethodRecord;
import com.baozi.consul.discovery.grpc.resover.provider.ConsulNameResolverProvider;
import com.baozi.consul.discovery.properties.DiscoveryProperties;
import com.baozi.consul.exception.ConsulClientException;
import com.baozi.consul.properties.ConsulProperties;
import io.grpc.*;
import io.grpc.health.v1.HealthGrpc;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GrpcConsulDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(GrpcConsulDiscovery.class);
    private Optional<ConsulClient> consulClientOptional = Optional.empty();
    private static final GrpcConsulDiscovery grpcConsulDiscovery = new GrpcConsulDiscovery();

    static class GrpcConsul {
        protected ConsulClient consulClient;
        protected DiscoveryProperties discoveryProperties;

        public void checkConsulClient(ConsulClient consulClient) {
            if (consulClient != null) {
                this.consulClient = consulClient;
                return;
            }
            this.consulClient = grpcConsulDiscovery.consulClientOptional.orElseGet(() -> {
                ConsulProperties consulProperties = new ConsulProperties();
                consulProperties.setHost(this.discoveryProperties.getHost());
                consulProperties.setPort(this.discoveryProperties.getPort());
                try {
                    ConsulClient client = new ConsulClient(this.discoveryProperties.getHttpClient(), consulProperties);
                    grpcConsulDiscovery.consulClientOptional = Optional.of(client);
                    return client;
                } catch (URISyntaxException e) {
                    throw new DiscoveryException("consul client创建失败", e);
                }
            });
        }
    }

    public static class GrpcConsulCustomer extends GrpcConsul {
        private static final Logger logger = LoggerFactory.getLogger(GrpcConsulCustomer.class);
        private final ConcurrentHashMap<String, Channel> grpcChannelMap = new ConcurrentHashMap<>();

        private GrpcConsulCustomer(ConsulClient consulClient, DiscoveryProperties discoveryProperties) {
            this.discoveryProperties = discoveryProperties;
            this.checkConsulClient(consulClient);

            // grpc服务注册器注入
            NameResolverRegistry.getDefaultRegistry().register(ConsulNameResolverProvider.newBuilder(consulClient)
                    .withResolveInterval(Duration.ofSeconds(5)).build());

            String[] registerServiceNames = discoveryProperties.getRegisterServiceNames();
            // grpc channel map初始化
            for (String serviceName : registerServiceNames) {
                // 注入对应服务提供器
                ManagedChannel channel = ManagedChannelBuilder.forTarget("consul://" + discoveryProperties.getHost()
                                + ":" + discoveryProperties.getPort() + "/" + serviceName)
                        .overrideAuthority(serviceName)
                        .defaultLoadBalancingPolicy("round_robin")
                        .disableRetry()
                        .keepAliveTime(20, TimeUnit.SECONDS)
                        .usePlaintext().build();
                this.grpcChannelMap.put(serviceName, channel);
            }
        }

        public ConcurrentHashMap<String, Channel> getGrpcChannelMap() {
            return grpcChannelMap;
        }
    }

    public static class GrpcConsulCustomerBuilder {
        private ConsulClient consulClient;
        private DiscoveryProperties discoveryProperties;

        public GrpcConsulCustomerBuilder consulClient(ConsulClient consulClient) {
            this.consulClient = consulClient;
            return this;
        }

        public GrpcConsulCustomerBuilder discoveryProperties(DiscoveryProperties discoveryProperties) {
            this.discoveryProperties = discoveryProperties;
            return this;
        }

        public Optional<GrpcConsulCustomer> build() {
            this.discoveryProperties = Optional.ofNullable(this.discoveryProperties).orElse(new DiscoveryProperties());
            if (!this.discoveryProperties.isClient()) return Optional.empty();
            return Optional.of(new GrpcConsulCustomer(this.consulClient, this.discoveryProperties));
        }

        public ConsulClient getConsulClient() {
            return consulClient;
        }

        public DiscoveryProperties getDiscoveryProperties() {
            return discoveryProperties;
        }
    }

    public static class GrpcConsulProvider extends GrpcConsul {
        private final HealthGrpc.HealthImplBase healthCheckImpl;
        private Server grpcServer;

        private GrpcConsulProvider(ConsulClient consulClient,
                                   DiscoveryProperties discoveryProperties,
                                   HealthGrpc.HealthImplBase healthCheckImpl) {
            this.discoveryProperties = discoveryProperties;
            this.healthCheckImpl = healthCheckImpl;
            this.checkConsulClient(consulClient);
        }

        public void destroy() {
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
            try {
                this.grpcServer.awaitTermination();
            } catch (InterruptedException e) {
                logger.error("grpc server关闭失败", e);
            }
        }

        public void init(Object[] services) {
            DiscoveryProperties.Service serviceProperties = this.discoveryProperties.getService();
            // 找到根包
            Reflections rootPackageReflections = new Reflections(new ConfigurationBuilder().forPackages("").setScanners(Scanners.TypesAnnotated));
            Set<Class<?>> rootPackageClasses = rootPackageReflections.get(Scanners.TypesAnnotated.get(EnableGrpc.class).asClass());
            if (rootPackageClasses == null || rootPackageClasses.isEmpty()) {
                String errorMessage = "未使用EnableGrpc注解指定主启动类";
                logger.error(errorMessage);
                throw new DiscoveryException(errorMessage);
            }
            if (rootPackageClasses.size() > 1) {
                String errorMessage = "主启动类只能有一个";
                logger.error(errorMessage);
                throw new DiscoveryException(errorMessage);
            }
            Class<?> rootClass = rootPackageClasses.stream().findFirst().get();
            String rootClassPackageName = rootClass.getPackageName();
            // 找到异常处理类，方法转map
            Map<Class<?>, ClassInstanceWithMethodRecord> errorHandleMap = getErrorHandleMap(rootClassPackageName);
            // 如果没有传入服务实例列表
            if (services == null || services.length == 0) {
                // 找到所有service
                Reflections serviceReflections = new Reflections(new ConfigurationBuilder().forPackages(rootClassPackageName).setScanners(Scanners.TypesAnnotated));
                Set<Class<?>> serviceClassSet = serviceReflections.get(Scanners.TypesAnnotated.get(GrpcService.class).asClass());
                if (logger.isDebugEnabled()) {
                    List<String> serviceNameList = serviceClassSet.stream().map(Class::getCanonicalName).toList();
                    StringBuilder logMessage = new StringBuilder();
                    for (String serviceName : serviceNameList) {
                        logMessage.append("- " + serviceName);
                    }
                    logger.debug("找到如下服务" + logMessage);
                }
                services = getServiceInstances(serviceClassSet);
            }
            // service构造器
            ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serviceProperties.getPort());
            // 加载健康检查
            serverBuilder.addService(this.healthCheckImpl);
            // 加载所有service
            for (Object service : services) {
                if (service instanceof BindableService bindableService) {
                    serverBuilder.addService(bindableService);
                } else if (service instanceof ServerServiceDefinition serverServiceDefinition) {
                    serverBuilder.addService(serverServiceDefinition);
                } else {
                    throw new IllegalServiceTypeException(service.getClass().getPackageName() + "不是合法的service类型");
                }
            }

            // 启动grpc服务
            try {
                this.grpcServer = serverBuilder.intercept(new ErrorServerInterceptor(errorHandleMap, discoveryProperties.getService().getName())).build();
                this.grpcServer.start();
            } catch (IOException e) {
                throw new DiscoveryException("grpc启动失败", e);
            }
            NewService newService = getNewService(serviceProperties);
            boolean result;
            try {
                result = consulClient.registerService(newService);
            } catch (ConsulClientException e) {
                throw new DiscoveryException("注册服务错误", e);
            }
            logger.info("服务注册{}", result);
        }

        private Object[] getServiceInstances(Set<Class<?>> serviceClasses) {
            Object[] services = new Object[serviceClasses.size()];
            int i = 0;
            for (Class<?> serviceClass : serviceClasses) {
                try {
                    services[i++] = serviceClass.getConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new DiscoveryException(serviceClass.getCanonicalName() + "没有无参构造器", e);
                }
            }
            return services;
        }

        private Map<Class<?>, ClassInstanceWithMethodRecord> getErrorHandleMap(String rootPackage) {
            Reflections errorHandleReflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(rootPackage).setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated));
            Set<Class<?>> classes = errorHandleReflections.get(Scanners.TypesAnnotated.get(GrpcAdvice.class).asClass());

            return classes.stream()
                    .map(this::findAnnotatedMethods).flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
        }

        private Set<Map.Entry<Class<?>, ClassInstanceWithMethodRecord>> findAnnotatedMethods(final Class<?> clazz) {
            try {
                final Object instance = clazz.getConstructor().newInstance();
                return Arrays.stream(clazz.getMethods()).filter(e -> e.isAnnotationPresent(GrpcExceptionHandler.class))
                        .collect(Collectors.toUnmodifiableMap(e -> {
                            GrpcExceptionHandler grpcExceptionHandler = e.getDeclaredAnnotation(GrpcExceptionHandler.class);
                            Class<?>[] parameterTypes = e.getParameterTypes();
                            if (parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0]))
                                return parameterTypes[0];
                            else if (parameterTypes.length == 0 && grpcExceptionHandler.value() != null)
                                return grpcExceptionHandler.value();
                            else throw new IllegalArgumentException("GrpcExceptionHandler注解方法参数非法");
                        }, e -> new ClassInstanceWithMethodRecord(instance, e))).entrySet();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new DiscoveryException(clazz.getCanonicalName() + "实例化失败", e);
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
            serviceCheck.setGrpc(check.getUrl());
            serviceCheck.setInterval(check.getInterval());
            serviceCheck.setGrpcUseTLS(check.isGrpcUseTls());
            newService.setCheck(serviceCheck);
            return newService;
        }
    }

    public static class GrpcConsulProviderBuilder {
        private ConsulClient consulClient;
        private DiscoveryProperties discoveryProperties;
        private HealthGrpc.HealthImplBase healthCheckImpl;

        public GrpcConsulProviderBuilder consulClient(ConsulClient consulClient) {
            this.consulClient = consulClient;
            return this;
        }

        public GrpcConsulProviderBuilder discoveryProperties(DiscoveryProperties discoveryProperties) {
            this.discoveryProperties = discoveryProperties;
            return this;
        }

        public GrpcConsulProviderBuilder healthCheckImpl(HealthGrpc.HealthImplBase healthCheckImpl) {
            this.healthCheckImpl = healthCheckImpl;
            return this;
        }

        public Optional<GrpcConsulProvider> build() {
            this.discoveryProperties = Optional.ofNullable(this.discoveryProperties).orElse(new DiscoveryProperties());
            if (!this.discoveryProperties.isRegister()) return Optional.empty();
            return Optional.of(new GrpcConsulProvider(this.consulClient, this.discoveryProperties, this.healthCheckImpl));
        }

        public ConsulClient getConsulClient() {
            return consulClient;
        }

        public DiscoveryProperties getDiscoveryProperties() {
            return discoveryProperties;
        }

        public HealthGrpc.HealthImplBase getHealthCheckImpl() {
            return healthCheckImpl;
        }
    }

}
