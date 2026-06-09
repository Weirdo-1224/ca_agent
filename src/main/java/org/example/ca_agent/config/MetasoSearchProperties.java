package org.example.ca_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ca-agent.search")
public class MetasoSearchProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://metaso.cn/api/v1";
    private int timeout = 30000;
    private int maxResultsPerQuery = 10;

    public boolean isEnabled() {
        return enabled && !apiKey.isBlank();
    }

    public boolean isEnabledRaw() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxResultsPerQuery() {
        return maxResultsPerQuery;
    }

    public void setMaxResultsPerQuery(int maxResultsPerQuery) {
        this.maxResultsPerQuery = maxResultsPerQuery;
    }
}
