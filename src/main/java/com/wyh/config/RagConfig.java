package com.wyh.config;


import com.wyh.monitor.MetricsRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {


    @Bean
    public RetrievalAugmentationAdvisor retrievalAdvisor(SimpleVectorStore vectorStore,
                                                        CompressionQueryTransformer compressionQueryTransformer,
                                                        MetricsRegistry metricsRegistry) {
        DocumentRetriever baseRetriever = VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStore)
                .topK(3)
                // 阈值由 RagEvaluationTest 评测得出：0.5 时 Recall@3=95.83%，0.35 时 100%
                .similarityThreshold(0.35)
                .build();

        // 包装检索器：记录是否命中知识库，供 /monitor/ai 统计命中率与兜底率
        DocumentRetriever meteredRetriever = query -> {
            List<Document> documents = baseRetriever.retrieve(query);
            metricsRegistry.recordKbRetrieval(documents != null && !documents.isEmpty());
            return documents;
        };

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(meteredRetriever)
                // 多轮对话时把“那它呢”这类追问改写成可独立检索的问题
                .queryTransformers(compressionQueryTransformer)
                // 顺序大于对话记忆 advisor，保证先补充历史消息再检索知识库
                .order(100)
                .build();
    }

    /**
     * 压缩式查询改写：结合对话历史，把省略的追问补全成完整问题
     */
    @Bean
    public CompressionQueryTransformer compressionQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
    }
}
