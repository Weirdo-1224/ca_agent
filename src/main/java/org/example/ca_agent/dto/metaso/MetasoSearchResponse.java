package org.example.ca_agent.dto.metaso;

import lombok.Data;

import java.util.List;

@Data
public class MetasoSearchResponse {

    private String summary;
    private List<WebpageResult> webpages;
    private List<WebpageResult> documents;
    private List<WebpageResult> scholars;
    private List<WebpageResult> images;
    private List<WebpageResult> videos;
    private List<WebpageResult> podcasts;

    @Data
    public static class WebpageResult {
        private String title;
        private String link;
        private String url;
        private String snippet;
        private String displayDate;
        private String publishDate;
        private String date;
        private String source;
        private Double score;
        private Integer position;
        private List<String> authors;
        private String author;
    }

    public List<WebpageResult> getResultsForScope(String scope) {
        return switch (scope) {
            case "webpage" -> webpages;
            case "document" -> documents;
            case "scholar" -> scholars;
            case "image" -> images;
            case "video" -> videos;
            case "podcast" -> podcasts;
            default -> webpages;
        };
    }
}
