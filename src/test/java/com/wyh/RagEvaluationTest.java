package com.wyh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索评测：
 * 用一批带标准答案来源的问题，计算 Recall@1、Recall@3 和 MRR，
 * 用于比较切分方式、topK、相似度阈值、查询改写等改动是否真的提升了召回。
 * <p>
 * 运行前需要有 DASHSCOPE_API_KEY 和已构建的 data/vector-store.json：
 * mvn test -Dtest=RagEvaluationTest
 */
@SpringBootTest(properties = {
        // 本测试只用到向量检索，不需要真实对话模型
        "spring.ai.deepseek.api-key=rag-eval-placeholder",
        "tlias.jwt.secret=rag-eval-secret-key-with-32-bytes-min!!"
})
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
class RagEvaluationTest {

    private static final Path STORE_FILE = Path.of("data/vector-store.json");
    private static final Path REPORT_FILE = Path.of("target/rag-eval-report.md");

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${eval.rag.top-k:3}")
    private int topK;

    @Value("${eval.rag.similarity-threshold:0.35}")
    private double similarityThreshold;

    @Test
    void evaluateRetrieval() throws Exception {
        Assumptions.assumeTrue(Files.exists(STORE_FILE),
                "缺少 data/vector-store.json，请先启动一次应用生成向量库");

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.load(STORE_FILE.toFile());

        List<EvalItem> items = new ObjectMapper().readValue(
                new ClassPathResource("eval/rag-questions.json").getInputStream(),
                new TypeReference<>() {
                });

        List<ItemResult> results = new ArrayList<>();
        for (EvalItem item : items) {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(item.question())
                    .topK(topK)
                    // 与线上 RAG advisor 的阈值保持一致，评测结果才能反映真实效果
                    .similarityThreshold(similarityThreshold)
                    .build());

            int rank = -1;
            for (int i = 0; i < documents.size(); i++) {
                Object source = documents.get(i).getMetadata().get("source");
                if (item.expectedSource().equals(source)) {
                    rank = i + 1;
                    break;
                }
            }
            results.add(new ItemResult(item, rank, documents.stream()
                    .map(d -> String.valueOf(d.getMetadata().get("source")))
                    .toList()));
        }

        double recallAt1 = results.stream().filter(r -> r.rank() == 1).count() / (double) results.size();
        double recallAt3 = results.stream().filter(r -> r.rank() > 0).count() / (double) results.size();
        double mrr = results.stream()
                .mapToDouble(r -> r.rank() > 0 ? 1.0 / r.rank() : 0)
                .average().orElse(0);

        writeReport(results, recallAt1, recallAt3, mrr);

        System.out.printf("RAG 检索评测：共 %d 题，topK=%d，阈值=%.2f，Recall@1=%.2f%%，Recall@%d=%.2f%%，MRR=%.3f%n",
                results.size(), topK, similarityThreshold, recallAt1 * 100, topK, recallAt3 * 100, mrr);
        System.out.println("详细报告：target/rag-eval-report.md");
    }

    private void writeReport(List<ItemResult> results, double recallAt1, double recallAt3, double mrr)
            throws Exception {
        StringBuilder md = new StringBuilder();
        md.append("# RAG 检索评测报告\n\n")
                .append("- 题量：").append(results.size()).append('\n')
                .append("- TopK：").append(topK).append('\n')
                .append("- 相似度阈值：").append(String.format("%.2f", similarityThreshold)).append('\n')
                .append("- Recall@1：").append(String.format("%.2f%%", recallAt1 * 100)).append('\n')
                .append("- Recall@").append(topK).append("：").append(String.format("%.2f%%", recallAt3 * 100)).append('\n')
                .append("- MRR：").append(String.format("%.3f", mrr)).append("\n\n")
                .append("| # | 问题 | 期望来源 | 命中排名 | 实际 Top").append(topK).append(" |\n")
                .append("|---|---|---|---|---|\n");

        for (ItemResult r : results) {
            md.append("| ").append(r.item().id())
                    .append(" | ").append(escape(r.item().question()))
                    .append(" | ").append(r.item().expectedSource())
                    .append(" | ").append(r.rank() > 0 ? r.rank() : "未命中")
                    .append(" | ").append(String.join(", ", r.actualSources()))
                    .append(" |\n");
        }

        Files.createDirectories(REPORT_FILE.getParent());
        Files.writeString(REPORT_FILE, md.toString(), StandardCharsets.UTF_8);
    }

    private String escape(String text) {
        return text.replace("|", "\\|");
    }

    private record EvalItem(int id, String question, String expectedSource) {
    }

    private record ItemResult(EvalItem item, int rank, List<String> actualSources) {
    }
}
