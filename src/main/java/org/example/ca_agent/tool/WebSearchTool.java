package org.example.ca_agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.client.MetasoSearchClient;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.example.ca_agent.dto.metaso.MetasoSearchResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final MetasoSearchClient client;
    private final MetasoSearchProperties properties;

    public List<MetasoSearchResponse.WebpageResult> search(String query) {
        return search(query, "webpage");
    }

    public List<MetasoSearchResponse.WebpageResult> search(String query, String scope) {
        if (!properties.isEnabled()) {
            log.warn("Metaso search disabled; returning empty results for query: {}", query);
            return Collections.emptyList();
        }
        try {
            MetasoSearchResponse response = client.search(
                    query, scope, false, properties.getMaxResultsPerQuery());
            List<MetasoSearchResponse.WebpageResult> results = response.getResultsForScope(scope);
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("Metaso search failed for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }
}
