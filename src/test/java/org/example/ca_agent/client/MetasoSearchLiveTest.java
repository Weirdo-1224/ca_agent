package org.example.ca_agent.client;

import org.example.ca_agent.dto.metaso.MetasoSearchResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live")
@ActiveProfiles("local")
@SpringBootTest
class MetasoSearchLiveTest {

    @Autowired
    private MetasoSearchClient client;

    @Test
    void searchReturnsWebpageResults() {
        MetasoSearchResponse response = client.search(
                "Cursor AI coding assistant pricing", "webpage", false, 3);

        List<MetasoSearchResponse.WebpageResult> results = response.getWebpages();
        assertThat(results).isNotNull().isNotEmpty();
        assertThat(results.get(0)).satisfies(r -> {
            assertThat(r.getTitle()).isNotBlank();
            assertThat(r.getLink() != null ? r.getLink() : r.getUrl()).isNotBlank();
        });
    }

    @Test
    void readPageReturnsMarkdownContent() {
        String content = client.readPage("https://www.cursor.com/pricing", "markdown");

        assertThat(content).isNotBlank().hasSizeGreaterThan(100);
    }
}
