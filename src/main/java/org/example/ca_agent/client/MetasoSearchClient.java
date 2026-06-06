package org.example.ca_agent.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.example.ca_agent.dto.metaso.MetasoSearchResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetasoSearchClient {

    private final MetasoSearchProperties properties;
    private final RestTemplate restTemplate;

    public MetasoSearchResponse search(String query, String scope, boolean includeSummary, int size) {
        String url = properties.getBaseUrl() + "/search";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of(
                "q", query,
                "scope", scope,
                "includeSummary", includeSummary,
                "size", String.valueOf(size)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        log.info("Metaso search: query={}, scope={}, size={}", query, scope, size);

        ResponseEntity<MetasoSearchResponse> response = restTemplate.postForEntity(
                url, request, MetasoSearchResponse.class);

        MetasoSearchResponse result = response.getBody();
        if (result == null) {
            throw new RuntimeException("Metaso search returned empty body");
        }
        return result;
    }

    public String readPage(String url, String format) {
        String apiUrl = properties.getBaseUrl() + "/reader";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean markdown = "markdown".equalsIgnoreCase(format);
        headers.setAccept(java.util.List.of(
                markdown ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of("url", url);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        log.info("Metaso readPage: url={}, format={}", url, format);

        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl, request, String.class);

        String result = response.getBody();
        if (result == null) {
            throw new RuntimeException("Metaso reader returned empty body");
        }
        return result;
    }
}
