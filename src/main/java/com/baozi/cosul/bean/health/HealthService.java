package com.baozi.cosul.bean.health;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HealthService {
    public static class Node {

        @JSONField(name = "ID")
        private String id;

        @JSONField(name = "Node")
        private String node;

        @JSONField(name = "Address")
        private String address;

        @JSONField(name = "Datacenter")
        private String datacenter;

        @JSONField(name = "TaggedAddresses")
        private Map<String, String> taggedAddresses;

        @JSONField(name = "Meta")
        private Map<String, String> meta;

        @JSONField(name = "CreateIndex")
        private Long createIndex;

        @JSONField(name = "ModifyIndex")
        private Long modifyIndex;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getDatacenter() {
            return datacenter;
        }

        public void setDatacenter(String datacenter) {
            this.datacenter = datacenter;
        }

        public Map<String, String> getTaggedAddresses() {
            return taggedAddresses;
        }

        public void setTaggedAddresses(Map<String, String> taggedAddresses) {
            this.taggedAddresses = taggedAddresses;
        }

        public Map<String, String> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, String> meta) {
            this.meta = meta;
        }

        public Long getCreateIndex() {
            return createIndex;
        }

        public void setCreateIndex(Long createIndex) {
            this.createIndex = createIndex;
        }

        public Long getModifyIndex() {
            return modifyIndex;
        }

        public void setModifyIndex(Long modifyIndex) {
            this.modifyIndex = modifyIndex;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "id='" + id + '\'' +
                    ", node='" + node + '\'' +
                    ", address='" + address + '\'' +
                    ", datacenter='" + datacenter + '\'' +
                    ", taggedAddresses=" + taggedAddresses +
                    ", meta=" + meta +
                    ", createIndex=" + createIndex +
                    ", modifyIndex=" + modifyIndex +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node1 = (Node) o;
            return Objects.equals(id, node1.id) &&
                    Objects.equals(node, node1.node) &&
                    Objects.equals(address, node1.address) &&
                    Objects.equals(datacenter, node1.datacenter) &&
                    Objects.equals(taggedAddresses, node1.taggedAddresses) &&
                    Objects.equals(meta, node1.meta) &&
                    Objects.equals(createIndex, node1.createIndex) &&
                    Objects.equals(modifyIndex, node1.modifyIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, node, address, datacenter, taggedAddresses, meta, createIndex, modifyIndex);
        }
    }

    public static class Service {
        @JSONField(name = "ID")
        private String id;

        @JSONField(name = "Service")
        private String service;

        @JSONField(name = "Tags")
        private List<String> tags;

        @JSONField(name = "Address")
        private String address;

        @JSONField(name = "Meta")
        private Map<String, String> meta;

        @JSONField(name = "Port")
        private Integer port;

        @JSONField(name = "EnableTagOverride")
        private Boolean enableTagOverride;

        @JSONField(name = "CreateIndex")
        private Long createIndex;

        @JSONField(name = "ModifyIndex")
        private Long modifyIndex;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Map<String, String> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, String> meta) {
            this.meta = meta;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Boolean getEnableTagOverride() {
            return enableTagOverride;
        }

        public void setEnableTagOverride(Boolean enableTagOverride) {
            this.enableTagOverride = enableTagOverride;
        }

        public Long getCreateIndex() {
            return createIndex;
        }

        public void setCreateIndex(Long createIndex) {
            this.createIndex = createIndex;
        }

        public Long getModifyIndex() {
            return modifyIndex;
        }

        public void setModifyIndex(Long modifyIndex) {
            this.modifyIndex = modifyIndex;
        }

        @Override
        public String toString() {
            return "Service{" +
                    "id='" + id + '\'' +
                    ", service='" + service + '\'' +
                    ", tags=" + tags +
                    ", address='" + address + '\'' +
                    ", meta=" + meta +
                    ", port=" + port +
                    ", enableTagOverride=" + enableTagOverride +
                    ", createIndex=" + createIndex +
                    ", modifyIndex=" + modifyIndex +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Service service1 = (Service) o;
            return Objects.equals(id, service1.id) &&
                    Objects.equals(service, service1.service) &&
                    Objects.equals(tags, service1.tags) &&
                    Objects.equals(address, service1.address) &&
                    Objects.equals(meta, service1.meta) &&
                    Objects.equals(port, service1.port) &&
                    Objects.equals(enableTagOverride, service1.enableTagOverride) &&
                    Objects.equals(createIndex, service1.createIndex) &&
                    Objects.equals(modifyIndex, service1.modifyIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, service, tags, address, meta, port, enableTagOverride, createIndex, modifyIndex);
        }
    }

    @JSONField(name = "Node")
    private Node node;

    @JSONField(name = "Service")
    private Service service;

    @JSONField(name = "Checks")
    private List<Check> checks;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<Check> getChecks() {
        return checks;
    }

    public void setChecks(List<Check> checks) {
        this.checks = checks;
    }

    @Override
    public String toString() {
        return "HealthService{" +
                "node=" + node +
                ", service=" + service +
                ", checks=" + checks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthService that = (HealthService) o;
        return Objects.equals(node, that.node) &&
                Objects.equals(service, that.service) &&
                Objects.equals(checks, that.checks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, service, checks);
    }
}
