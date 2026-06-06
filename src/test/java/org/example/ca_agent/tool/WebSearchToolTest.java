package org.example.ca_agent.tool;

import org.example.ca_agent.client.MetasoSearchClient;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.example.ca_agent.dto.metaso.MetasoSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSearchToolTest {

    private final MetasoSearchClient client = mock(MetasoSearchClient.class);
    private final MetasoSearchProperties properties = new MetasoSearchProperties();

    @Test
    void returnsEmptyListWhenSearchDisabled() {
        properties.setEnabled(false);
        WebSearchTool tool = new WebSearchTool(client, properties);

        List<MetasoSearchResponse.WebpageResult> results = tool.search("test");

        assertThat(results).isEmpty();
        verify(client, never()).search(anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void returnsResultsWhenSearchEnabled() {
        properties.setEnabled(true);
        properties.setApiKey("mk-test");
        properties.setMaxResultsPerQuery(3);

        MetasoSearchResponse.WebpageResult item = new MetasoSearchResponse.WebpageResult();
        item.setTitle("Result 1");
        item.setLink("https://example.com");

        MetasoSearchResponse response = new MetasoSearchResponse();
        response.setWebpages(List.of(item));

        when(client.search("test", "webpage", false, 3)).thenReturn(response);

        WebSearchTool tool = new WebSearchTool(client, properties);
        List<MetasoSearchResponse.WebpageResult> results = tool.search("test");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Result 1");
    }

    @Test
    void returnsEmptyListOnClientException() {
        properties.setEnabled(true);
        properties.setApiKey("mk-test");

        when(client.search(anyString(), anyString(), anyBoolean(), anyInt()))
                .thenThrow(new RuntimeException("API error"));

        WebSearchTool tool = new WebSearchTool(client, properties);
        List<MetasoSearchResponse.WebpageResult> results = tool.search("test");

        assertThat(results).isEmpty();
    }
}
