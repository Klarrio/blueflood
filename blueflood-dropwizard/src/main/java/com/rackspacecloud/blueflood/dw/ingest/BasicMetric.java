package com.rackspacecloud.blueflood.dw.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BasicMetric {

    private long collectionTime;
    private int ttlInSeconds;
    private Number metricValue;
    private String metricName;
    private List<String> tags;
    private Map<String, String> metadata;

    public BasicMetric() {
    }

    @JsonProperty
    public long getCollectionTime() {
        return collectionTime;
    }

    @JsonProperty
    public void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }

    @JsonProperty
    public int getTtlInSeconds() {
        return ttlInSeconds;
    }

    @JsonProperty
    public void setTtlInSeconds(int ttlInSeconds) {
        this.ttlInSeconds = ttlInSeconds;
    }

    @JsonProperty
    public Number getMetricValue() {
        return metricValue;
    }

    @JsonProperty
    public void setMetricValue(Number metricValue) {
        this.metricValue = metricValue;
    }

    @JsonProperty
    public String getMetricName() {
        return metricName;
    }

    @JsonProperty
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    @JsonProperty
    public List<String> getTags() { return Collections.unmodifiableList(tags); }

    @JsonProperty
    public void setTags(List<String> tags) { this.tags = tags; }

    @JsonProperty
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }

    @JsonProperty
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
