package com.pos.assistant.chat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaudeConfig {

    /**
     * Created only when a non-blank API key is present (environment variables are Spring properties). Without one the
     * assistant still runs in retrieval-only mode, which keeps the rest of the platform demoable offline.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("!'${ANTHROPIC_API_KEY:}'.isBlank()")
    AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
