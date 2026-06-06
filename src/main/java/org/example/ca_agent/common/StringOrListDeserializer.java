package org.example.ca_agent.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Deserializes a field that may be either a single string or a JSON array of strings.
 * Arrays are joined with "; " into a single string.
 */
public class StringOrListDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode element : node) {
                parts.add(element.asText());
            }
            return String.join("; ", parts);
        }
        return node.asText();
    }
}
