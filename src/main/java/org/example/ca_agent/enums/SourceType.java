package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum SourceType {

    OFFICIAL_SITE("official_site", "官方网站"),
    PRICING_PAGE("pricing_page", "定价页面"),
    DOCUMENTATION("documentation", "官方文档"),
    BLOG("blog", "博客"),
    CHANGELOG("changelog", "更新日志"),
    GITHUB("github", "GitHub"),
    REVIEW_ARTICLE("review_article", "评测文章"),
    COMMUNITY_DISCUSSION("community_discussion", "社区讨论"),
    NEWS("news", "新闻"),
    USER_COMMENT("user_comment", "用户评论"),
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    SourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
