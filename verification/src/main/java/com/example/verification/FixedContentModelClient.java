package com.example.verification;

import com.example.core.workflows.ContentModelClient;
import org.osgi.service.component.annotations.Component;

/**
 * Stand-in model provider for on-AEM verification: no network, deterministic reply.
 * Only in the verification bundle; projects provide their own ContentModelClient.
 */
@Component(service = ContentModelClient.class)
public class FixedContentModelClient implements ContentModelClient {

    static final String DESCRIPTION =
        "Plan a weekend of surf lessons, coastal hikes and local food on the Portuguese coast.";

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "{\"description\": \"" + DESCRIPTION + "\"}";
    }

    @Override
    public String modelId() {
        return "fixed-verification-client";
    }
}
