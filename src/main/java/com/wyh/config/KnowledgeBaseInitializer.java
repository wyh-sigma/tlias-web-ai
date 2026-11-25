package com.wyh.config;

import com.wyh.service.KnowledgeBaseService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseInitializer(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        knowledgeBaseService.loadOrRebuild();
    }
}
