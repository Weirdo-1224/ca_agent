package org.example.ca_agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.client.MetasoSearchClient;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebPageReaderTool {

    private final MetasoSearchClient client;
    private final MetasoSearchProperties properties;

    public String read(String url) {
        if (!properties.isEnabled()) {
            log.warn("Metaso reader disabled; returning empty content for url: {}", url);
            return "";
        }
        try {
            return client.readPage(url, "markdown");
        } catch (Exception e) {
            log.error("Metaso reader failed for url '{}': {}", url, e.getMessage());
            return "";
        }
    }
}
