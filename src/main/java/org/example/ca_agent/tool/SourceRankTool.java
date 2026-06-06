package org.example.ca_agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class SourceRankTool {

    private static final List<String> OFFICIAL_DOMAINS = List.of(
            "github.com", "cursor.com", "windsurf.com", "codeium.com",
            "copilot.github.com", "openai.com", "anthropic.com",
            "claude.ai", "deepseek.com", "kimi.moonshot.cn",
            "tongyi.aliyun.com", "qianwen.aliyun.com", "baidu.com"
    );

    private static final List<String> PRICING_KEYWORDS = List.of(
            "pricing", "price", "plan", "subscription", "费用", "定价", "价格", "套餐"
    );

    private static final List<String> DOCS_KEYWORDS = List.of(
            "docs", "documentation", "doc", "api", "guide", "教程", "文档", "手册"
    );

    private static final List<String> BLOG_KEYWORDS = List.of(
            "blog", "news", "article", "post", "博客", "新闻", "资讯"
    );

    private static final List<String> COMMUNITY_KEYWORDS = List.of(
            "reddit", "forum", "stackoverflow", "discord", "community",
            "讨论", "论坛", "社区"
    );

    public ReliabilityLevel rank(String url, String title, String snippet) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String lowerTitle = (title != null ? title : "").toLowerCase(Locale.ROOT);
        String combined = lowerUrl + " " + lowerTitle;

        for (String domain : OFFICIAL_DOMAINS) {
            if (lowerUrl.contains(domain)) {
                return ReliabilityLevel.HIGH;
            }
        }

        for (String kw : DOCS_KEYWORDS) {
            if (combined.contains(kw)) {
                return ReliabilityLevel.HIGH;
            }
        }

        for (String kw : PRICING_KEYWORDS) {
            if (combined.contains(kw)) {
                return ReliabilityLevel.MEDIUM;
            }
        }

        for (String kw : BLOG_KEYWORDS) {
            if (combined.contains(kw)) {
                return ReliabilityLevel.MEDIUM;
            }
        }

        for (String kw : COMMUNITY_KEYWORDS) {
            if (combined.contains(kw)) {
                return ReliabilityLevel.LOW;
            }
        }

        return ReliabilityLevel.MEDIUM;
    }

    public SourceType classify(String url, String title, String snippet) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String lowerTitle = (title != null ? title : "").toLowerCase(Locale.ROOT);
        String combined = lowerUrl + " " + lowerTitle;

        for (String kw : PRICING_KEYWORDS) {
            if (combined.contains(kw)) {
                return SourceType.PRICING_PAGE;
            }
        }

        for (String kw : DOCS_KEYWORDS) {
            if (combined.contains(kw)) {
                return SourceType.DOCUMENTATION;
            }
        }

        for (String kw : BLOG_KEYWORDS) {
            if (combined.contains(kw)) {
                return SourceType.BLOG;
            }
        }

        for (String kw : COMMUNITY_KEYWORDS) {
            if (combined.contains(kw)) {
                return SourceType.USER_COMMENT;
            }
        }

        return SourceType.OFFICIAL_SITE;
    }
}
