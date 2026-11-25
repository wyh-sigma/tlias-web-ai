package com.wyh.config;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class PromptConfig {


    @Value("classpath:prompts/tlias-assistant.txt")
    private Resource tliasAssistantPrompt;

    @Bean
    public PromptTemplate getPromptTemplate() {
        return new PromptTemplate(tliasAssistantPrompt);
    }
}
