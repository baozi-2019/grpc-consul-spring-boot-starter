package com.baozi.consul.discovery.grpc.interceptor;

import com.alibaba.fastjson2.JSON;
import com.baozi.consul.discovery.grpc.message.GrpcErrorMessage;
import com.baozi.consul.discovery.grpc.record.ClassInstanceWithMethodRecord;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public class ErrorServerInterceptor implements ServerInterceptor {
    public static final Metadata.Key<GrpcErrorMessage.ErrorMessage> errorMessageKey = ProtoUtils.keyForProto(GrpcErrorMessage.ErrorMessage.getDefaultInstance());
    private final Map<Class<?>, ClassInstanceWithMethodRecord> errorHandleMap;
    private final String serviceName;

    public ErrorServerInterceptor(Map<Class<?>, ClassInstanceWithMethodRecord> errorHandleMap, String serviceName) {
        this.errorHandleMap = errorHandleMap;
        this.serviceName = serviceName;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> reqTListener = next.startCall(call, headers);
        return new ErrorServerCallListener<>(reqTListener, call, this.errorHandleMap, this.serviceName);
    }

    private static class ErrorServerCallListener<ReqT, RespT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, RespT> call;
        private final Map<? extends Class<?>, ClassInstanceWithMethodRecord> methodMap;
        private final String serviceName;

        protected ErrorServerCallListener(ServerCall.Listener<ReqT> delegate, ServerCall<ReqT, RespT> call,
                                          Map<? extends Class<?>, ClassInstanceWithMethodRecord> methodMap, String serviceName) {
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
                ClassInstanceWithMethodRecord classInstanceWithMethodRecord = methodMap.get(e.getClass());
                classInstanceWithMethodRecord = Optional.ofNullable(classInstanceWithMethodRecord).orElse(new ClassInstanceWithMethodRecord(null, null));
                Method method = classInstanceWithMethodRecord.method();
                if (method == null) {
                    classInstanceWithMethodRecord = methodMap.get(Exception.class);
                    classInstanceWithMethodRecord = Optional.ofNullable(classInstanceWithMethodRecord).orElse(new ClassInstanceWithMethodRecord(null, null));
                    method = classInstanceWithMethodRecord.method();
                }
                GrpcErrorMessage.ErrorMessage.Builder errorMessageBuilder = GrpcErrorMessage.ErrorMessage.newBuilder().setServiceName(this.serviceName)
                        .setReason(e.getMessage());
                if (method != null) {
                    Object exceptionResult;
                    try {
                        if (method.getParameterCount() == 1)
                            exceptionResult = method.invoke(classInstanceWithMethodRecord.instance(), e);
                        else if (method.getParameterCount() == 0)
                            exceptionResult = method.invoke(classInstanceWithMethodRecord.instance());
                        else throw new IllegalArgumentException("参数不得多于一个");
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                    errorMessageBuilder = errorMessageBuilder.putMetadata("exceptionResult", JSON.toJSONString(exceptionResult));
                } else {
                    errorMessageBuilder.putMetadata("exceptionResult", e.getMessage());
                }
                Metadata metadata = new Metadata();
                metadata.put(errorMessageKey, errorMessageBuilder.build());
                this.call.close(Status.INTERNAL.withDescription(e.getMessage()), metadata);
                throw e;
            }
        }
    }
}
