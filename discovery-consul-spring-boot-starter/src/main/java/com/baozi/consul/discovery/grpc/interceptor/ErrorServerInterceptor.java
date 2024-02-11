package com.baozi.consul.discovery.grpc.interceptor;

import com.alibaba.fastjson2.JSON;
import com.baozi.consul.discovery.grpc.message.GrpcErrorMessage;
import com.baozi.consul.discovery.processor.GrpcExceptionProcessor;
import com.baozi.consul.discovery.properties.DiscoveryProperties;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ErrorServerInterceptor implements ServerInterceptor {
    public static final Metadata.Key<GrpcErrorMessage.ErrorMessage> errorMessageKey = ProtoUtils.keyForProto(GrpcErrorMessage.ErrorMessage.getDefaultInstance());
    private final GrpcExceptionProcessor grpcExceptionProcessor;
    private final DiscoveryProperties discoveryProperties;

    public ErrorServerInterceptor(GrpcExceptionProcessor grpcExceptionProcessor, DiscoveryProperties discoveryProperties) {
        this.grpcExceptionProcessor = grpcExceptionProcessor;
        this.discoveryProperties = discoveryProperties;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> reqTListener = next.startCall(call, headers);
        return new ErrorServerCallListener<>(reqTListener, call, this.grpcExceptionProcessor.getMethodMap(), this.discoveryProperties.getService().getName());
    }

    private static class ErrorServerCallListener<ReqT, RespT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, RespT> call;
        private final Map<? extends Class<?>, Method> methodMap;
        private final String serviceName;

        protected ErrorServerCallListener(ServerCall.Listener<ReqT> delegate, ServerCall<ReqT, RespT> call,
                                          Map<? extends Class<?>, Method> methodMap, String serviceName) {
            super(delegate);
            this.call = call;
            this.methodMap = methodMap;
            this.serviceName = serviceName;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (Exception e) {
                Method method = methodMap.get(e.getClass());
                if (method == null)
                    method = methodMap.get(Exception.class);
                GrpcErrorMessage.ErrorMessage.Builder errorMessageBuilder = GrpcErrorMessage.ErrorMessage.newBuilder().setServiceName(this.serviceName)
                        .setReason(e.getMessage());
                if (method != null) {
                    Class<?> declaringClass = method.getDeclaringClass();
                    Object exceptionResult;
                    try {
                        if (method.getParameterCount() == 1)
                            exceptionResult = method.invoke(declaringClass.getConstructor().newInstance(), e);
                        else if (method.getParameterCount() == 0)
                            exceptionResult = method.invoke(declaringClass.getConstructor().newInstance());
                        else throw new IllegalArgumentException("参数不得多于一个");
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                             NoSuchMethodException ex) {
                        throw new RuntimeException(ex);
                    }
                    errorMessageBuilder = errorMessageBuilder.putMetadata("exceptionResult", JSON.toJSONString(exceptionResult));
                }
                Metadata metadata = new Metadata();
                metadata.put(errorMessageKey, errorMessageBuilder.build());
                this.call.close(Status.INTERNAL.withDescription(e.getMessage()), metadata);
                throw e;
            }
        }
    }
}
