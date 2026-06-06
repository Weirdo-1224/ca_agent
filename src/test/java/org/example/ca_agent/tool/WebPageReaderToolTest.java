package org.example.ca_agent.tool;

import org.example.ca_agent.client.MetasoSearchClient;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebPageReaderToolTest {

    private final MetasoSearchClient client = mock(MetasoSearchClient.class);
    private final MetasoSearchProperties properties = new MetasoSearchProperties();

    @Test
    void returnsEmptyStringWhenDisabled() {
        properties.setEnabled(false);
        WebPageReaderTool tool = new WebPageReaderTool(client, properties);

        assertThat(tool.read("https://example.com")).isEmpty();
        verify(client, never()).readPage(anyString(), anyString());
    }

    @Test
    void returnsContentWhenEnabled() {
        properties.setEnabled(true);
        properties.setApiKey("mk-test");

        when(client.readPage("https://example.com", "markdown"))
                .thenReturn("# Title\nContent");

        WebPageReaderTool tool = new WebPageReaderTool(client, properties);
        String content = tool.read("https://example.com");

        assertThat(content).isEqualTo("# Title\nContent");
    }

    @Test
    void returnsEmptyStringOnClientException() {
        properties.setEnabled(false);
        WebPageReaderTool tool = new WebPageReaderTool(client, properties);
        assertThat(tool.read("https://example.com")).isEmpty();
    }
}
