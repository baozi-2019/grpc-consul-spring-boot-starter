package com.baozi.properties;

import java.time.Duration;

public class HttpClientProperties {
    private Integer maxConnTotal = 1000;
    private Integer maxConnPerRoute = 100;
    private DefaultSocketProperties defaultSocket = new DefaultSocketProperties();
    private DefaultConnectionProperties defaultConnection = new DefaultConnectionProperties();
    private Duration closeIdle = Duration.ofSeconds(5);
    private RequestProperties request = new RequestProperties();

    public Integer getMaxConnTotal() {
        return maxConnTotal;
    }

    public void setMaxConnTotal(Integer maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
    }

    public Integer getMaxConnPerRoute() {
        return maxConnPerRoute;
    }

    public void setMaxConnPerRoute(Integer maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
    }

    public DefaultSocketProperties getDefaultSocket() {
        return defaultSocket;
    }

    public void setDefaultSocket(DefaultSocketProperties defaultSocket) {
        this.defaultSocket = defaultSocket;
    }

    public DefaultConnectionProperties getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(DefaultConnectionProperties defaultConnection) {
        this.defaultConnection = defaultConnection;
    }

    public Duration getCloseIdle() {
        return closeIdle;
    }

    public void setCloseIdle(Duration closeIdle) {
        this.closeIdle = closeIdle;
    }

    public RequestProperties getRequest() {
        return request;
    }

    public void setRequest(RequestProperties request) {
        this.request = request;
    }

    public static class DefaultSocketProperties {
        private Duration soTimeout = Duration.ofSeconds(5);

        public Duration getSoTimeout() {
            return soTimeout;
        }

        public void setSoTimeout(Duration soTimeout) {
            this.soTimeout = soTimeout;
        }
    }

    public static class DefaultConnectionProperties {
        private Duration socketTimeout = Duration.ofSeconds(5);
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration timeToLive = Duration.ofSeconds(5);

        public Duration getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;
        }
    }

    public static class RequestProperties {
        private Duration connectionRequestTimeout = Duration.ofSeconds(5);
        private Duration connectionKeepAlive = Duration.ofSeconds(5);

        public Duration getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
        }

        public Duration getConnectionKeepAlive() {
            return connectionKeepAlive;
        }

        public void setConnectionKeepAlive(Duration connectionKeepAlive) {
            this.connectionKeepAlive = connectionKeepAlive;
        }
    }
}
