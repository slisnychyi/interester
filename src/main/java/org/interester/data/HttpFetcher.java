package org.interester.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interester.cli.CliException;

import java.net.URI;

public interface HttpFetcher {

    Response get(URI uri);

    record Response(int status, String body) {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        JsonNode json(String apiName) {
            try {
                return MAPPER.readTree(body);
            } catch (Exception e) {
                throw CliException.apiError(apiName + " returned invalid JSON.");
            }
        }
    }
}
