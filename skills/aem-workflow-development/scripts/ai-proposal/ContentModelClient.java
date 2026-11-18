package com.example.core.workflows;

import java.io.IOException;

public interface ContentModelClient {

    /** Returns the model's raw text reply. IOException = transient (timeouts, 5xx, rate limits). */
    String complete(String systemPrompt, String userPrompt) throws IOException;

    /** Recorded with each proposal, so reviewers and audits know what produced it. */
    String modelId();
}
