package com.baozi.cosul.bean.health;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * @author Vasily Vasilkov (vgv@ecwid.com)
 */
public class Check {

    public static enum CheckStatus {
        @JSONField(name = "unknown")
        UNKNOWN,
        @JSONField(name = "passing")
        PASSING,
        @JSONField(name = "warning")
        WARNING,
        @JSONField(name = "critical")
        CRITICAL
    }

    @JSONField(name = "Node")
    private String node;

    @JSONField(name = "CheckID")
    private String checkId;

    @JSONField(name = "Name")
    private String name;

    @JSONField(name = "Status")
    private CheckStatus status;

    @JSONField(name = "Notes")
    private String notes;

    @JSONField(name = "Output")
    private String output;

    @JSONField(name = "ServiceID")
    private String serviceId;

    @JSONField(name = "ServiceName")
    private String serviceName;

    @JSONField(name = "ServiceTags")
    private List<String> serviceTags;

    @JSONField(name = "CreateIndex")
    private Long createIndex;

    @JSONField(name = "ModifyIndex")
    private Long modifyIndex;

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public void setStatus(CheckStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<String> getServiceTags() {
        return serviceTags;
    }

    public void setServiceTags(List<String> serviceTags) {
        this.serviceTags = serviceTags;
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
        return "Check{" +
                "node='" + node + '\'' +
                ", checkId='" + checkId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", notes='" + notes + '\'' +
                ", output='" + output + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", serviceTags=" + serviceTags +
                ", createIndex=" + createIndex +
                ", modifyIndex=" + modifyIndex +
                '}';
    }
}
