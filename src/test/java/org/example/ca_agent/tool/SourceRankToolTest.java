package org.example.ca_agent.tool;

import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SourceRankToolTest {

    private final SourceRankTool tool = new SourceRankTool();

    @ParameterizedTest
    @CsvSource({
            "https://cursor.com, HIGH",
            "https://github.com/cursor, HIGH",
            "https://openai.com/pricing, HIGH",
            "https://docs.anthropic.com, HIGH",
            "https://example.com/blog, MEDIUM",
            "https://reddit.com/r/cursor, LOW",
            "https://news.site/article, MEDIUM",
            "https://unknown.com/page, MEDIUM"
    })
    void ranksUrlByDomainAndKeywords(String url, ReliabilityLevel expected) {
        assertThat(tool.rank(url, "", "")).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "https://cursor.com/pricing, pricing, PRICING_PAGE",
            "https://docs.anthropic.com, docs, DOCUMENTATION",
            "https://blog.openai.com, blog post, BLOG",
            "https://reddit.com/r/ai, discussion, USER_COMMENT",
            "https://cursor.com, home, OFFICIAL_SITE"
    })
    void classifiesUrlByKeywords(String url, String title, SourceType expected) {
        assertThat(tool.classify(url, title, "")).isEqualTo(expected);
    }

    @Test
    void blogKeywordInTitleClassifiesAsBlog() {
        assertThat(tool.classify("https://example.com", "Latest Blog Post", ""))
                .isEqualTo(SourceType.BLOG);
    }

    @Test
    void docsKeywordInUrlClassifiesAsDocumentation() {
        assertThat(tool.classify("https://example.com/api-docs", "API Reference", ""))
                .isEqualTo(SourceType.DOCUMENTATION);
    }
}
