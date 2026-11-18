package com.example.core.workflows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

final class DescriptionRules {

    static final int MIN_LENGTH = 50;
    static final int MAX_LENGTH = 160;
    private static final ObjectMapper JSON = new ObjectMapper();

    private DescriptionRules() {
    }

    /** Parses the model's JSON reply into the one field we accept. */
    static String fromModelReply(String reply) throws InvalidProposalException {
        JsonNode node;
        try {
            node = JSON.readTree(reply == null ? "" : reply.trim());
        } catch (IOException e) {
            throw new InvalidProposalException("reply was not valid JSON");
        }
        if (node == null || !node.path("description").isTextual()) {
            throw new InvalidProposalException("reply must be {\"description\": \"...\"}");
        }
        return validate(node.get("description").asText());
    }

    static String validate(String text) throws InvalidProposalException {
        String value = text == null ? "" : text.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidProposalException("description must be " + MIN_LENGTH + "-"
                + MAX_LENGTH + " characters, was " + value.length());
        }
        if (value.contains("<") || value.contains(">")) {
            throw new InvalidProposalException("description must be plain text, no HTML");
        }
        return value;
    }
}
