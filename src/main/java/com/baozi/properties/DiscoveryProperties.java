package com.baozi.properties;

public class DiscoveryProperties {
    private final static String HOST = "127.0.0.1";

    private String host = HOST;
    private int port = 8500;
    private boolean register = false;
    private boolean client = false;
    private Service service;
    private String[] registerServiceNames;

    public static class Service {
        private String id;
        private String name;
        private String host = HOST;
        private int port = 5000;
        private boolean temporary = false;
        private ServiceCheckProperties check;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isTemporary() {
            return temporary;
        }

        public void setTemporary(boolean temporary) {
            this.temporary = temporary;
        }

        public ServiceCheckProperties getCheck() {
            return check;
        }

        public void setCheck(ServiceCheckProperties check) {
            this.check = check;
        }

        public static class ServiceCheckProperties {
            private String url;
            private String interval = "5s";
            private boolean grpcUseTls = false;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getInterval() {
                return interval;
            }

            public void setInterval(String interval) {
                this.interval = interval;
            }

            public boolean isGrpcUseTls() {
                return grpcUseTls;
            }

            public void setGrpcUseTls(boolean grpcUseTls) {
                this.grpcUseTls = grpcUseTls;
            }
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String[] getRegisterServiceNames() {
        return registerServiceNames;
    }

    public void setRegisterServiceNames(String[] registerServiceNames) {
        this.registerServiceNames = registerServiceNames;
    }
}
