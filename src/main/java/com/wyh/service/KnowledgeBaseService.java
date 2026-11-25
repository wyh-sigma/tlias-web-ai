package com.wyh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 知识库管理：启动加载/重建 + 运行期热更新
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private final Path storeFile;
    private final Path hashFile;
    private final SimpleVectorStore vectorStore;

    public KnowledgeBaseService(SimpleVectorStore vectorStore,
                                @Value("${tlias.rag.store-path:data/vector-store.json}") String storePath) {
        this.vectorStore = vectorStore;
        this.storeFile = Path.of(storePath);
        // 哈希文件与向量库文件同目录同名：vector-store.json -> vector-store.sha256。
        // 这样既与历史文件兼容（升级后不会被误判成需要重建），又能让不同 profile 互不干扰。
        String name = storeFile.getFileName().toString();
        String base = name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
        this.hashFile = storeFile.resolveSibling(base + ".sha256");
    }

    /**
     * 启动时调用：知识内容没变直接加载向量库，变了就重建
     */
    public synchronized void loadOrRebuild() throws IOException {
        Map<String, String> knowledge = readKnowledge();

        if (!needRebuild(knowledge)) {
            try {
                vectorStore.load(storeFile.toFile());
                log.info("已从 {} 加载向量库", storeFile);
                return;
            } catch (Exception e) {
                // 常见原因：换了嵌入模型导致向量维度不一致（例如压测用的假向量），此时直接重建
                log.warn("向量库加载失败，改为重建：{}", e.getMessage());
            }
        }
        doRebuild(knowledge);
    }

    /**
     * 手动热更新：清空旧向量后按当前知识文件重新入库，无需重启
     */
    public synchronized RebuildResult rebuild() throws IOException {
        return doRebuild(readKnowledge());
    }

    private RebuildResult doRebuild(Map<String, String> knowledge) throws IOException {
        // 先清空旧数据，避免热更新后重复文档越积越多
        vectorStore.delete(new FilterExpressionBuilder().isNotNull("source").build());

        List<Document> documents = new ArrayList<>();
        knowledge.forEach((name, text) -> documents.add(new Document(text, Map.of("source", name))));

        List<Document> chunks = new TokenTextSplitter().apply(documents);
        vectorStore.add(chunks);

        Files.createDirectories(storeFile.getParent());
        vectorStore.save(storeFile.toFile());
        Files.writeString(hashFile, sha256(knowledge), StandardCharsets.UTF_8);
        log.info("知识库已重建：{} 个知识文件，{} 个分片", documents.size(), chunks.size());
        return new RebuildResult(documents.size(), chunks.size());
    }

    private Map<String, String> readKnowledge() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge/*.txt");

        Map<String, String> knowledge = new TreeMap<>();
        for (Resource resource : resources) {
            String name = resource.getFilename();
            String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            knowledge.put(name == null ? resource.getDescription() : name, text);
        }
        return knowledge;
    }

    private boolean needRebuild(Map<String, String> knowledge) {
        if (!Files.exists(storeFile) || !Files.exists(hashFile)) {
            return true;
        }
        try {
            String lastHash = Files.readString(hashFile, StandardCharsets.UTF_8).trim();
            boolean changed = !lastHash.equals(sha256(knowledge));
            if (changed) {
                log.info("知识文件内容有更新，准备重建向量库");
            }
            return changed;
        } catch (IOException e) {
            log.warn("读取知识库哈希失败，准备重建向量库", e);
            return true;
        }
    }

    private String sha256(Map<String, String> knowledge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            knowledge.forEach((name, text) -> {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
                digest.update(text.getBytes(StandardCharsets.UTF_8));
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("计算知识库哈希失败", e);
        }
    }

    public record RebuildResult(int files, int chunks) {
    }
}
